package com.krishilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.dto.AuthDtos.LoginRequest;
import com.krishilink.entity.Lot;
import com.krishilink.repository.LotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TradeSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LotRepository lotRepository;

    private String farmerToken;
    private String buyerToken;
    private String fpoToken;

    @BeforeEach
    void setUp() throws Exception {
        farmerToken = obtainToken("farmer@krishilink.com");
        buyerToken = obtainToken("buyer@krishilink.com");
        fpoToken = obtainToken("fpo@krishilink.com");
    }

    @Test
    @DisplayName("Only transaction participants can view or pay, and paying twice is a no-op")
    void paymentRequiresBuyerAndIsIdempotent() throws Exception {
        long txId = acceptedTransaction(100.0, 40.0);

        mockMvc.perform(authed(get("/api/transactions/" + txId), fpoToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(get("/api/payments/" + txId), fpoToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), fpoToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), farmerToken))
                .andExpect(status().isForbidden());

        String firstRef = data(mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment.status").value("PAID"))
                .andExpect(jsonPath("$.data.transaction.status").value("CONFIRMED"))
                .andReturn()).get("payment") instanceof Map<?, ?> p ? (String) p.get("referenceId") : null;

        mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment already processed"))
                .andExpect(jsonPath("$.data.payment.referenceId").value(firstRef));

        mockMvc.perform(authed(patch("/api/payments/" + txId), buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REFUNDED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Transaction status follows the trade lifecycle and buyer-only steps")
    void transactionStatusTransitionsAreEnforced() throws Exception {
        long txId = acceptedTransaction(100.0, 30.0);

        updateStatus(txId, "COMPLETED", buyerToken).andExpect(status().isBadRequest());

        mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), buyerToken))
                .andExpect(status().isOk());

        updateStatus(txId, "COMPLETED", buyerToken).andExpect(status().isBadRequest());
        updateStatus(txId, "IN_TRANSIT", fpoToken).andExpect(status().isForbidden());
        updateStatus(txId, "IN_TRANSIT", farmerToken).andExpect(status().isOk());
        updateStatus(txId, "DELIVERED", farmerToken).andExpect(status().isForbidden());
        updateStatus(txId, "DELIVERED", buyerToken).andExpect(status().isOk());
        updateStatus(txId, "COMPLETED", farmerToken).andExpect(status().isForbidden());
        updateStatus(txId, "COMPLETED", buyerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Cancelling an unpaid transaction returns stock to the lot")
    void cancellingReleasesStock() throws Exception {
        long lotId = publishedLot(100.0);
        long offerId = createOffer(lotId, 60.0);
        long txId = number(data(mockMvc.perform(authed(patch("/api/offers/" + offerId + "/accept"), farmerToken))
                .andExpect(status().isOk()).andReturn()).get("id"));

        assertThat(lotRepository.findById(lotId).map(Lot::getRemainingQuantity)).contains(40.0);

        updateStatus(txId, "CANCELLED", buyerToken).andExpect(status().isOk());

        Lot lot = lotRepository.findById(lotId).orElseThrow();
        assertThat(lot.getRemainingQuantity()).isEqualTo(100.0);
        assertThat(lot.getStatus().name()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Concurrent acceptance of two offers cannot oversell a lot")
    void concurrentAcceptanceCannotOversell() throws Exception {
        long lotId = publishedLot(100.0);
        long offerA = createOffer(lotId, 80.0);
        long offerB = createOffer(lotId, 80.0);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> results = new ArrayList<>();
        for (long offerId : List.of(offerA, offerB)) {
            Callable<Integer> accept = () -> {
                start.await();
                return mockMvc.perform(authed(patch("/api/offers/" + offerId + "/accept"), farmerToken))
                        .andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(accept));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        pool.shutdown();

        assertThat(statuses).containsOnlyOnce(200);
        assertThat(statuses).anyMatch(s -> s == 400 || s == 409);
        assertThat(lotRepository.findById(lotId).map(Lot::getRemainingQuantity)).contains(20.0);
    }

    @Test
    @DisplayName("Offer totals are exact decimal amounts")
    void offerTotalIsExactDecimal() throws Exception {
        long lotId = publishedLot(50.0);
        mockMvc.perform(authed(post("/api/offers"), buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lotId", lotId, "quantity", 3.0, "pricePerUnit", 0.1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricePerUnit").value(0.1))
                .andExpect(jsonPath("$.data.totalAmount").value(0.3));
    }

    @Test
    @DisplayName("Sellers cannot bid on their own lots")
    void cannotOfferOnOwnLot() throws Exception {
        long lotId = publishedLot(50.0);
        mockMvc.perform(authed(post("/api/offers"), farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lotId", lotId, "quantity", 10.0, "pricePerUnit", 40.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Disputes: only participants can raise, only admins can resolve")
    void disputeAuthorization() throws Exception {
        long txId = acceptedTransaction(100.0, 10.0);
        Map<String, Object> dispute = Map.of("transactionId", txId, "reason", "Quality", "description", "Test");

        mockMvc.perform(authed(post("/api/disputes"), fpoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispute)))
                .andExpect(status().isForbidden());

        long disputeId = number(data(mockMvc.perform(authed(post("/api/disputes"), buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispute)))
                .andExpect(status().isOk()).andReturn()).get("id"));

        mockMvc.perform(authed(patch("/api/disputes/" + disputeId), buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Only Krishi Kendra inspectors or admins can record inspections")
    void inspectionRequiresInspectorRole() throws Exception {
        long lotId = publishedLot(20.0);
        mockMvc.perform(authed(post("/api/inspections"), farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lotId", lotId, "grade", "Grade A"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public registration cannot create admin accounts")
    void cannotSelfRegisterAsAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Mallory", "email", "mallory-" + System.nanoTime() + "@example.com",
                                "password", "Secret@123", "phone", "9999999999",
                                "location", "Indore", "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    private long acceptedTransaction(double lotQuantity, double offerQuantity) throws Exception {
        long offerId = createOffer(publishedLot(lotQuantity), offerQuantity);
        return number(data(mockMvc.perform(authed(patch("/api/offers/" + offerId + "/accept"), farmerToken))
                .andExpect(status().isOk()).andReturn()).get("id"));
    }

    private long publishedLot(double quantity) throws Exception {
        long lotId = number(data(mockMvc.perform(authed(post("/api/lots"), farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "commodity", "Wheat", "quantity", quantity,
                                "expectedPrice", 40.0, "location", "Indore"))))
                .andExpect(status().isOk()).andReturn()).get("id"));
        mockMvc.perform(authed(post("/api/lots/" + lotId + "/publish"), farmerToken))
                .andExpect(status().isOk());
        return lotId;
    }

    private long createOffer(long lotId, double quantity) throws Exception {
        return number(data(mockMvc.perform(authed(post("/api/offers"), buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lotId", lotId, "quantity", quantity, "pricePerUnit", 42.0))))
                .andExpect(status().isOk()).andReturn()).get("id"));
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(long txId, String status, String token)
            throws Exception {
        return mockMvc.perform(authed(patch("/api/transactions/" + txId + "/status"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))));
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private Map<?, ?> data(MvcResult result) throws Exception {
        return (Map<?, ?>) objectMapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }

    private String obtainToken(String email) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("Demo@12345");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return (String) data(result).get("token");
    }
}

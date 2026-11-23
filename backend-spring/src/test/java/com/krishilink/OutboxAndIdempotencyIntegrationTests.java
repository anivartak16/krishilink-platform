package com.krishilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.dto.AuthDtos.LoginRequest;
import com.krishilink.entity.OutboxEvent;
import com.krishilink.outbox.OutboxRelay;
import com.krishilink.outbox.TradeEvents;
import com.krishilink.repository.NotificationRepository;
import com.krishilink.repository.OutboxEventRepository;
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
import java.util.UUID;
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
class OutboxAndIdempotencyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxRelay outboxRelay;

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
    @DisplayName("Accepting an offer stores an event with the trade; the relay turns it into a notification")
    void acceptedOfferIsRelayedAsNotification() throws Exception {
        long txId = acceptedTransaction(100.0, 10.0);

        List<OutboxEvent> events = outboxEventRepository.findByAggregateTypeAndAggregateId("Transaction", txId);
        assertThat(events).singleElement().satisfies(e -> {
            assertThat(e.getEventType()).isEqualTo(TradeEvents.OFFER_ACCEPTED);
            assertThat(e.getPublishedAt()).isNull();
        });
        long notificationsBefore = notificationRepository.count();

        assertThat(outboxRelay.relayPending()).isGreaterThanOrEqualTo(1);

        assertThat(outboxEventRepository.findById(events.get(0).getId()).orElseThrow().getPublishedAt()).isNotNull();
        assertThat(notificationRepository.count()).isGreaterThan(notificationsBefore);
        assertThat(notificationRepository.findAll()).anyMatch(n ->
                n.getType().equals("OFFER_ACCEPTED") && n.getUser().getEmail().equals("buyer@krishilink.com"));
    }

    @Test
    @DisplayName("A rejected request leaves no outbox event behind")
    void rolledBackRequestWritesNoEvent() throws Exception {
        long lotId = publishedLot(50.0);
        long before = outboxEventRepository.count();
        mockMvc.perform(authed(post("/api/offers"), farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lotId", lotId, "quantity", 10.0, "pricePerUnit", 40.0))))
                .andExpect(status().isBadRequest());
        assertThat(outboxEventRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("An event whose handler fails stays pending and records the error")
    void failedEventIsKeptForRetry() {
        OutboxEvent event = outboxEventRepository.save(OutboxEvent.builder()
                .aggregateType("Test").aggregateId(0L).eventType("UNKNOWN_EVENT").payload("{}").build());
        try {
            outboxRelay.relayPending();
            OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(reloaded.getPublishedAt()).isNull();
            assertThat(reloaded.getAttempts()).isEqualTo(1);
            assertThat(reloaded.getLastError()).contains("No handler");
        } finally {
            outboxEventRepository.deleteById(event.getId());
        }
    }

    @Test
    @DisplayName("Idempotency-Key replays the stored response and rejects reuse for another request")
    void paymentWithIdempotencyKey() throws Exception {
        long txId = acceptedTransaction(100.0, 10.0);
        long otherTxId = acceptedTransaction(100.0, 10.0);
        String key = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of("paymentMethod", "UPI");

        String firstRef = (String) ((Map<?, ?>) data(pay(txId, key, body)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.data.payment.status").value("PAID"))
                .andReturn()).get("payment")).get("referenceId");

        pay(txId, key, body)
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"))
                .andExpect(jsonPath("$.data.payment.referenceId").value(firstRef));

        pay(otherTxId, key, body).andExpect(status().isUnprocessableEntity());
        pay(txId, key, Map.of("paymentMethod", "CARD")).andExpect(status().isUnprocessableEntity());

        assertThat(outboxEventRepository.findByAggregateTypeAndAggregateId("Transaction", txId))
                .filteredOn(e -> e.getEventType().equals(TradeEvents.PAYMENT_SECURED))
                .hasSize(1);
    }

    @Test
    @DisplayName("Concurrent retries with the same Idempotency-Key get the same payment")
    void concurrentRetriesShareOneResult() throws Exception {
        long txId = acceptedTransaction(100.0, 10.0);
        String key = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<MvcResult> attempt = () -> {
            start.await();
            return pay(txId, key, Map.of("paymentMethod", "UPI")).andReturn();
        };
        List<Future<MvcResult>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(attempt));
        }
        start.countDown();
        List<String> refs = new ArrayList<>();
        for (Future<MvcResult> f : futures) {
            MvcResult result = f.get();
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            refs.add((String) ((Map<?, ?>) data(result).get("payment")).get("referenceId"));
        }
        pool.shutdown();
        assertThat(refs.get(0)).isNotNull().isEqualTo(refs.get(1));
    }

    private org.springframework.test.web.servlet.ResultActions pay(long txId, String key, Map<String, Object> body)
            throws Exception {
        return mockMvc.perform(authed(post("/api/payments/" + txId + "/process"), buyerToken)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
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

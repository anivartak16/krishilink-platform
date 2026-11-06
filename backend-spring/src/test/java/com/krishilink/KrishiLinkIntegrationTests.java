package com.krishilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.dto.AuthDtos.LoginRequest;
import com.krishilink.entity.User;
import com.krishilink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class KrishiLinkIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String farmerToken;
    private String adminToken;
    private String fpoToken;

    @BeforeEach
    void setUp() throws Exception {
        farmerToken = obtainToken("farmer@krishilink.com", "Demo@12345");
        adminToken = obtainToken("admin@krishilink.com", "Demo@12345");
        fpoToken = obtainToken("fpo@krishilink.com", "Demo@12345");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        Map<?, ?> data = (Map<?, ?>) map.get("data");
        return (String) data.get("token");
    }

    @Test
    @DisplayName("Test Auth: Login and Get Current User (/api/auth/me)")
    void testAuthMe() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("farmer@krishilink.com"));
    }

    @Test
    @DisplayName("Test Auth: e-KYC Verification (/api/auth/ekyc)")
    void testAuthEkyc() throws Exception {
        Map<String, Object> body = Map.of(
                "ekycType", "KISAN_CREDIT_CARD",
                "idNumber", "KCC-987654321",
                "fullName", "Ramesh Patel",
                "village", "Indore Rural"
        );

        mockMvc.perform(post("/api/auth/ekyc")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ekycStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Test Dashboard: (/api/dashboard) and (/api/dashboard/summary)")
    void testDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Test Admin: (/api/admin/summary) and (/api/admin/stats)")
    void testAdminSummary() throws Exception {
        mockMvc.perform(get("/api/admin/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").isNumber());

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Test Markets: (/api/markets) and (/api/markets/nearby)")
    void testMarketsNearby() throws Exception {
        mockMvc.perform(get("/api/markets/nearby?commodity=Wheat&role=FARMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Test Predictions: (/api/predictions), (/api/predictions/overview), (/api/predictions/Wheat)")
    void testPredictions() throws Exception {
        mockMvc.perform(get("/api/predictions/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/predictions/Wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commodity").value("Wheat"))
                .andExpect(jsonPath("$.data.predictedPrice").isNumber());
    }

    @Test
    @DisplayName("Test Lots: get lots, create, publish, and verify-request")
    void testLotsLifecycle() throws Exception {
        // 1. Get lots
        mockMvc.perform(get("/api/lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 2. Create lot
        Map<String, Object> newLot = Map.of(
                "commodity", "Soybean",
                "quantity", 500.0,
                "expectedPrice", 45.0,
                "location", "Indore"
        );

        MvcResult res = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Map<?, ?> json = objectMapper.readValue(res.getResponse().getContentAsString(), Map.class);
        Map<?, ?> lotData = (Map<?, ?>) json.get("data");
        Number lotId = (Number) lotData.get("id");

        // 3. Send verify-request
        mockMvc.perform(post("/api/lots/" + lotId + "/verify-request")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Ready for testing"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Publish lot
        mockMvc.perform(post("/api/lots/" + lotId + "/publish")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Test Storage & Logistics Booking")
    void testStorageAndLogisticsBooking() throws Exception {
        // 1. Get Warehouses
        MvcResult wRes = mockMvc.perform(get("/api/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 2. Book Storage
        Map<String, Object> storageBook = Map.of(
                "warehouseId", 1,
                "quantity", 50,
                "days", 10
        );

        mockMvc.perform(post("/api/storage/book")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storageBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // 3. Get storage bookings
        mockMvc.perform(get("/api/storage/bookings")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 4. Book Logistics
        Map<String, Object> logisticsBook = Map.of(
                "providerId", 1,
                "quantity", 50,
                "distanceKm", 25,
                "pickupLocation", "Indore Farm",
                "deliveryLocation", "Indore Mandi"
        );

        mockMvc.perform(post("/api/logistics/book")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logisticsBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("BOOKED"));

        // 5. Get logistics bookings
        mockMvc.perform(get("/api/logistics/bookings")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Test FPO: members, farmers, lots, and aggregation")
    void testFpoFlow() throws Exception {
        // 1. Get FPO members
        mockMvc.perform(get("/api/fpo/members")
                        .header("Authorization", "Bearer " + fpoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 2. Search farmers
        mockMvc.perform(get("/api/fpo/farmers?search=Ramesh")
                        .header("Authorization", "Bearer " + fpoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 3. Add member
        mockMvc.perform(post("/api/fpo/members")
                        .header("Authorization", "Bearer " + fpoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("farmerId", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Verify member e-KYC
        mockMvc.perform(post("/api/fpo/members/2/verify-ekyc")
                        .header("Authorization", "Bearer " + fpoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Get FPO lots
        mockMvc.perform(get("/api/fpo/lots")
                        .header("Authorization", "Bearer " + fpoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Test Inspection: (/api/inspections/lots) and (/api/inspections)")
    void testInspectionFlow() throws Exception {
        // 1. Get inspection lots
        mockMvc.perform(get("/api/inspections/lots")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 2. Perform inspection
        Map<String, Object> insp = Map.of(
                "lotId", 1,
                "grade", "Grade A",
                "moisture", 11.5,
                "foreignMatter", 0.5,
                "damagedPercentage", 0.2,
                "inspectionStatus", "VERIFIED",
                "inspectionNotes", "Excellent quality grain certified."
        );

        mockMvc.perform(post("/api/inspections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Test
    @DisplayName("Test Payments: (/api/payments/{transactionId}) and (/process)")
    void testPaymentFlow() throws Exception {
        // 1. Get payment for transaction 1
        mockMvc.perform(get("/api/payments/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.transaction").exists());

        // 2. Process payment
        Map<String, Object> payReq = Map.of(
                "paymentMethod", "UPI",
                "remarks", "Automated escrow settlement test"
        );

        mockMvc.perform(post("/api/payments/1/process")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment.status").value("PAID"));
    }

    @Test
    @DisplayName("Test MarketPrice: stats, active-options, sync status")
    void testMarketPriceEndpoints() throws Exception {
        mockMvc.perform(get("/api/marketPrice/prices/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRecords").isNumber());

        mockMvc.perform(get("/api/marketPrice/prices/active-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.states").isArray());

        mockMvc.perform(get("/api/marketPrice/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true));
    }
}

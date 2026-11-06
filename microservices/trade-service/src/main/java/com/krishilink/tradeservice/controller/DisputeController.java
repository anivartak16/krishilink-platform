package com.krishilink.tradeservice.controller;

import com.krishilink.tradeservice.dto.ApiResponse;
import com.krishilink.tradeservice.entity.Dispute;
import com.krishilink.tradeservice.repository.DisputeRepository;
import com.krishilink.tradeservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Dispute>>> getDisputes() {
        return ResponseEntity.ok(ApiResponse.ok(disputeRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Dispute>> createDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long txId = Long.valueOf(body.get("transactionId").toString());
        String reason = body.getOrDefault("reason", "Dispute").toString();
        String desc = body.getOrDefault("description", "").toString();

        Dispute d = Dispute.builder()
                .transactionId(txId)
                .raisedById(principal.getId())
                .raisedByName(principal.getUsername())
                .reason(reason)
                .description(desc)
                .status("OPEN")
                .build();
        d = disputeRepository.save(d);
        return ResponseEntity.ok(ApiResponse.ok(d, "Dispute submitted"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Dispute>> updateDispute(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Dispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        if (body.containsKey("status")) d.setStatus(body.get("status"));
        if (body.containsKey("resolution")) d.setResolution(body.get("resolution"));
        d = disputeRepository.save(d);
        return ResponseEntity.ok(ApiResponse.ok(d, "Dispute updated"));
    }
}

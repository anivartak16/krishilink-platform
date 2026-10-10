package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Lot;
import com.krishilink.enums.LotStatus;
import com.krishilink.repository.LotRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fpo")
@RequiredArgsConstructor
public class FpoController {

    private final LotRepository lotRepository;

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Lot>>> getFpoLots(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Lot> lots = lotRepository.findByOwner(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(lots));
    }
}

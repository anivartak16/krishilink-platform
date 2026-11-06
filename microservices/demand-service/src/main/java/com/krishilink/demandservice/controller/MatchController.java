package com.krishilink.demandservice.controller;

import com.krishilink.demandservice.dto.ApiResponse;
import com.krishilink.demandservice.dto.MatchDto;
import com.krishilink.demandservice.service.DemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final DemandService demandService;

    @GetMapping("/{demandId}")
    public ResponseEntity<ApiResponse<List<MatchDto>>> getMatches(@PathVariable Long demandId) {
        List<MatchDto> matches = demandService.getMatchesFor(demandId);
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }
}

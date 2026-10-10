package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.MatchDto;
import com.krishilink.entity.Demand;
import com.krishilink.entity.User;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.DemandService;
import com.krishilink.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final IntelligenceService intelligenceService;
    private final DemandService demandService;

    @GetMapping("/{demandId}")
    public ResponseEntity<ApiResponse<List<MatchDto>>> getMatches(
            @PathVariable Long demandId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Demand demand = demandService.getDemandById(demandId);
        User currentUser = principal != null ? principal.getUser() : null;
        List<MatchDto> matches = intelligenceService.getMatchesFor(demand, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }
}

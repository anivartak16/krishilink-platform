package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Warehouse;
import com.krishilink.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final WarehouseRepository warehouseRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Warehouse>>> getWarehouses(
            @RequestParam(required = false) String location) {
        List<Warehouse> list = location != null ?
                warehouseRepository.findByLocationContainingIgnoreCase(location) :
                warehouseRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

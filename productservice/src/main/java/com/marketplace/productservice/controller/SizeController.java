package com.marketplace.productservice.controller;

import com.marketplace.productservice.dto.request.SizeRequestDto;
import com.marketplace.productservice.dto.response.SizeResponseDto;
import com.marketplace.productservice.service.SizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sizes")
@RequiredArgsConstructor
public class SizeController {

    private final SizeService sizeService;

    @PostMapping
    public ResponseEntity<SizeResponseDto> createSize(@Valid @RequestBody SizeRequestDto requestDto) {
        SizeResponseDto response = sizeService.createSize(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SizeResponseDto> getSizeById(@PathVariable UUID id) {
        SizeResponseDto response = sizeService.getSizeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SizeResponseDto>> getAllSizes() {
        List<SizeResponseDto> response = sizeService.getAllSizes();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SizeResponseDto> updateSize(
        @PathVariable UUID id,
        @Valid @RequestBody SizeRequestDto requestDto
    ) {
        SizeResponseDto response = sizeService.updateSize(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSize(@PathVariable UUID id) {
        sizeService.deleteSize(id);
        return ResponseEntity.noContent().build();
    }
}

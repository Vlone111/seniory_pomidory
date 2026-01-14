package com.marketplace.productservice.controller;

import com.marketplace.productservice.dto.request.CategoryRequestDto;
import com.marketplace.productservice.dto.response.CategoryResponseDto;
import com.marketplace.productservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto requestDto) {
        CategoryResponseDto response = categoryService.createCategory(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable UUID id) {
        CategoryResponseDto response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<CategoryResponseDto>> getCategoriesByShopId(@PathVariable UUID shopId) {
        List<CategoryResponseDto> response = categoryService.getCategoriesByShopId(shopId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/root")
    public ResponseEntity<List<CategoryResponseDto>> getRootCategories() {
        List<CategoryResponseDto> response = categoryService.getRootCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<CategoryResponseDto>> getSubCategories(@PathVariable UUID parentId) {
        List<CategoryResponseDto> response = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
        @PathVariable UUID id,
        @Valid @RequestBody CategoryRequestDto requestDto
    ) {
        CategoryResponseDto response = categoryService.updateCategory(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

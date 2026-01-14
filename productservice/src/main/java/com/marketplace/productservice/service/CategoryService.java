package com.marketplace.productservice.service;

import com.marketplace.productservice.dto.request.CategoryRequestDto;
import com.marketplace.productservice.dto.response.CategoryResponseDto;
import com.marketplace.productservice.entity.Category;
import com.marketplace.productservice.exception.ResourceNotFoundException;
import com.marketplace.productservice.mapper.CategoryMapper;
import com.marketplace.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        Category category = categoryMapper.toEntity(requestDto);
        
        if (requestDto.getParentId() != null) {
            Category parent = categoryRepository.findById(requestDto.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + requestDto.getParentId()));
            category.setParent(parent);
        }
        
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toDto(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoriesByShopId(UUID shopId) {
        return categoryRepository.findByShopId(shopId).stream()
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteByShopId(UUID shopId) {
        categoryRepository.deleteByShopId(shopId);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getRootCategories() {
        return categoryRepository.findByParentIsNull().stream()
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getSubCategories(UUID parentId) {
        return categoryRepository.findByParentId(parentId).stream()
            .map(categoryMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponseDto updateCategory(UUID id, CategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        categoryMapper.updateEntityFromDto(requestDto, category);
        
        if (requestDto.getParentId() != null) {
            Category parent = categoryRepository.findById(requestDto.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + requestDto.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}

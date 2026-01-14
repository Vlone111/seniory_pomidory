package com.marketplace.productservice.service;

import com.marketplace.productservice.dto.request.ProductRequestDto;
import com.marketplace.productservice.dto.response.ProductResponseDto;
import com.marketplace.productservice.entity.Brand;
import com.marketplace.productservice.entity.Category;
import com.marketplace.productservice.entity.Product;
import com.marketplace.productservice.exception.ResourceNotFoundException;
import com.marketplace.productservice.mapper.ProductMapper;
import com.marketplace.productservice.repository.BrandRepository;
import com.marketplace.productservice.repository.CategoryRepository;
import com.marketplace.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto requestDto, UUID userId) {
        Product product = productMapper.toEntity(requestDto);
        product.setCreatedBy(userId);
        
        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.getCategoryId()));
            product.setCategory(category);
        }
        
        if (requestDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(requestDto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + requestDto.getBrandId()));
            product.setBrand(brand);
        }
        
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByShopId(UUID shopId) {
        return productRepository.findAllByShopId(shopId).stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getActiveProductsByShopId(UUID shopId) {
        return productRepository.findAllByShopIdAndIsActive(shopId, true).stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> searchProducts(
        UUID categoryId,
        UUID brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String searchText
    ) {
        return productRepository.searchProducts(categoryId, brandId, minPrice, maxPrice, searchText).stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        productMapper.updateEntityFromDto(requestDto, product);
        
        if (requestDto.getCategoryId() != null) {
            Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + requestDto.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        
        if (requestDto.getBrandId() != null) {
            Brand brand = brandRepository.findById(requestDto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + requestDto.getBrandId()));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }
        
        Product updatedProduct = productRepository.save(product);
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public void deleteByShopId(UUID shopId) {
        productRepository.deleteByShopId(shopId);
    }
}

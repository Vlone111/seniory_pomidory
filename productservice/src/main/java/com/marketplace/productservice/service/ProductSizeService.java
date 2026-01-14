package com.marketplace.productservice.service;

import com.marketplace.productservice.dto.request.ProductSizeRequestDto;
import com.marketplace.productservice.dto.request.StockDecreaseDto;
import com.marketplace.productservice.dto.response.ProductSizeResponseDto;
import com.marketplace.productservice.entity.Product;
import com.marketplace.productservice.entity.ProductSize;
import com.marketplace.productservice.entity.Size;
import com.marketplace.productservice.exception.InsufficientStockException;
import com.marketplace.productservice.exception.ResourceNotFoundException;
import com.marketplace.productservice.mapper.ProductSizeMapper;
import com.marketplace.productservice.repository.ProductRepository;
import com.marketplace.productservice.repository.ProductSizeRepository;
import com.marketplace.productservice.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSizeService {

    private final ProductSizeRepository productSizeRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final ProductSizeMapper productSizeMapper;

    @Transactional
    public ProductSizeResponseDto createProductSize(ProductSizeRequestDto requestDto) {
        ProductSize productSize = productSizeMapper.toEntity(requestDto);
        
        Product product = productRepository.findById(requestDto.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + requestDto.getProductId()));
        productSize.setProduct(product);
        
        Size size = sizeRepository.findById(requestDto.getSizeId())
            .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + requestDto.getSizeId()));
        productSize.setSize(size);
        
        ProductSize savedProductSize = productSizeRepository.save(productSize);
        return productSizeMapper.toDto(savedProductSize);
    }

    @Transactional(readOnly = true)
    public ProductSizeResponseDto getProductSizeById(UUID id) {
        ProductSize productSize = productSizeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductSize not found with id: " + id));
        return productSizeMapper.toDto(productSize);
    }

    @Transactional(readOnly = true)
    public List<ProductSizeResponseDto> getProductSizesByProductId(UUID productId) {
        return productSizeRepository.findByProductId(productId).stream()
            .map(productSizeMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductSizeResponseDto updateProductSize(UUID id, ProductSizeRequestDto requestDto) {
        ProductSize productSize = productSizeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductSize not found with id: " + id));
        
        productSizeMapper.updateEntityFromDto(requestDto, productSize);
        
        if (requestDto.getProductId() != null) {
            Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + requestDto.getProductId()));
            productSize.setProduct(product);
        }
        
        if (requestDto.getSizeId() != null) {
            Size size = sizeRepository.findById(requestDto.getSizeId())
                .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + requestDto.getSizeId()));
            productSize.setSize(size);
        }
        
        ProductSize updatedProductSize = productSizeRepository.save(productSize);
        return productSizeMapper.toDto(updatedProductSize);
    }

    @Transactional
    public void deleteProductSize(UUID id) {
        if (!productSizeRepository.existsById(id)) {
            throw new ResourceNotFoundException("ProductSize not found with id: " + id);
        }
        productSizeRepository.deleteById(id);
    }

    @Transactional
    public void decreaseStock(StockDecreaseDto stockDecreaseDto) {
        log.info("Decreasing stock for product: {}, size: {}, quantity: {}", 
            stockDecreaseDto.getProductId(), 
            stockDecreaseDto.getSizeId(), 
            stockDecreaseDto.getQuantity());
        
        ProductSize productSize = productSizeRepository
            .findByProductIdAndSizeIdWithLock(stockDecreaseDto.getProductId(), stockDecreaseDto.getSizeId())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("ProductSize not found for product: %s and size: %s", 
                    stockDecreaseDto.getProductId(), 
                    stockDecreaseDto.getSizeId())));
        
        int currentQuantity = productSize.getQuantityAvailable();
        int requestedQuantity = stockDecreaseDto.getQuantity();
        
        if (currentQuantity < requestedQuantity) {
            throw new InsufficientStockException(
                String.format("Insufficient stock. Available: %d, Requested: %d", 
                    currentQuantity, 
                    requestedQuantity));
        }
        
        productSize.setQuantityAvailable(currentQuantity - requestedQuantity);
        productSizeRepository.save(productSize);
        
        log.info("Stock decreased successfully. New quantity: {}", productSize.getQuantityAvailable());
    }
}

package com.marketplace.productservice.service;

import com.marketplace.productservice.dto.request.BrandRequestDto;
import com.marketplace.productservice.dto.response.BrandResponseDto;
import com.marketplace.productservice.entity.Brand;
import com.marketplace.productservice.exception.ResourceNotFoundException;
import com.marketplace.productservice.mapper.BrandMapper;
import com.marketplace.productservice.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional
    public BrandResponseDto createBrand(BrandRequestDto requestDto) {
        Brand brand = brandMapper.toEntity(requestDto);
        Brand savedBrand = brandRepository.save(brand);
        return brandMapper.toDto(savedBrand);
    }

    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return brandMapper.toDto(brand);
    }

    @Transactional(readOnly = true)
    public List<BrandResponseDto> getAllBrands() {
        return brandRepository.findAll().stream()
            .map(brandMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrandResponseDto> getBrandsByShopId(UUID shopId) {
        return brandRepository.findByShopId(shopId).stream()
            .map(brandMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteByShopId(UUID shopId) {
        brandRepository.deleteByShopId(shopId);
    }

    @Transactional
    public BrandResponseDto updateBrand(UUID id, BrandRequestDto requestDto) {
        Brand brand = brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brandMapper.updateEntityFromDto(requestDto, brand);
        Brand updatedBrand = brandRepository.save(brand);
        return brandMapper.toDto(updatedBrand);
    }

    @Transactional
    public void deleteBrand(UUID id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.deleteById(id);
    }
}

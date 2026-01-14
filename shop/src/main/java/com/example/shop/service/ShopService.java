package com.example.shop.service;

import com.example.shop.dto.request.RegistrationRequest;
import com.example.shop.dto.request.UpdateShopRequest;
import com.example.shop.dto.response.ShopResponseDto;
import com.example.shop.entity.Shop;
import com.example.shop.exception.DuplicateResourceException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.mapper.ShopMapper;
import com.example.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopMapper shopMapper;

    @Transactional
    public ShopResponseDto createShop(RegistrationRequest dto, UUID ownerId) {
        log.info("Creating shop for owner: {}", ownerId);

        if (shopRepository.existsByShopUrl(dto.getShopUrl())) {
            throw new DuplicateResourceException("Shop URL already exists: " + dto.getShopUrl());
        }

        if (shopRepository.existsByShopName(dto.getShopName())) {
            throw new DuplicateResourceException("Shop name already exists: " + dto.getShopName());
        }

        Shop shop = shopMapper.toEntity(dto);
        shop.setOwnerId(ownerId);

        Shop savedShop = shopRepository.save(shop);
        log.info("Shop created successfully. shopId={}, ownerId={}", savedShop.getId(), ownerId);

        return shopMapper.toDto(savedShop);
    }

    @Transactional(readOnly = true)
    public ShopResponseDto getShopById(UUID shopId) {
        log.debug("Getting shop by id: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        return shopMapper.toDto(shop);
    }

    @Transactional(readOnly = true)
    public ShopResponseDto getShopByUrl(String shopUrl) {
        log.debug("Getting shop by URL: {}", shopUrl);
        Shop shop = shopRepository.findByShopUrl(shopUrl)
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found with URL: " + shopUrl));
        return shopMapper.toDto(shop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponseDto> getShopsByOwner(UUID ownerId) {
        log.debug("Getting shops for owner: {}", ownerId);
        return shopRepository.findByOwnerId(ownerId).stream()
            .map(shopMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShopResponseDto> getAllShops() {
        log.debug("Getting all shops");
        return shopRepository.findAll().stream()
            .map(shopMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ShopResponseDto updateShop(UUID shopId, UpdateShopRequest dto, UUID ownerId) {
        log.info("Updating shop: {} by owner: {}", shopId, ownerId);

        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        if (!shop.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You are not the owner of this shop");
        }

        if (dto.getShopName() != null && !dto.getShopName().equals(shop.getShopName())) {
            if (shopRepository.existsByShopName(dto.getShopName())) {
                throw new DuplicateResourceException("Shop name already exists: " + dto.getShopName());
            }
        }

        shopMapper.updateEntityFromDto(dto, shop);
        Shop updatedShop = shopRepository.save(shop);
        log.info("Shop updated successfully. shopId={}", shopId);

        return shopMapper.toDto(updatedShop);
    }

    @Transactional
    public void deleteShop(UUID shopId, UUID ownerId) {
        log.info("Deleting shop: {} by owner: {}", shopId, ownerId);

        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        if (!shop.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You are not the owner of this shop");
        }

        shopRepository.delete(shop);
        log.info("Shop deleted successfully. shopId={}", shopId);
    }
}

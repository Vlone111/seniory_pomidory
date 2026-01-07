package com.example.shop.service;

import com.example.shop.dao.Shopdao;
import com.example.shop.dto.request.RegistrationRequest;
import com.example.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final Shopdao shopdao;

    public Long createShop(RegistrationRequest dto, UUID ownerId) {
        log.debug("Start createShop. ownerId={}", ownerId);

        try {
            Shop shop = new Shop();
            shop.setShopName(dto.getShopName());
            shop.setDescription(dto.getDescription());
            shop.setShopUrl(dto.getShopUrl());
            shop.setDesignCode(dto.getDesignCode());
            shop.setOwnerId(ownerId);
            if (dto.getPfpUrl() != null && !dto.getPfpUrl().isBlank()) {
                shop.setPfpUrl(dto.getPfpUrl());
            }

            shop = shopdao.save(shop);

            log.info("Shop saved. shopId={}, ownerId={}", shop.getId(), ownerId);
            return shop.getId();
        } catch (Exception e) {
            log.error("Failed to create shop. ownerId={}, dto={}", ownerId, dto, e);
            throw e;
        }
    }

    public void updateShopAvatar(Long shopId, String pfpUrl, UUID currentUserId) {
        log.debug("Start updateShopAvatar. shopId={}, currentUserId={}", shopId, currentUserId);

        try {
            Shop shop = shopdao.findByIdAndOwnerId(shopId, currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found or access denied"));

            shop.setPfpUrl(pfpUrl);
            shopdao.save(shop);

            log.info("Shop avatar updated. shopId={}, pfpUrl={}", shopId, pfpUrl);
        } catch (Exception e) {
            log.error("Failed to update shop avatar. shopId={}, currentUserId={}, pfpUrl={}", shopId, currentUserId, pfpUrl, e);
            throw e;
        }
    }
}

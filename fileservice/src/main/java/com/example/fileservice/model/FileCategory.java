package com.example.fileservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileCategory {
    SHOP_AVATAR("shops/avatars", "Shop profile pictures"),
    SHOP_BANNER("shops/banners", "Shop banner images"),
    PRODUCT_IMAGE("products/images", "Product images"),
    PRODUCT_THUMBNAIL("products/thumbnails", "Product thumbnails"),
    USER_AVATAR("users/avatars", "User profile pictures"),
    GENERAL("general", "General files");

    private final String prefix;
    private final String description;

    public static FileCategory fromString(String category) {
        if (category == null || category.isBlank()) {
            return GENERAL;
        }
        try {
            return FileCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}

package com.example.shop.mapper;

import com.example.shop.dto.request.RegistrationRequest;
import com.example.shop.dto.request.UpdateShopRequest;
import com.example.shop.dto.response.ShopResponseDto;
import com.example.shop.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ShopMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Shop toEntity(RegistrationRequest dto);

    ShopResponseDto toDto(Shop entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shopUrl", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UpdateShopRequest dto, @MappingTarget Shop entity);
}

package com.marketplace.productservice.mapper;

import com.marketplace.productservice.dto.request.ProductRequestDto;
import com.marketplace.productservice.dto.response.ProductResponseDto;
import com.marketplace.productservice.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {BrandMapper.class, CategoryMapper.class})
public interface ProductMapper {

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Product toEntity(ProductRequestDto dto);

    ProductResponseDto toDto(Product entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(ProductRequestDto dto, @MappingTarget Product entity);
}

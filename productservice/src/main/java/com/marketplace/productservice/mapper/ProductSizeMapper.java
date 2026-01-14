package com.marketplace.productservice.mapper;

import com.marketplace.productservice.dto.request.ProductSizeRequestDto;
import com.marketplace.productservice.dto.response.ProductSizeResponseDto;
import com.marketplace.productservice.entity.ProductSize;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {SizeMapper.class})
public interface ProductSizeMapper {

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "size", ignore = true)
    ProductSize toEntity(ProductSizeRequestDto dto);

    @Mapping(target = "productId", source = "product.id")
    ProductSizeResponseDto toDto(ProductSize entity);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "size", ignore = true)
    void updateEntityFromDto(ProductSizeRequestDto dto, @MappingTarget ProductSize entity);
}

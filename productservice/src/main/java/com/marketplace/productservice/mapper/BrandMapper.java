package com.marketplace.productservice.mapper;

import com.marketplace.productservice.dto.request.BrandRequestDto;
import com.marketplace.productservice.dto.response.BrandResponseDto;
import com.marketplace.productservice.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toEntity(BrandRequestDto dto);

    BrandResponseDto toDto(Brand entity);

    void updateEntityFromDto(BrandRequestDto dto, @MappingTarget Brand entity);
}

package com.marketplace.productservice.mapper;

import com.marketplace.productservice.dto.request.SizeRequestDto;
import com.marketplace.productservice.dto.response.SizeResponseDto;
import com.marketplace.productservice.entity.Size;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SizeMapper {

    Size toEntity(SizeRequestDto dto);

    SizeResponseDto toDto(Size entity);

    void updateEntityFromDto(SizeRequestDto dto, @MappingTarget Size entity);
}

package com.marketplace.productservice.mapper;

import com.marketplace.productservice.dto.request.CategoryRequestDto;
import com.marketplace.productservice.dto.response.CategoryResponseDto;
import com.marketplace.productservice.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parent", ignore = true)
    Category toEntity(CategoryRequestDto dto);

    @Mapping(target = "parentId", source = "parent.id")
    CategoryResponseDto toDto(Category entity);

    @Mapping(target = "parent", ignore = true)
    void updateEntityFromDto(CategoryRequestDto dto, @MappingTarget Category entity);
}

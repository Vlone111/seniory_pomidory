package com.marketplace.newsservice.mapper;

import com.marketplace.newsservice.dto.request.NewsRequestDto;
import com.marketplace.newsservice.dto.response.NewsDetailDto;
import com.marketplace.newsservice.dto.response.NewsResponseDto;
import com.marketplace.newsservice.entity.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NewsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    News toEntity(NewsRequestDto dto);

    NewsResponseDto toResponseDto(News entity);

    NewsDetailDto toDetailDto(News entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDto(NewsRequestDto dto, @MappingTarget News entity);
}

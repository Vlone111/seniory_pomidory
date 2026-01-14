package com.example.userservice.mapper;

import com.example.userservice.dto.CreateRecipientDTO;
import com.example.userservice.dto.RecipientDTO;
import com.example.userservice.entity.RecipientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecipientMapper {

    RecipientDTO toDTO(RecipientEntity entity);

    List<RecipientDTO> toDTOList(List<RecipientEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    RecipientEntity toEntity(CreateRecipientDTO dto);
}

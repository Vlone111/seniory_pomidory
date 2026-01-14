package com.example.userservice.mapper;

import com.example.userservice.dto.UpdateProfileDTO;
import com.example.userservice.dto.UserProfileDTO;
import com.example.userservice.entity.UserEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileDTO toProfileDTO(UserEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "recipients", ignore = true)
    void updateEntityFromDTO(UpdateProfileDTO dto, @MappingTarget UserEntity entity);
}

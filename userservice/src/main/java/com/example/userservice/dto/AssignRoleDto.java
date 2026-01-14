package com.example.userservice.dto;

import lombok.Data;

@Data
public class AssignRoleDto {
    private String targetUserId;
    private String roleName; // admin, owner, user
}






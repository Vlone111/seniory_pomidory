package com.example.userservice.repository;

import com.example.userservice.entity.RecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipientRepository extends JpaRepository<RecipientEntity, UUID> {

    List<RecipientEntity> findAllByUserId(String userId);

    Optional<RecipientEntity> findByIdAndUserId(UUID id, String userId);

    void deleteByIdAndUserId(UUID id, String userId);
}

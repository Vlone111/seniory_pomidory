package com.example.userservice.service;

import com.example.userservice.dto.CreateRecipientDTO;
import com.example.userservice.dto.RecipientDTO;
import com.example.userservice.entity.RecipientEntity;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.exception.RecipientNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.RecipientMapper;
import com.example.userservice.repository.RecipientRepository;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final RecipientMapper recipientMapper;

    @Transactional(readOnly = true)
    public List<RecipientDTO> getRecipientsByUserId(String userId) {
        List<RecipientEntity> recipients = recipientRepository.findAllByUserId(userId);
        return recipientMapper.toDTOList(recipients);
    }

    @Transactional
    public RecipientDTO createRecipient(String userId, CreateRecipientDTO dto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        RecipientEntity recipient = recipientMapper.toEntity(dto);
        recipient.setUser(user);

        RecipientEntity savedRecipient = recipientRepository.save(recipient);
        log.info("Recipient created for user {}: {}", userId, savedRecipient.getId());

        return recipientMapper.toDTO(savedRecipient);
    }

    @Transactional(readOnly = true)
    public RecipientDTO getRecipientById(String userId, UUID recipientId) {
        RecipientEntity recipient = recipientRepository.findByIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new RecipientNotFoundException("Recipient not found: " + recipientId));
        return recipientMapper.toDTO(recipient);
    }

    @Transactional
    public RecipientDTO updateRecipient(String userId, UUID recipientId, CreateRecipientDTO dto) {
        RecipientEntity recipient = recipientRepository.findByIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new RecipientNotFoundException("Recipient not found: " + recipientId));

        recipient.setAddress(dto.getAddress());
        recipient.setZipCode(dto.getZipCode());
        recipient.setCity(dto.getCity());
        recipient.setComment(dto.getComment());

        RecipientEntity updatedRecipient = recipientRepository.save(recipient);
        log.info("Recipient updated: {}", recipientId);

        return recipientMapper.toDTO(updatedRecipient);
    }

    @Transactional
    public void deleteRecipient(String userId, UUID recipientId) {
        RecipientEntity recipient = recipientRepository.findByIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new RecipientNotFoundException("Recipient not found: " + recipientId));

        recipientRepository.delete(recipient);
        log.info("Recipient deleted: {}", recipientId);
    }
}

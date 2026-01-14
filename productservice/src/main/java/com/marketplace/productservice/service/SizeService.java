package com.marketplace.productservice.service;

import com.marketplace.productservice.dto.request.SizeRequestDto;
import com.marketplace.productservice.dto.response.SizeResponseDto;
import com.marketplace.productservice.entity.Size;
import com.marketplace.productservice.exception.ResourceNotFoundException;
import com.marketplace.productservice.mapper.SizeMapper;
import com.marketplace.productservice.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SizeService {

    private final SizeRepository sizeRepository;
    private final SizeMapper sizeMapper;

    @Transactional
    public SizeResponseDto createSize(SizeRequestDto requestDto) {
        Size size = sizeMapper.toEntity(requestDto);
        Size savedSize = sizeRepository.save(size);
        return sizeMapper.toDto(savedSize);
    }

    @Transactional(readOnly = true)
    public SizeResponseDto getSizeById(UUID id) {
        Size size = sizeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + id));
        return sizeMapper.toDto(size);
    }

    @Transactional(readOnly = true)
    public List<SizeResponseDto> getAllSizes() {
        return sizeRepository.findAll().stream()
            .map(sizeMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public SizeResponseDto updateSize(UUID id, SizeRequestDto requestDto) {
        Size size = sizeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Size not found with id: " + id));
        sizeMapper.updateEntityFromDto(requestDto, size);
        Size updatedSize = sizeRepository.save(size);
        return sizeMapper.toDto(updatedSize);
    }

    @Transactional
    public void deleteSize(UUID id) {
        if (!sizeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Size not found with id: " + id);
        }
        sizeRepository.deleteById(id);
    }
}

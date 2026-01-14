package com.marketplace.orderservice.service;

import com.marketplace.orderservice.dto.mapper.OrderMapper;
import com.marketplace.orderservice.dto.response.DeliveryMethodDto;
import com.marketplace.orderservice.repository.DeliveryMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryMethodService {

    private final DeliveryMethodRepository deliveryMethodRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<DeliveryMethodDto> getAllDeliveryMethods() {
        log.info("Fetching all delivery methods");
        return orderMapper.toDeliveryMethodDtoList(deliveryMethodRepository.findAll());
    }
}

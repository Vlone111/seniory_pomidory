package com.marketplace.orderservice.service;

import com.marketplace.orderservice.dto.mapper.OrderMapper;
import com.marketplace.orderservice.dto.response.PaymentMethodDto;
import com.marketplace.orderservice.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> getAllPaymentMethods() {
        log.info("Fetching all payment methods");
        return orderMapper.toPaymentMethodDtoList(paymentMethodRepository.findAll());
    }
}

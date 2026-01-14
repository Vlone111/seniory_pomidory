package com.marketplace.orderservice.dto.mapper;

import com.marketplace.orderservice.dto.response.DeliveryMethodDto;
import com.marketplace.orderservice.dto.response.OrderItemResponseDto;
import com.marketplace.orderservice.dto.response.OrderResponseDto;
import com.marketplace.orderservice.dto.response.PaymentMethodDto;
import com.marketplace.orderservice.entity.DeliveryMethod;
import com.marketplace.orderservice.entity.Order;
import com.marketplace.orderservice.entity.OrderItem;
import com.marketplace.orderservice.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponseDto toOrderResponseDto(Order order);

    List<OrderResponseDto> toOrderResponseDtoList(List<Order> orders);

    @Mapping(target = "subtotal", expression = "java(orderItem.getSubtotal())")
    OrderItemResponseDto toOrderItemResponseDto(OrderItem orderItem);

    List<OrderItemResponseDto> toOrderItemResponseDtoList(List<OrderItem> orderItems);

    DeliveryMethodDto toDeliveryMethodDto(DeliveryMethod deliveryMethod);

    List<DeliveryMethodDto> toDeliveryMethodDtoList(List<DeliveryMethod> deliveryMethods);

    PaymentMethodDto toPaymentMethodDto(PaymentMethod paymentMethod);

    List<PaymentMethodDto> toPaymentMethodDtoList(List<PaymentMethod> paymentMethods);
}

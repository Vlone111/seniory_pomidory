package com.marketplace.cartservice.mapper;

import com.marketplace.cartservice.dto.request.AddItemRequest;
import com.marketplace.cartservice.dto.response.CartItemResponse;
import com.marketplace.cartservice.dto.response.CartResponse;
import com.marketplace.cartservice.model.Cart;
import com.marketplace.cartservice.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "totalPrice", expression = "java(cart.getTotalPrice())")
    @Mapping(target = "totalItems", expression = "java(cart.getTotalItems())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "subtotal", expression = "java(item.getSubtotal())")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponseList(List<CartItem> items);

    CartItem toCartItem(AddItemRequest request);
}

package com.marketplace.cartservice.mapper;

import com.marketplace.cartservice.dto.request.AddItemRequest;
import com.marketplace.cartservice.dto.response.CartItemResponse;
import com.marketplace.cartservice.dto.response.CartResponse;
import com.marketplace.cartservice.model.Cart;
import com.marketplace.cartservice.model.CartItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-11T07:34:55+0500",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260101-2150, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CartMapperImpl implements CartMapper {

    @Override
    public CartResponse toResponse(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        CartResponse.CartResponseBuilder cartResponse = CartResponse.builder();

        cartResponse.items( toItemResponseList( cart.getItems() ) );
        cartResponse.lastUpdated( cart.getLastUpdated() );
        cartResponse.userId( cart.getUserId() );

        cartResponse.totalPrice( cart.getTotalPrice() );
        cartResponse.totalItems( cart.getTotalItems() );

        return cartResponse.build();
    }

    @Override
    public CartItemResponse toItemResponse(CartItem item) {
        if ( item == null ) {
            return null;
        }

        CartItemResponse.CartItemResponseBuilder cartItemResponse = CartItemResponse.builder();

        cartItemResponse.imageUrl( item.getImageUrl() );
        cartItemResponse.name( item.getName() );
        cartItemResponse.price( item.getPrice() );
        cartItemResponse.productId( item.getProductId() );
        cartItemResponse.quantity( item.getQuantity() );
        cartItemResponse.shopId( item.getShopId() );

        cartItemResponse.subtotal( item.getSubtotal() );

        return cartItemResponse.build();
    }

    @Override
    public List<CartItemResponse> toItemResponseList(List<CartItem> items) {
        if ( items == null ) {
            return null;
        }

        List<CartItemResponse> list = new ArrayList<CartItemResponse>( items.size() );
        for ( CartItem cartItem : items ) {
            list.add( toItemResponse( cartItem ) );
        }

        return list;
    }

    @Override
    public CartItem toCartItem(AddItemRequest request) {
        if ( request == null ) {
            return null;
        }

        CartItem.CartItemBuilder cartItem = CartItem.builder();

        cartItem.imageUrl( request.getImageUrl() );
        cartItem.name( request.getName() );
        cartItem.price( request.getPrice() );
        cartItem.productId( request.getProductId() );
        cartItem.quantity( request.getQuantity() );
        cartItem.shopId( request.getShopId() );

        return cartItem.build();
    }
}

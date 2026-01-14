package com.example.shop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShopRequest {

    private String shopName;
    private String description;
    private String pfpUrl;
    private String designCode;
}

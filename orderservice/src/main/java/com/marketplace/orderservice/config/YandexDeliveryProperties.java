package com.marketplace.orderservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yandex.delivery")
@Data
public class YandexDeliveryProperties {

    private String apiUrl = "https://b2b.taxi.yandex.net/b2b/cargo/integration/v2";
    
    private String apiToken;
    
    // Станция отгрузки (warehouse_id)
    private String sourcePlatformStation;
    
    // Вес по умолчанию (в граммах)
    private Integer defaultWeight = 10000;
}

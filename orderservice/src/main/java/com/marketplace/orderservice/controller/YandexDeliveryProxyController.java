package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.config.YandexDeliveryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Прокси-контроллер для Яндекс Доставки виджета
 * Проксирует запросы от фронтенд виджета к API Яндекс Доставки
 */
@RestController
@RequestMapping("/api/yandex-delivery")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class YandexDeliveryProxyController {

    private final YandexDeliveryProperties properties;
    private final WebClient.Builder webClientBuilder;

    /**
     * Получение информации о ПВЗ по городу
     */
    @GetMapping("/pickup-points")
    public Mono<ResponseEntity<Object>> getPickupPoints(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        
        log.info("Getting pickup points: city={}, lat={}, lon={}", city, latitude, longitude);

        // Формируем URL с параметрами
        StringBuilder url = new StringBuilder(properties.getApiUrl() + "/delivery-options?");
        
        if (latitude != null && longitude != null) {
            url.append("latitude=").append(latitude).append("&");
            url.append("longitude=").append(longitude).append("&");
        }

        return webClientBuilder.build()
                .get()
                .uri(url.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken())
                .header("Accept-Language", "ru")
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error getting pickup points", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Расчет стоимости и сроков доставки
     */
    @PostMapping("/calculate")
    public Mono<ResponseEntity<Object>> calculateDelivery(@RequestBody Map<String, Object> request) {
        log.info("Calculating delivery: {}", request);

        return webClientBuilder.build()
                .post()
                .uri(properties.getApiUrl() + "/calculate-delivery")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Accept-Language", "ru")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error calculating delivery", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Универсальный прокси для виджета Яндекс Доставки
     */
    @PostMapping("/widget-proxy")
    public Mono<ResponseEntity<Object>> widgetProxy(@RequestBody Map<String, Object> request) {
        log.info("Yandex Delivery widget proxy request: {}", request);

        String endpoint = (String) request.get("endpoint");
        if (endpoint == null) {
            endpoint = "/delivery-options";
        }

        return webClientBuilder.build()
                .post()
                .uri(properties.getApiUrl() + endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Accept-Language", "ru")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error in widget proxy", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Получение конфигурации для виджета
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getWidgetConfig() {
        return ResponseEntity.ok(Map.of(
                "sourcePlatformStation", properties.getSourcePlatformStation() != null 
                    ? properties.getSourcePlatformStation() 
                    : "05e809bb-4521-42d9-a936-0fb0744c0fb3",
                "defaultWeight", properties.getDefaultWeight(),
                "apiAvailable", properties.getApiToken() != null
        ));
    }
}

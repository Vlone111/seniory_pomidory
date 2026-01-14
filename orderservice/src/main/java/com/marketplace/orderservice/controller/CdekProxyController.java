package com.marketplace.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Прокси-контроллер для СДЭК виджета
 * Проксирует запросы от фронтенд виджета к API СДЭК
 */
@RestController
@RequestMapping("/api/cdek")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CdekProxyController {

    @Value("${cdek.account:EMsc7p9ub9iYAg9p19v76QY97N892U7h}")
    private String cdekAccount;

    @Value("${cdek.secure:7RTe6idH654523G97N892U7h}")
    private String cdekSecure;

    @Value("${cdek.api-url:https://api.edu.cdek.ru/v2}")
    private String cdekApiUrl;

    private final WebClient.Builder webClientBuilder;

    private String accessToken;
    private long tokenExpiresAt = 0;

    /**
     * Получение токена авторизации СДЭК
     */
    private Mono<String> getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return Mono.just(accessToken);
        }

        log.info("Getting new CDEK access token");

        return webClientBuilder.build()
                .post()
                .uri(cdekApiUrl + "/oauth/token?grant_type=client_credentials")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + 
                        Base64.getEncoder().encodeToString(
                                (cdekAccount + ":" + cdekSecure).getBytes(StandardCharsets.UTF_8)))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    accessToken = (String) response.get("access_token");
                    Integer expiresIn = (Integer) response.get("expires_in");
                    tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000;
                    log.info("CDEK token obtained, expires in {} seconds", expiresIn);
                    return accessToken;
                });
    }

    /**
     * Получение списка ПВЗ
     */
    @GetMapping("/deliverypoints")
    public Mono<ResponseEntity<Object>> getDeliveryPoints(
            @RequestParam(required = false) String city_code,
            @RequestParam(required = false) String postal_code,
            @RequestParam(required = false) String country_code) {
        
        log.info("Getting delivery points: city_code={}, postal_code={}", city_code, postal_code);

        return getAccessToken()
                .flatMap(token -> {
                    StringBuilder url = new StringBuilder(cdekApiUrl + "/deliverypoints?");
                    if (city_code != null) url.append("city_code=").append(city_code).append("&");
                    if (postal_code != null) url.append("postal_code=").append(postal_code).append("&");
                    if (country_code != null) url.append("country_code=").append(country_code).append("&");

                    return webClientBuilder.build()
                            .get()
                            .uri(url.toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .retrieve()
                            .bodyToMono(Object.class)
                            .map(ResponseEntity::ok);
                })
                .onErrorResume(e -> {
                    log.error("Error getting delivery points", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Расчет стоимости доставки
     */
    @PostMapping("/calculator/tarifflist")
    public Mono<ResponseEntity<Object>> calculateTariffs(@RequestBody Map<String, Object> request) {
        log.info("Calculating tariffs: {}", request);

        return getAccessToken()
                .flatMap(token -> webClientBuilder.build()
                        .post()
                        .uri(cdekApiUrl + "/calculator/tarifflist")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .map(ResponseEntity::ok))
                .onErrorResume(e -> {
                    log.error("Error calculating tariffs", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Получение списка городов
     */
    @GetMapping("/location/cities")
    public Mono<ResponseEntity<Object>> getCities(
            @RequestParam(required = false) String country_codes,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer size) {
        
        log.info("Getting cities: city={}", city);

        return getAccessToken()
                .flatMap(token -> {
                    StringBuilder url = new StringBuilder(cdekApiUrl + "/location/cities?");
                    if (country_codes != null) url.append("country_codes=").append(country_codes).append("&");
                    if (city != null) url.append("city=").append(city).append("&");
                    if (size != null) url.append("size=").append(size).append("&");

                    return webClientBuilder.build()
                            .get()
                            .uri(url.toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .retrieve()
                            .bodyToMono(Object.class)
                            .map(ResponseEntity::ok);
                })
                .onErrorResume(e -> {
                    log.error("Error getting cities", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Универсальный прокси для виджета СДЭК (GET запросы)
     * Виджет отправляет GET запросы с параметрами
     */
    @GetMapping("/widget")
    public Mono<ResponseEntity<Object>> widgetProxyGet(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Boolean is_handout,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String city_code,
            @RequestParam(required = false) String type) {
        
        log.info("CDEK widget GET request: action={}, is_handout={}, city_code={}", action, is_handout, city_code);

        return getAccessToken()
                .flatMap(token -> {
                    StringBuilder url = new StringBuilder(cdekApiUrl);
                    
                    if ("offices".equals(action)) {
                        url.append("/deliverypoints?");
                        if (is_handout != null) url.append("is_handout=").append(is_handout).append("&");
                        if (city_code != null) url.append("city_code=").append(city_code).append("&");
                        if (type != null) url.append("type=").append(type).append("&");
                        if (size != null) url.append("size=").append(size).append("&");
                        if (page != null) url.append("page=").append(page).append("&");
                    } else if ("cities".equals(action)) {
                        url.append("/location/cities?country_codes=RU&");
                        if (size != null) url.append("size=").append(size).append("&");
                    } else {
                        url.append("/deliverypoints?is_handout=true&size=100");
                    }

                    log.info("Calling CDEK API: {}", url);

                    return webClientBuilder.build()
                            .get()
                            .uri(url.toString())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .retrieve()
                            .bodyToMono(Object.class)
                            .map(ResponseEntity::ok);
                })
                .onErrorResume(e -> {
                    log.error("Error in widget GET proxy", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Универсальный прокси для виджета СДЭК (POST запросы)
     */
    @PostMapping("/widget")
    public Mono<ResponseEntity<Object>> widgetProxyPost(@RequestBody Map<String, Object> request) {
        log.info("CDEK widget POST request: {}", request);

        String action = (String) request.get("action");
        
        return getAccessToken()
                .flatMap(token -> {
                    if ("calculate".equals(action)) {
                        return webClientBuilder.build()
                                .post()
                                .uri(cdekApiUrl + "/calculator/tarifflist")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(Object.class)
                                .map(ResponseEntity::ok);
                    } else {
                        return webClientBuilder.build()
                                .get()
                                .uri(cdekApiUrl + "/deliverypoints?is_handout=true&size=100")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(Object.class)
                                .map(ResponseEntity::ok);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error in widget POST proxy", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", e.getMessage())));
                });
    }
}

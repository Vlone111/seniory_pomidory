package com.marketplace.productservice.messaging;

import com.marketplace.productservice.dto.request.StockDecreaseDto;
import com.marketplace.productservice.service.ProductSizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDecreaseListener {

    private final ProductSizeService productSizeService;

    @RabbitListener(queues = "${rabbitmq.queue.stock-decrease}")
    public void handleStockDecrease(StockDecreaseDto stockDecreaseDto) {
        log.info("Received stock decrease message: {}", stockDecreaseDto);
        
        try {
            productSizeService.decreaseStock(stockDecreaseDto);
            log.info("Stock decrease processed successfully");
        } catch (Exception e) {
            log.error("Error processing stock decrease: {}", e.getMessage(), e);
            throw e;
        }
    }
}

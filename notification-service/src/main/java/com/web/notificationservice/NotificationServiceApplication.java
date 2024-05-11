package com.web.notificationservice;

import com.web.notificationservice.event.OrderPlaceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@Slf4j
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class);
    }

    @KafkaListener(topics = "notificationTopic")
    public void orderNotification(OrderPlaceEvent orderPlaceEvent) {
        log.info("order place with id -> " + orderPlaceEvent.getOrderNumber());
    }

}
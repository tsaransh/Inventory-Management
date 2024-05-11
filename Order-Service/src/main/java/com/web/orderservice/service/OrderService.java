package com.web.orderservice.service;

import com.web.orderservice.dto.InventoryResponse;
import com.web.orderservice.dto.OrderLineItemsDto;
import com.web.orderservice.dto.OrderRequest;
import com.web.orderservice.event.OrderPlaceEvent;
import com.web.orderservice.model.Order;
import com.web.orderservice.model.OrderLineItems;
import com.web.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClient;
    private final KafkaTemplate kafkaTemplate;
//    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = new ArrayList<>();

        for (OrderLineItemsDto orderLineItemsDto : orderRequest.getOrderLineItemsDtoList()) {
            orderLineItems.add(mapToDto(orderLineItemsDto));
        }

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = new ArrayList<>();
        for (OrderLineItems orderLineItem : order.getOrderLineItemsList()) {
            skuCodes.add(orderLineItem.getSkuCode());
        }
//
//        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
//        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookup.start())) {
//            // Call the inventory service and place the order if products are in stock
//            InventoryResponse[] inventoryResponses = webClient.build().get()
//                    .uri("http://inventory-service/api/inventory",uriBuilder ->
//                            uriBuilder.queryParam("skuCode", skuCodes).build())
//                    .retrieve()
//                    .bodyToMono(InventoryResponse[].class)
//                    .block();
//
//            if (inventoryResponses != null) {
//                boolean allProductsInStock = true;
//                for (InventoryResponse inventoryResponse : inventoryResponses) {
//                    if (!inventoryResponse.isInStock()) {
//                        allProductsInStock = false;
//                        break;
//                    }
//                }
//                if (allProductsInStock) {
//                    orderRepository.save(order);
//                    return "order place successfully";
//                } else {
//                    throw new IllegalArgumentException("Product is not in stock, please try again later!");
//                }
//            }
//        } finally {
//            inventoryServiceLookup.end();
//        }

        // Call the inventory service and place the order if products are in stock
        InventoryResponse[] inventoryResponses = webClient.build().get()
                .uri("http://inventory-service/api/inventory",uriBuilder ->
                        uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        if (inventoryResponses != null) {
            boolean allProductsInStock = true;
            for (InventoryResponse inventoryResponse : inventoryResponses) {
                if (!inventoryResponse.isInStock()) {
                    allProductsInStock = false;
                    break;
                }
            }
            if (allProductsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlaceEvent(order.getOrderNumber()));
                return "order place successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later!");
            }
        }


        return null;
    }


    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}

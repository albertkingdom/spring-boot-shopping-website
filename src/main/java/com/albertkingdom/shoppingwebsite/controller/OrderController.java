package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;

@RestController
@RequestMapping(path = "/api/order")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /*
     * POST /api/order — request body:
     * { "items": [ { "productId": 1, "quantity": 2 }, ... ] }
     *
     * userId is resolved from the authenticated principal and priceSum is
     * always computed on the server from current product prices; neither can
     * be set from the request body.
     */
    @PostMapping
    public ResponseEntity<?> saveOrder(@Valid @RequestBody CreateOrderRequest orderRequest, Principal principal) {
        Long id = orderService.createOrder(orderRequest, principal.getName());
        return ResponseEntity
                .created(URI.create("/api/order/" + id))
                .body(Collections.singletonMap("id", id));
    }

    @GetMapping("{id}")
    public OrderDetailResponse getOrderDetailById(@PathVariable("id") Long id) {
        return orderService.getOrderDetailById(id);
    }

    @GetMapping()
    public PageResponse<OrderSummaryResponse> getOrdersByPage(
            @RequestParam(name = "page", defaultValue = "0") @Min(value = 0, message = "page must be zero or greater.") int page) {
        return orderService.getOrdersByPage(page);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok("Order deleted successfully");
    }
}

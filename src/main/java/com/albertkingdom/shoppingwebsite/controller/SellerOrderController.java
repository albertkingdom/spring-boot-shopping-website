package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.SellerOrderResponse;
import com.albertkingdom.shoppingwebsite.service.OrderService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/seller/orders")
@Validated
public class SellerOrderController {
    private final OrderService orderService;

    public SellerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<SellerOrderResponse> getMyOrders(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page, Principal principal) {
        return orderService.getOrdersForSeller(principal.getName(), page);
    }

    @GetMapping("/{id}")
    public SellerOrderResponse getMyOrder(@PathVariable Long id, Principal principal) {
        return orderService.getOrderForSeller(principal.getName(), id);
    }
}

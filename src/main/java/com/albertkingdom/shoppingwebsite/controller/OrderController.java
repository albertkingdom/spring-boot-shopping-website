package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping(path = "/api/order")
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
    public HttpStatus saveOrder(@Valid @RequestBody CreateOrderRequest orderRequest, Principal principal) {
        orderService.createOrder(orderRequest, principal.getName());
        return HttpStatus.OK;
    }

    @GetMapping("{id}")
    public OrderDetailResponse getOrderDetailById(@PathVariable("id") Long id) {
        return orderService.getOrderDetailById(id);
    }

    @GetMapping()
    public PageResponse<OrderSummaryResponse> getOrdersByPage(@RequestParam(name = "page") int page) {
        return orderService.getOrdersByPage(page);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return new ResponseEntity<>("Order deleted successfully", HttpStatus.OK);
    }
}

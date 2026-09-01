package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.model.Order;

import java.util.List;

public interface OrderService {

    /**
     * Create an order for the authenticated user. Runs in a single
     * transaction: product lookups, snapshot construction, total
     * computation, and persistence are atomic.
     *
     * @return the id of the created order
     */
    Long createOrder(CreateOrderRequest request, String userEmail);

    Order saveOrder(Order order);

    List<Order> getAllOrders();

    PageResponse<OrderSummaryResponse> getOrdersByPage(int page);

    Order getOrderById(Long id);

    OrderDetailResponse getOrderDetailById(Long id);

    void deleteOrder(Long id);
}

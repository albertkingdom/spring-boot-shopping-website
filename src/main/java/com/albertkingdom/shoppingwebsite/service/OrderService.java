package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.model.Order;

import java.util.List;

public interface OrderService {

    Order saveOrder(Order order);

    List<Order> getAllOrders();

    PageResponse<OrderSummaryResponse> getOrdersByPage(int page);

    Order getOrderById(Long id);

    OrderDetailResponse getOrderDetailById(Long id);

    void deleteOrder(Long id);
}

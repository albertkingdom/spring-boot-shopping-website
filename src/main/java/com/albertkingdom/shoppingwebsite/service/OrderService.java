package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.CustomOrderResponse;
import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrdersPagination;

import java.util.List;

public interface OrderService {

    Order saveOrder(Order order);

    List<Order> getAllOrders();

    OrdersPagination getOrdersByPage(int page);

    Order getOrderById(Long id);

    CustomOrderResponse getOrderDetailById(Long id);

    void deleteOrder(Long id);
}

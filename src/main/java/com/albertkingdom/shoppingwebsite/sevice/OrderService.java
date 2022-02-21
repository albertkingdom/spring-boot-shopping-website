package com.albertkingdom.shoppingwebsite.sevice;

import com.albertkingdom.shoppingwebsite.model.CustomOrderResponse;
import com.albertkingdom.shoppingwebsite.model.Order;

import java.util.List;

public interface OrderService {

    Order saveOrder(Order order);

    List<Order> getAllOrders();

    Order getOrderById(Long id);

    CustomOrderResponse getOrderDetailById(Long id);

    void deleteOrder(Long id);
}

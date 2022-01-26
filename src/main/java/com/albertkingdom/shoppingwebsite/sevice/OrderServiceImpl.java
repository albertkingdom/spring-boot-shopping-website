package com.albertkingdom.shoppingwebsite.sevice;

import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderRepository orderRepository;
    @Override
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.findById(id).orElseThrow();
        orderRepository.deleteById(id);
    }
}

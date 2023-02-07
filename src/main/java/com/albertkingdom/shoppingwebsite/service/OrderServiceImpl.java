package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.model.*;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import com.albertkingdom.shoppingwebsite.repository.ProductRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public OrdersPagination getOrdersByPage(int page) {
        Pageable pageWithTenElementsDesc = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Order> result = orderRepository.findAll(pageWithTenElementsDesc);
        return new OrdersPagination(result.getContent(), result.getTotalPages(), result.getTotalElements());
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Override
    public CustomOrderResponse getOrderDetailById(Long id) {
        Order result = orderRepository.findById(id).orElseThrow();

        List<OrderItemDetail> orderItemDetailList = result.getOrderItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            return new OrderItemDetail(product.getName(), product.getPrice(), item.getQuantity());

        }).collect(Collectors.toList());

        CustomOrderResponse orderResponse = new CustomOrderResponse(
                result.getId(),
                result.getPriceSum(),
                result.getUserId(),
                userRepository.findById(result.getUserId()).orElseThrow().getEmail(),
                orderItemDetailList
        );

        return orderResponse;
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.findById(id).orElseThrow();
        orderRepository.deleteById(id);
    }
}

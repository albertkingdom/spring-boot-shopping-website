package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderItemResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
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
    public PageResponse<OrderSummaryResponse> getOrdersByPage(int page) {
        Pageable pageWithTenElementsDesc = PageRequest.of(page, 10, Sort.by("id").descending());
        Page<Order> result = orderRepository.findAll(pageWithTenElementsDesc);
        return PageResponse.of(result, OrderSummaryResponse::from);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public OrderDetailResponse getOrderDetailById(Long id) {
        Order result = orderRepository.findById(id).orElseThrow(RuntimeException::new);

        List<OrderItemResponse> items = result.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        String userEmail = userRepository.findById(result.getUserId())
                .orElseThrow(RuntimeException::new)
                .getEmail();

        return new OrderDetailResponse(
                result.getId(),
                result.getPriceSum(),
                result.getUserId(),
                userEmail,
                items);
    }

    @Override
    public void deleteOrder(Long id) {
        orderRepository.findById(id).orElseThrow(RuntimeException::new);
        orderRepository.deleteById(id);
    }
}

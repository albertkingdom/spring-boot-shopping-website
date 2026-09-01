package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderItemRequest;
import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.dto.response.OrderDetailResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderItemResponse;
import com.albertkingdom.shoppingwebsite.dto.response.OrderSummaryResponse;
import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrderItem;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductService productService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }

    @Override
    @Transactional
    public Long createOrder(CreateOrderRequest request, String userEmail) {
        Order order = new Order();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest item : request.getItems()) {
            Product product = productService.getProductById(item.getProductId());
            order.addOrderItem(OrderItem.snapshotOf(product, item.getQuantity()));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setPriceSum(total);
        order.setUserId(userRepository.findByEmail(userEmail).getId());

        return orderRepository.save(order).getId();
    }

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
    @Transactional(readOnly = true)
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
    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.findById(id).orElseThrow(RuntimeException::new);
        orderRepository.deleteById(id);
    }
}

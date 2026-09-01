package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderItemRequest;
import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void getOrderById() {
        Long id = 1L;

        Order order = new Order();
        order.setId(id);
        order.setPriceSum(new BigDecimal("999.00"));
        order.setUserId(1L);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        assertEquals(order, service.getOrderById(id));
    }

    @Test
    void createOrder_snapshotsProductsAndSumsTotalExactly() {
        Product a = new Product(10L, "A", new BigDecimal("199.99"));
        Product b = new Product(11L, "B", new BigDecimal("0.10"));
        when(productService.getProductById(10L)).thenReturn(a);
        when(productService.getProductById(11L)).thenReturn(b);

        User caller = new User(7L, "alice@example.com", "hash", "Alice", null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(caller);

        Order persisted = new Order();
        persisted.setId(42L);
        when(orderRepository.save(any(Order.class))).thenReturn(persisted);

        CreateOrderItemRequest itemA = new CreateOrderItemRequest();
        itemA.setProductId(10L);
        itemA.setQuantity(2);
        CreateOrderItemRequest itemB = new CreateOrderItemRequest();
        itemB.setProductId(11L);
        itemB.setQuantity(3);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Arrays.asList(itemA, itemB));

        Long returnedId = service.createOrder(request, "alice@example.com");

        assertEquals(42L, returnedId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        org.mockito.Mockito.verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        // Two line items, both snapshotted with product name and unit price.
        assertEquals(2, saved.getOrderItems().size());
        assertEquals("A", saved.getOrderItems().get(0).getProductName());
        assertEquals(new BigDecimal("199.99"), saved.getOrderItems().get(0).getUnitPrice());
        assertEquals(2, saved.getOrderItems().get(0).getQuantity());
        assertEquals("B", saved.getOrderItems().get(1).getProductName());
        assertEquals(new BigDecimal("0.10"), saved.getOrderItems().get(1).getUnitPrice());
        assertEquals(3, saved.getOrderItems().get(1).getQuantity());

        // 199.99 * 2 + 0.10 * 3 = 399.98 + 0.30 = 400.28, exact.
        assertEquals(new BigDecimal("400.28"), saved.getPriceSum());
        assertEquals(7L, saved.getUserId());
    }
}

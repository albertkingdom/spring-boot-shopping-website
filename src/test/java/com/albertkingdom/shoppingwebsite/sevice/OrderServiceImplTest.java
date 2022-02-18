package com.albertkingdom.shoppingwebsite.sevice;

import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
class OrderServiceImplTest {
    @MockBean
    private OrderRepository repo;
    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void getOrderById() {
        Long id = 1L;
        Float priceSum = 999F;
        Long userId = 1L;

        Order order = new Order();
        order.setId(id);
        order.setPriceSum(priceSum);
        order.setUserId(userId);

        Mockito.when(repo.findById(id)).thenReturn(Optional.of(order));

        assertEquals(service.getOrderById(id), order);

    }
}
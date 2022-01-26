package com.albertkingdom.shoppingwebsite;

import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrderItem;
import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.sevice.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class OrderTests {
    @Autowired
    private OrderService orderService;

    @Test
    public void saveOrder() {
        Order order = new Order();
        order.setPriceSum(1000F);

        OrderItem orderItem = new OrderItem(1L, 2);


        order.addOrderItem(orderItem);

        orderService.saveOrder(order);
    }
}

package com.albertkingdom.shoppingwebsite.dto.response;

import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellerOrderResponseTest {

    @Test
    void serialization_excludesWholeOrderTotalAndOtherSellerItems() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setPriceSum(new BigDecimal("999.00"));
        order.setOrderItems(Arrays.asList(
                item("Mine", "10.00", 7L),
                item("Other seller item", "989.00", 8L)));

        String json = new ObjectMapper().writeValueAsString(
                SellerOrderResponse.from(order, "buyer@example.com", 7L));

        assertTrue(json.contains("buyer@example.com"));
        assertTrue(json.contains("Mine"));
        assertFalse(json.contains("Other seller item"));
        assertFalse(json.contains("priceSum"));
        assertFalse(json.contains("999.00"));
    }

    private OrderItem item(String name, String unitPrice, Long sellerId) {
        OrderItem item = new OrderItem(1L, 1);
        item.setProductName(name);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSellerId(sellerId);
        return item;
    }
}

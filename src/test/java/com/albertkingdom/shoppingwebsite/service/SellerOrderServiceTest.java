package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.PageResponse;
import com.albertkingdom.shoppingwebsite.dto.response.SellerOrderResponse;
import com.albertkingdom.shoppingwebsite.model.Order;
import com.albertkingdom.shoppingwebsite.model.OrderItem;
import com.albertkingdom.shoppingwebsite.model.Role;
import com.albertkingdom.shoppingwebsite.model.User;
import com.albertkingdom.shoppingwebsite.repository.OrderRepository;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.repository.BuyerEmailProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyLong;

@ExtendWith(MockitoExtension.class)
class SellerOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductService productService;
    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void getOrdersForSeller_returnsOnlyThatSellersItemsAndSubtotal() {
        User seller = seller(7L, "seller@example.com");
        User buyer = new User(9L, "buyer@example.com", "hash", "Buyer", Collections.emptyList());
        Order order = new Order();
        order.setId(21L);
        order.setUserId(9L);
        order.setPriceSum(new BigDecimal("999.00"));
        order.setOrderItems(Arrays.asList(
                item(1L, "Mine", 2, "10.00", 7L),
                item(2L, "Another seller", 1, "979.00", 8L),
                item(3L, "Platform legacy item", 1, "20.00", null)));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(orderRepository.findDistinctByOrderItemsSellerId(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(order)));
        when(userRepository.findBuyerEmailsByIdIn(Collections.singleton(9L)))
                .thenReturn(Collections.singletonList(buyerEmail(9L, "buyer@example.com")));

        PageResponse<SellerOrderResponse> response = service.getOrdersForSeller("seller@example.com", 0);

        SellerOrderResponse result = response.getContent().get(0);
        assertEquals("buyer@example.com", result.getBuyerEmail());
        assertEquals(1, result.getItems().size());
        assertEquals("Mine", result.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("20.00"), result.getSellerSubtotal());
        assertFalse(result.getItems().stream().anyMatch(item -> "Another seller".equals(item.getProductName())));
        assertFalse(result.getItems().stream().anyMatch(item -> "Platform legacy item".equals(item.getProductName())));
        verify(userRepository).findBuyerEmailsByIdIn(Collections.singleton(9L));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getOrderForSeller_rejectsOrderWithoutSellersItems() {
        User seller = seller(7L, "seller@example.com");
        Order order = new Order();
        order.setId(21L);
        order.setOrderItems(Collections.singletonList(item(2L, "Another seller", 1, "10.00", 8L)));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> service.getOrderForSeller("seller@example.com", 21L));
    }

    @Test
    void getOrderForSeller_hidesPlatformLegacyItemsFromMixedOrder() {
        User seller = seller(7L, "seller@example.com");
        User buyer = new User(9L, "buyer@example.com", "hash", "Buyer", Collections.emptyList());
        Order order = new Order();
        order.setId(21L);
        order.setUserId(9L);
        order.setOrderItems(Arrays.asList(
                item(1L, "Mine", 1, "10.00", 7L),
                item(3L, "Platform legacy item", 1, "20.00", null)));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(orderRepository.findById(21L)).thenReturn(Optional.of(order));
        when(userRepository.findById(9L)).thenReturn(Optional.of(buyer));

        SellerOrderResponse response = service.getOrderForSeller("seller@example.com", 21L);

        assertEquals(1, response.getItems().size());
        assertEquals("Mine", response.getItems().get(0).getProductName());
        assertFalse(response.getItems().stream().anyMatch(item -> "Platform legacy item".equals(item.getProductName())));
    }

    @Test
    void getOrdersForSeller_batchesDistinctBuyerEmailsWithoutPerOrderLookups() {
        User seller = seller(7L, "seller@example.com");
        Order first = order(21L, 9L);
        Order second = order(22L, 10L);
        Order sameBuyer = order(23L, 9L);
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(orderRepository.findDistinctByOrderItemsSellerId(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(first, second, sameBuyer)));
        when(userRepository.findBuyerEmailsByIdIn(new HashSet<>(Arrays.asList(9L, 10L))))
                .thenReturn(Arrays.asList(buyerEmail(9L, "buyer-a@example.com"), buyerEmail(10L, "buyer-b@example.com")));

        PageResponse<SellerOrderResponse> response = service.getOrdersForSeller("seller@example.com", 0);

        assertEquals(3, response.getContent().size());
        verify(userRepository).findBuyerEmailsByIdIn(new HashSet<>(Arrays.asList(9L, 10L)));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getOrdersForSeller_rejectsMissingBuyerFromProjection() {
        User seller = seller(7L, "seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(seller);
        when(orderRepository.findDistinctByOrderItemsSellerId(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(order(21L, 9L))));
        when(userRepository.findBuyerEmailsByIdIn(Collections.singleton(9L))).thenReturn(Collections.emptyList());

        assertThrows(com.albertkingdom.shoppingwebsite.exception.ResourceNotFoundException.class,
                () -> service.getOrdersForSeller("seller@example.com", 0));
    }

    private Order order(Long id, Long buyerId) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(buyerId);
        order.setOrderItems(Collections.singletonList(item(id, "Mine", 1, "10.00", 7L)));
        return order;
    }

    private BuyerEmailProjection buyerEmail(Long id, String email) {
        return new BuyerEmailProjection() {
            @Override public Long getId() { return id; }
            @Override public String getEmail() { return email; }
        };
    }

    private User seller(Long id, String email) {
        return new User(id, email, "hash", "Seller",
                Collections.singletonList(new Role("ROLE_SELLER", Collections.emptyList())));
    }

    private OrderItem item(Long productId, String name, int quantity, String unitPrice, Long sellerId) {
        OrderItem item = new OrderItem(productId, quantity);
        item.setProductName(name);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSellerId(sellerId);
        return item;
    }
}

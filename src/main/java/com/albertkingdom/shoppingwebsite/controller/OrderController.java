package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderItemRequest;
import com.albertkingdom.shoppingwebsite.dto.request.CreateOrderRequest;
import com.albertkingdom.shoppingwebsite.model.*;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.service.OrderServiceImpl;
import com.albertkingdom.shoppingwebsite.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;


@RestController
@RequestMapping(path = "/api/order")
public class OrderController {
    @Autowired
    private OrderServiceImpl orderServiceImpl;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductServiceImpl productServiceImpl;

    /*
     * POST /api/order — request body:
     * { "items": [ { "productId": 1, "quantity": 2 }, ... ] }
     *
     * userId is resolved from the authenticated principal and priceSum is
     * always computed on the server from current product prices; neither can
     * be set from the request body.
     */
    @PostMapping
    public HttpStatus saveOrder(@Valid @RequestBody CreateOrderRequest orderRequest, Principal principal) {
        Order newOrder = new Order();
        List<CreateOrderItemRequest> items = orderRequest.getItems();

        String userEmail = principal.getName();
        BigDecimal orderTotalPrice = BigDecimal.ZERO;
        for (CreateOrderItemRequest i : items) {
            Product product = productServiceImpl.getProductById(i.getProductId());
            OrderItem orderItem = OrderItem.snapshotOf(product, i.getQuantity());
            newOrder.addOrderItem(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            orderTotalPrice = orderTotalPrice.add(lineTotal);
        }

        newOrder.setPriceSum(orderTotalPrice);
        newOrder.setUserId(userRepository.findByEmail(userEmail).getId());

        orderServiceImpl.saveOrder(newOrder);

        return HttpStatus.OK;
    }
    @GetMapping("{id}")
    public CustomOrderResponse getOrderDetailById(@PathVariable("id") Long id) {

        return orderServiceImpl.getOrderDetailById(id);
    }

    @GetMapping()
    public OrdersPagination getOrdersByPage(@RequestParam(name = "page") int page ) {
        return orderServiceImpl.getOrdersByPage(page);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable("id") Long id) {
        orderServiceImpl.deleteOrder(id);
        return new ResponseEntity<String>("Order deleted successfully", HttpStatus.OK);
    }

}

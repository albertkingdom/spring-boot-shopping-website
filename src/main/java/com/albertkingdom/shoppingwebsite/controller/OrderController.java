package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.*;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.service.OrderServiceImpl;
import com.albertkingdom.shoppingwebsite.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    create an order with request like
    { items :
    [
    {productId: xx, quantity: yy},
    {productId: xx, quantity: yy}
    ],
    userId: xxx,
    totalPrice: xxx
    }
     */


    @PostMapping
    public HttpStatus saveOrder(@RequestBody OrderRequest orderRequest, Principal principal) {
        Order newOrder = new Order();
        List<OrderRequestItem> items = orderRequest.getItems();

        String userEmail = principal.getName();
        BigDecimal orderTotalPrice = BigDecimal.ZERO;
        for (OrderRequestItem i : items) {
            Product product = productServiceImpl.getProductById(i.getProductId());
            OrderItem orderItem = OrderItem.snapshotOf(product, i.getProductCount());
            newOrder.addOrderItem(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(i.getProductCount()));
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

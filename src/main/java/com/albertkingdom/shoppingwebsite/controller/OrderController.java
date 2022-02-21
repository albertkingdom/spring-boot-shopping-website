package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.*;
import com.albertkingdom.shoppingwebsite.repository.UserRepository;
import com.albertkingdom.shoppingwebsite.sevice.OrderServiceImpl;
import com.albertkingdom.shoppingwebsite.sevice.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Float orderTotalPrice = 0F;
        for (OrderRequestItem i : items ) {
            System.out.println("productId: " + i.getProductId() + ",productCount: "+ i.getProductCount());
            OrderItem orderItem = new OrderItem(i.getProductId(),i.getProductCount());
            newOrder.addOrderItem(orderItem);

            orderTotalPrice += productServiceImpl.getProductById(i.getProductId()).getPrice();
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
    public List<Order> getAllOrder() {
        List<Order> result = orderServiceImpl.getAllOrders();
        //System.out.println(result);
        return result;
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable("id") Long id) {
        orderServiceImpl.deleteOrder(id);
        return new ResponseEntity<String>("Order deleted successfully", HttpStatus.OK);
    }

}

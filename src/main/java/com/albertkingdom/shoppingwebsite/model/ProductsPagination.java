package com.albertkingdom.shoppingwebsite.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductsPagination {
    public List<Product> content;
    public int totalPages;
    public Long totalElements;
}

package com.albertkingdom.shoppingwebsite.controller;

import com.albertkingdom.shoppingwebsite.model.Product;
import com.albertkingdom.shoppingwebsite.model.ProductsPagination;
import com.albertkingdom.shoppingwebsite.service.CloudinaryService;
import com.albertkingdom.shoppingwebsite.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private CloudinaryService cloudinaryService;



    @PostMapping
    public ResponseEntity<Product> saveProduct(@RequestParam("productName") @NotBlank String productName,
                                               @RequestParam("productPrice") @NotBlank @Pattern(regexp = "^\\d+$", message = "Must be digits.") String productPrice,
                                               @RequestParam(value = "productImage", required = false) MultipartFile file
    ) {

        String imgUrl = null;
        String imgName = null;
        try {
            if (file != null) {
                Path tempFilePath = cloudinaryService.saveUploadedFiles(file);
                Map uploadResult = cloudinaryService.uploadFile(tempFilePath);
                imgUrl = uploadResult.get("url").toString();
                imgName = uploadResult.get("public_id").toString();
            }
            Product newProduct = productService.saveProduct(new Product(productName, Float.parseFloat(productPrice), imgUrl, imgName));

            return ResponseEntity.ok().body(newProduct);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().build();

    }

//    @GetMapping
//    public List<Product> getAllProducts(HttpSession session) {
//        return productService.getAllProducts();
//    }

    @GetMapping
    public ProductsPagination getProductsByPage(@RequestParam(name = "page") int page) {
        return productService.getProductsByPage(page);
    }
    // http://localhost:8080/api/products/1
    @GetMapping("{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) {
        Product product = productService.getProductById(id);
        //System.out.println("get by id" + product);

        return new ResponseEntity<Product>(product, HttpStatus.OK);
    }

    @PutMapping("{id}")
    public ResponseEntity<Product> updateProduct(@RequestParam("productName") @NotBlank String productName,
                                                 @RequestParam("productPrice") @NotBlank @Pattern(regexp = "^\\d+$", message = "Must be digits.") String productPrice,
                                                 @RequestParam(value = "productImage", required = false) MultipartFile file,
                                                 @PathVariable("id") Long id
    ) {

        String imgUrl = null;
        String imgName = null;
        try {
            if (file != null) {
                Path tempFilePath = cloudinaryService.saveUploadedFiles(file);
                Map uploadResult = cloudinaryService.uploadFile(tempFilePath);
                imgUrl = uploadResult.get("url").toString();
                imgName = uploadResult.get("public_id").toString();
            }
            Product updatedProduct = productService.updateProduct(new Product(productName, Float.parseFloat(productPrice), imgUrl, imgName), id);

            return ResponseEntity.ok().body(updatedProduct);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Long id) throws IOException {
        Product existedProduct = productService.getProductById(id);
        String imgName = existedProduct.getImgName();
        productService.deleteProduct(id);
        cloudinaryService.deleteFile(imgName);
        return new ResponseEntity<String>("Product deleted successfully", HttpStatus.OK);
    }
}

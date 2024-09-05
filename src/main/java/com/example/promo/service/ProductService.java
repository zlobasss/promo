package com.example.promo.service;

import com.example.promo.entity.Product;
import com.example.promo.entity.Section;
import com.example.promo.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public Product findByCode(String code) {
        return productRepository.findByCode(code);
    }

    public Product delete(String code) {
        Product product = findByCode(code);
        if (product != null) {
            productRepository.delete(product);
        }
        return product;
    }

    public Page<Product> getProducts(int pageNumber) {
        int pageSize = 5; // Количество товаров на странице
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return productRepository.findAll(pageable);
    }


}

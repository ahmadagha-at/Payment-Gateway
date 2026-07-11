package at.ahmad.paymentgateway.service;

import at.ahmad.paymentgateway.dtos.CreateProductDto;
import at.ahmad.paymentgateway.model.Product;
import at.ahmad.paymentgateway.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepo;

    public ProductService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public Product createProduct(CreateProductDto dto) {
        Product product = new Product();
        product.setTitle(dto.title());
        product.setPrice(dto.price());
        return productRepo.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product with ID " + id + " not found."));
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepo.delete(product);
    }
}

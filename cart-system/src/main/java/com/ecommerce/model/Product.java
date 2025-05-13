package com.ecommerce.model;

import java.util.Objects;

public class Product {
    private final int id;
    private String name;
    private double price;
    private int stockQuantity;
    private String description;
    private String category;

    // Constructor
    public Product(int id, String name, double price, int stockQuantity, 
                  String description, String category) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.price = validatePrice(price);
        this.stockQuantity = validateStock(stockQuantity);
        this.description = description;
        this.category = category;
    }

    // Input validation
    private double validatePrice(double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");
        return price;
    }

    private int validateStock(int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");
        return stock;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    // Setters with validation
    public void setName(String name) { this.name = Objects.requireNonNull(name); }
    public void setPrice(double price) { this.price = validatePrice(price); }
    public void setStockQuantity(int stockQuantity) { 
        this.stockQuantity = validateStock(stockQuantity); 
    }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }

    // Equality based on ID only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "Product[ID: %d, Name: %s, Price: ₹%.2f, Stock: %d, Category: %s]",
            id, name, price, stockQuantity, category
        );
    }
}
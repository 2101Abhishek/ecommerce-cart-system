package com.ecommerce.model;

import java.util.Objects;

public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = Objects.requireNonNull(product);
        this.quantity = validateQuantity(quantity);
    }

    private int validateQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        return quantity;
    }

    // Getters
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    // Setters with validation
    public void setQuantity(int quantity) { 
        this.quantity = validateQuantity(quantity); 
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return product.equals(cartItem.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product);
    }

    @Override
    public String toString() {
        return String.format(
            "CartItem[%s x%d = ₹%.2f]",
            product.getName(), quantity, getTotalPrice()
        );
    }
}
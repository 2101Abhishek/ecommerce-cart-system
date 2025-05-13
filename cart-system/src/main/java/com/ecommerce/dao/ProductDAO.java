package com.ecommerce.dao;

import com.ecommerce.model.Product;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProductDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/EcommerceData";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Mysql@123";

    // Thread-safe product cache
    private final Map<Integer, Product> productCache = new ConcurrentHashMap<>();
    private final Set<String> categories = new HashSet<>();

    public List<Product> getAllProducts() throws SQLException {
        if (!productCache.isEmpty()) {
            return new ArrayList<>(productCache.values());
        }

        String sql = "SELECT id, name, price, stock_quantity, description, category FROM products";
        List<Product> products = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("description"),
                    rs.getString("category")
                );
                products.add(product);
                productCache.put(product.getId(), product);
                categories.add(product.getCategory());
            }
        }
        return Collections.unmodifiableList(products);
    }

    public Product getProductById(int id) throws SQLException {
        if (productCache.containsKey(id)) {
            return productCache.get(id);
        }

        try {
            Product product = fetchProductFromDB(id);
            productCache.put(id, product);
            return product;
        } catch (SQLException e) {
            throw e; // Rethrow original SQLException
        }
    }


    private Product fetchProductFromDB(int id) throws SQLException {
        String sql = "SELECT id, name, price, stock_quantity, description, category FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("description"),
                        rs.getString("category")
                    );
                }
            }
        }
        throw new SQLException("Product not found");
    }

    public Set<String> getUniqueCategories() {
        return Collections.unmodifiableSet(categories);
    }

    // Existing methods...
    public int getStockQuantity(int productId) throws SQLException {
        return getProductById(productId).getStockQuantity();
    }

    public void increaseStock(int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
            // Update cache
            Product p = productCache.get(productId);
            if (p != null) {
                p.setStockQuantity(p.getStockQuantity() + quantity);
            }
        }
    }

    public void decreaseStockQuantity(int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
            // Update cache
            Product p = productCache.get(productId);
            if (p != null) {
                p.setStockQuantity(p.getStockQuantity() - quantity);
            }
        }
    }
}
package com.ecommerce.main;

import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.exception.OutOfStockException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.service.CartService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ECommerceApp {
    private static final CartService cartService = new CartService();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final Scanner scanner = new Scanner(System.in);
    private static final double HIGH_VALUE_THRESHOLD = 1000.00;
    
    public static void main(String[] args) {
        boolean running = true;
        
        while (running) {
            printMenu();
            int choice = getUserChoice();
            
            try {
                running = handleUserChoice(choice);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }
    
    private static void printMenu() {
        System.out.println("\n=== E-Commerce Cart System ===");
        System.out.println("1. View Products");
        System.out.println("2. Add Product to Cart");
        System.out.println("3. View Cart");
        System.out.println("4. Remove Product from Cart");
        System.out.println("5. Update Product Quantity");
        System.out.println("6. View Stock Levels");
        System.out.println("7. Checkout");
        System.out.println("8. Increase Product Stock (Admin)");
        System.out.println("9. View Product Categories");
        System.out.println("10. View High-Value Items (₹" + HIGH_VALUE_THRESHOLD + "+)");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }
    
    private static int getUserChoice() {
        while (!scanner.hasNextInt()) {
            System.out.println("Please enter a valid number (0-10)");
            scanner.next();
        }
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return choice;
    }
    
    private static boolean handleUserChoice(int choice) throws SQLException, ProductNotFoundException, OutOfStockException {
        switch (choice) {
            case 1:
                viewProducts();
                return true;
            case 2:
                addToCart();
                return true;
            case 3:
                viewCart();
                return true;
            case 4:
                removeFromCart();
                return true;
            case 5:
                updateQuantity();
                return true;
            case 6:
                viewStockLevels();
                return true;
            case 7:
                checkout();
                return true;
            case 8:
                increaseStock();
                return true;
            case 9:
                viewCategories();
                return true;
            case 10:
                viewHighValueItems();
                return true;
            case 0:
                System.out.println("Thank you for visiting!");
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }
    
    private static void viewProducts() throws SQLException {
        System.out.println("\n=== Available Products ===");
        List<Product> products = productDAO.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        
        products.forEach(product -> System.out.printf(
            "ID: %d | %-20s | %10s | %3d in stock | %s\n",
            product.getId(),
            product.getName(),
            formatPrice(product.getPrice()),
            product.getStockQuantity(),
            product.getCategory()
        ));
    }
    
    private static void addToCart() throws ProductNotFoundException, OutOfStockException {
        System.out.print("Enter Product ID: ");
        int productId = scanner.nextInt();
        System.out.print("Enter Quantity: ");
        int quantity = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        cartService.addToCart(productId, quantity);
        System.out.println("Product added to cart successfully!");
    }
    
    private static void viewCart() {
        System.out.println("\n=== Your Shopping Cart ===");
        List<CartItem> items = cartService.getCartItems();
        
        if (items.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
        
        items.forEach(item -> System.out.printf(
            "%-20s | %3d x %10s = %12s\n",
            item.getProduct().getName(),
            item.getQuantity(),
            formatPrice(item.getProduct().getPrice()),
            formatPrice(item.getTotalPrice())
        ));
        
        System.out.println("----------------------------------------");
        System.out.printf("%37s\n", "TOTAL: " + formatPrice(cartService.calculateTotalPrice()));
    }
    
    private static void removeFromCart() {
        System.out.print("Enter Product ID to remove: ");
        int productId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        if (cartService.getCartItems().stream().noneMatch(item -> item.getProduct().getId() == productId)) {
            System.out.println("Product not found in cart.");
            return;
        }
        
        cartService.removeFromCart(productId);
        System.out.println("Product removed from cart successfully!");
    }
    
    private static void updateQuantity() throws ProductNotFoundException, OutOfStockException {
        System.out.print("Enter Product ID: ");
        int productId = scanner.nextInt();
        System.out.print("Enter New Quantity: ");
        int quantity = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        cartService.updateQuantity(productId, quantity);
        System.out.println("Quantity updated successfully!");
    }
    
    private static void viewStockLevels() throws SQLException {
        System.out.println("\n=== Current Stock Levels ===");
        productDAO.getAllProducts().forEach(product -> 
            System.out.printf("%-20s (ID: %d): %3d available\n", 
                product.getName(), 
                product.getId(), 
                product.getStockQuantity())
        );
    }
    
    private static void viewCategories() throws SQLException {
        System.out.println("\n=== Product Categories ===");
        productDAO.getUniqueCategories().forEach(System.out::println);
    }
    
    private static void viewHighValueItems() {
        List<CartItem> highValueItems = cartService.getExpensiveItems(HIGH_VALUE_THRESHOLD);
        
        if (highValueItems.isEmpty()) {
            System.out.println("\nNo items over " + formatPrice(HIGH_VALUE_THRESHOLD) + " in your cart.");
            return;
        }
        
        System.out.println("\n=== High-Value Items (> " + formatPrice(HIGH_VALUE_THRESHOLD) + ") ===");
        highValueItems.forEach(item -> 
            System.out.printf("%-20s | %12s\n",
                item.getProduct().getName(),
                formatPrice(item.getTotalPrice()))
        );
    }
    
    private static void checkout() {
        if (cartService.getCartItems().isEmpty()) {
            System.out.println("Your cart is empty. Nothing to checkout.");
            return;
        }

        // 1. Show cart summary
        System.out.println("\n=== Review Your Order ===");
        viewCart();
        
        // 2. Collect customer details
        String[] customerInfo = collectCustomerInfo();
        
        // 3. Confirm details
        if (!verifyCustomerDetails(customerInfo)) {
            System.out.println("Checkout cancelled.");
            return;
        }
        
        // 4. Process payment
        System.out.println("\nProcessing payment...");
        try {
            Thread.sleep(1500); // Simulate payment processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 5. Process order
        processOrder(customerInfo);
    }

    private static boolean verifyCustomerDetails(String[] customerInfo) {
        while (true) {
            System.out.println("\nPlease verify your information:");
            System.out.println("1. Name: " + customerInfo[0]);
            System.out.println("2. Email: " + customerInfo[1]);
            System.out.println("3. Phone: " + customerInfo[2]);
            System.out.println("4. Address: " + customerInfo[3]);
            System.out.print("Is this correct? (Y/N/Edit option number): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y")) {
                return true;
            } else if (input.equals("n")) {
                return false;
            } else if (input.matches("[1-4]")) {
                editSpecificField(customerInfo, Integer.parseInt(input));
            } else {
                System.out.println("Invalid input. Please try again.");
            }
        }
    }

    private static void processOrder(String[] customerInfo) {
        try {
            // Validate stock
            cartService.validateCartStock();
            
            OrderDAO orderDAO = new OrderDAO();
            
            // Create order
            int orderId = orderDAO.createOrder(
                cartService.getCartItems(),
                customerInfo[0],
                customerInfo[1],
                customerInfo[2],
                customerInfo[3]
            );

            // Update inventory
            for (CartItem item : cartService.getCartItems()) {
                productDAO.decreaseStockQuantity(item.getProduct().getId(), item.getQuantity());
            }

            // Update order status
            orderDAO.updateOrderStatus(orderId, "processing", "paid");
            orderDAO.markInventoryUpdated(orderId);

            // Show receipt
            printReceipt();
            
            // Thank you message
            System.out.println("\nThank you for your purchase, " + customerInfo[0] + "!");
            System.out.println("Order ID: " + orderId);
            System.out.println("A confirmation has been sent to: " + customerInfo[1]);
            
            cartService.clearCart();

        } catch (OutOfStockException e) {
            System.out.println("\nError during checkout: " + e.getMessage());
            System.out.println("Please adjust your cart and try again.");
        } catch (Exception e) {
            System.out.println("\nError during checkout: " + e.getMessage());
            System.out.println("Your order was not completed. Please try again.");
        }
    }

    private static String[] collectCustomerInfo() {
        String[] info = new String[4];
        System.out.println("\n=== Enter Your Details ===");
        System.out.print("Full Name: ");
        info[0] = scanner.nextLine();
        
        System.out.print("Email Address: ");
        info[1] = scanner.nextLine();
        
        info[2] = getValidPhoneNumber();
        
        System.out.print("Shipping Address: ");
        info[3] = scanner.nextLine();
        
        return info;
    }

    private static void editSpecificField(String[] customerInfo, int fieldNumber) {
        switch (fieldNumber) {
            case 1:
                System.out.print("Enter new name: ");
                customerInfo[0] = scanner.nextLine();
                break;
            case 2:
                System.out.print("Enter new email: ");
                customerInfo[1] = scanner.nextLine();
                break;
            case 3:
                customerInfo[2] = getValidPhoneNumber();
                break;
            case 4:
                System.out.print("Enter new address: ");
                customerInfo[3] = scanner.nextLine();
                break;
        }
        System.out.println("Field updated successfully!");
    }

    private static String getValidPhoneNumber() {
        while (true) {
            System.out.print("Phone Number (10 digits): ");
            String phone = scanner.nextLine();
            if (phone.matches("\\d{10}")) {
                return phone;
            }
            System.out.println("Invalid phone number! Please enter exactly 10 digits.");
        }
    }

    private static void increaseStock() {
        try {
            System.out.println("\n=== Increase Stock ===");
            viewProducts();
            
            System.out.print("Enter Product ID to restock: ");
            int productId = scanner.nextInt();
            System.out.print("Enter Quantity to add: ");
            int quantity = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            if (quantity <= 0) {
                System.out.println("Quantity must be positive.");
                return;
            }
            
            productDAO.increaseStock(productId, quantity);
            System.out.println("Stock increased successfully!");
            
        } catch (SQLException e) {
            System.out.println("Error updating stock: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Invalid input. Please enter numbers only.");
            scanner.nextLine(); // Clear invalid input
        }
    }

    private static void printReceipt() {
        List<CartItem> items = cartService.getCartItems();
        double subtotal = cartService.calculateTotalPrice();
        double tax = subtotal * 0.18;
        double total = subtotal + tax;
        
        System.out.println("\n========= ORDER RECEIPT =========");
        System.out.println("----------------------------------------");
        System.out.printf("%-20s %6s %12s\n", "ITEM", "QTY", "PRICE");
        System.out.println("----------------------------------------");
        
        items.forEach(item -> System.out.printf(
            "%-20s %6d %12s\n", 
            item.getProduct().getName(),
            item.getQuantity(),
            formatPrice(item.getTotalPrice())
        ));
        
        System.out.println("----------------------------------------");
        System.out.printf("%-20s %18s\n", "SUBTOTAL:", formatPrice(subtotal));
        System.out.printf("%-20s %18s\n", "GST (18%):", formatPrice(tax));
        System.out.println("----------------------------------------");
        System.out.printf("%-20s %18s\n", "TOTAL:", formatPrice(total));
        System.out.println("========================================");
        System.out.println("Order Date: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        System.out.println("========================================");
    }
    
    private static String formatPrice(double price) {
        return String.format("₹%,.2f", price);
    }
}
package com.example;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;


public class SecureBankingApp 
{
    private static final String URL = "jdbc:mysql://localhost:3306/bank_system";
    private static final String USER = "root";
    private static final String PASSWORD = "password";
    private static final Scanner scanner = new Scanner(System.in);
    private static String currentAccountNo = null;
    private static String mfaCode = null;

    public static void main(String[] args) {
        System.out.println("Welcome to Secure Bank System");

        while (true) {
            System.out.println("\n1. Create Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> createAccount();
                    case 2 -> login();
                    case 3 -> {
                        System.out.println("Thank you for using Secure Bank System. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
    }

    public static void createAccount() {
        System.out.print("Enter Account Number (format: ABC12345): ");
        String accountNo = scanner.nextLine();

        if (!InputValidator.validateAccountNumber(accountNo)) {
            System.out.println("Invalid account number format");
            return;
        }

        System.out.print("Enter Password (min 8 chars with upper, lower, number, special): ");
        String password = scanner.nextLine();

        if (!InputValidator.validatePassword(password)) {
            System.out.println("Password does not meet requirements");
            return;
        }

        System.out.print("Enter Initial Deposit: ");
        try {
            double balance = Double.parseDouble(scanner.nextLine());
            
            if (!InputValidator.validateAmount(balance)) {
                System.out.println("Invalid amount");
                return;
            }

            try {
                byte[] salt = PasswordEncryptionService.generateSalt();
                byte[] encryptedPassword = PasswordEncryptionService.getEncryptedPassword(password, salt);

                String sql = "INSERT INTO customers (accountNo, password, salt, balance) VALUES (?, ?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, accountNo);
                    stmt.setBytes(2, encryptedPassword);
                    stmt.setBytes(3, salt);
                    stmt.setDouble(4, balance);
                    
                    stmt.executeUpdate();
                    System.out.println("Account created successfully");
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
                SecureExceptionHandler.handle(e, "Error creating account");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format");
        }
    }

    public static void login() {
        System.out.print("Enter Account Number: ");
        String accountNo = scanner.nextLine();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT password, salt, balance FROM customers WHERE accountNo = ?")) {
            
            stmt.setString(1, accountNo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                byte[] storedPassword = rs.getBytes("password");
                byte[] salt = rs.getBytes("salt");
                
                if (PasswordEncryptionService.authenticate(password, storedPassword, salt)) {
                    mfaCode = MFAService.generateVerificationCode();
                    if (MFAService.sendVerificationCode(accountNo, mfaCode)) {
                        System.out.print("Enter Verification Code: ");
                        String code = scanner.nextLine();
                        
                        if (MFAService.verifyCode(mfaCode, code)) {
                            currentAccountNo = accountNo;
                            System.out.println("Login successful!");
                            showCustomerMenu();
                        } else {
                            System.out.println("Invalid verification code");
                        }
                    }
                } else {
                    System.out.println("Invalid credentials");
                }
            } else {
                System.out.println("Account not found");
            }
        } catch (Exception e) {
            SecureExceptionHandler.handle(e, "Login error");
        }
    }

    private static void showCustomerMenu() {
        while (currentAccountNo != null) {
            System.out.println("\n1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Logout");
            System.out.print("Select an option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1 -> checkBalance();
                    case 2 -> deposit();
                    case 3 -> withdraw();
                    case 4 -> {
                        currentAccountNo = null;
                        mfaCode = null;
                        System.out.println("Logged out successfully");
                    }
                    default -> System.out.println("Invalid option");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
    }

    private static void checkBalance() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT balance FROM customers WHERE accountNo = ?")) {
            
            stmt.setString(1, currentAccountNo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.printf("Current balance: $%.2f%n", rs.getDouble("balance"));
            }
        } catch (Exception e) {
            SecureExceptionHandler.handle(e, "Error checking balance");
        }
    }

    private static void deposit() {
        System.out.print("Enter deposit amount: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            
            if (!InputValidator.validateAmount(amount)) {
                System.out.println("Invalid amount");
                return;
            }

            updateBalance(amount, true);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format");
        }
    }

    private static void withdraw() {
        System.out.print("Enter withdrawal amount: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            
            if (!InputValidator.validateAmount(amount)) {
                System.out.println("Invalid amount");
                return;
            }

            updateBalance(amount, false);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format");
        }
    }

    private static void updateBalance(double amount, boolean isDeposit) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false); // Start transaction
            
            try (PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT balance FROM customers WHERE accountNo = ? FOR UPDATE");
                 PreparedStatement updateStmt = conn.prepareStatement(
                     "UPDATE customers SET balance = ? WHERE accountNo = ?")) {
                
                // Check current balance
                checkStmt.setString(1, currentAccountNo);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    double currentBalance = rs.getDouble("balance");
                    double newBalance;
                    
                    if (isDeposit) {
                        newBalance = currentBalance + amount;
                    } else {
                        if (amount > currentBalance) {
                            System.out.println("Insufficient funds");
                            return;
                        }
                        newBalance = currentBalance - amount;
                    }
                    
                    // Update balance
                    updateStmt.setDouble(1, newBalance);
                    updateStmt.setString(2, currentAccountNo);
                    updateStmt.executeUpdate();
                    
                    conn.commit();
                    System.out.printf("Transaction successful. New balance: $%.2f%n", newBalance);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            SecureExceptionHandler.handle(e, "Transaction error");
        }
    }
}

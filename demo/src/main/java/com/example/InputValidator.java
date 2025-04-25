package com.example;

public class InputValidator {
    private static final int MAX_ACCOUNT_LENGTH = 20;
    private static final double MAX_TRANSACTION_AMOUNT = 1000000;

    public static boolean validateAccountNumber(String accountNo) {
        return accountNo != null && 
               accountNo.matches("[A-Z]{2,3}\\d{5,8}") && 
               accountNo.length() <= MAX_ACCOUNT_LENGTH;
    }

    public static boolean validatePassword(String password) {
        return password != null && 
               password.length() >= 8 && 
               password.matches(".*[A-Z].*") && 
               password.matches(".*[a-z].*") && 
               password.matches(".*\\d.*") && 
               password.matches(".*[!@#$%^&*].*");
    }

    public static boolean validateAmount(double amount) {
        return amount > 0 && amount <= MAX_TRANSACTION_AMOUNT;
    }
}

package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Unit test for simple App.
 */
public class SecureBankingAppTest 
{
    // Mock implementation of OTPService.generateOTP for testing purposes
    public static String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000); // Generates a 6-digit OTP
    }
    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/bank_test";

    // Mock implementation of isValidUsername for testing purposes
    public static boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{5,20}$");
    }
    private static Connection testConn;

    // Mock implementation of isValidPassword for testing purposes
    public static boolean isValidPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        // Create a separate test database
        testConn = DriverManager.getConnection(TEST_DB_URL, "root", "yourpassword");
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS bank_test");
            stmt.execute("USE bank_test");
            
            // Create test tables
            stmt.execute("CREATE TABLE users LIKE bank.users");
            stmt.execute("CREATE TABLE transactions LIKE bank.transactions");
        }
    }

    @AfterEach
    public void cleanUp() throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    void testPasswordHashing() {
        String password = "SecurePass123!";
        assertTrue(isValidUsername("validUser123"));
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        assertTrue(BCrypt.checkpw(password, hash));
    }

    @Test
    void testUsernameValidation() {
        assertTrue(isValidUsername("validUser123"));
        assertFalse(isValidUsername("inv@lid"));
        assertFalse(isValidUsername("a".repeat(21)));
        assertTrue(isValidPassword("StrongPass1!"));
        assertFalse(isValidPassword("weak"));
        assertFalse(isValidPassword("noSpecialChar1"));
        assertFalse(isValidPassword("NOLOWERCASE1!"));
    }
    @Test
    void testPasswordValidation() {
        assertTrue(isValidPassword("StrongPass1!"));
        assertFalse(isValidPassword("weak"));
        assertFalse(isValidPassword("noSpecialChar1"));
        assertFalse(isValidPassword("NOLOWERCASE1!"));
        // Removed unused variable otp1
    }

    @Test
    void testOTPGeneration() {
        String otp1 = generateOTP();
        String otp2 = generateOTP();
        
        assertEquals(6, otp1.length());
        assertEquals(6, otp2.length());
        assertNotEquals(otp1, otp2);
    }

    @Test
    void testUserRegistration() throws SQLException {
        String username = "testuser";
        String password = "TestPass123!";
        
        // Test registration
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            assertEquals(1, ps.executeUpdate());
        }
        
        // Verify user exists
        query = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = testConn.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(BCrypt.checkpw(password, rs.getString("password")));
        }
    }

    @Test
    void testBalanceUpdate() throws SQLException {
        // Setup test user
        String setupQuery = "INSERT INTO users (username, password, balance) VALUES (?, ?, 100.00)";
        try (PreparedStatement ps = testConn.prepareStatement(setupQuery)) {
            ps.setString(1, "testuser");
            ps.setString(2, BCrypt.hashpw("password", BCrypt.gensalt()));
            ps.executeUpdate();
        }
        
        // Get user ID
        int userId;
        String getIdQuery = "SELECT id FROM users WHERE username = 'testuser'";
        try (Statement stmt = testConn.createStatement()) {
            ResultSet rs = stmt.executeQuery(getIdQuery);
            rs.next();
            userId = rs.getInt("id");
        }
        
        // Test deposit
        String updateQuery = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(updateQuery)) {
            ps.setDouble(1, 50.00);
            ps.setInt(2, userId);
            assertEquals(1, ps.executeUpdate());
        }
        
        // Verify new balance
        String balanceQuery = "SELECT balance FROM users WHERE id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(balanceQuery)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertEquals(150.00, rs.getDouble("balance"), 0.001);
        }
    }
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }
}

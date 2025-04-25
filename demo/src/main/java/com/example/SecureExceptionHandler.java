package com.example;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SecureExceptionHandler {
    private static final Logger logger = Logger.getLogger(SecureExceptionHandler.class.getName());

    public static void handle(Exception e, String userFriendlyMessage) {
        logger.log(Level.SEVERE, "Error occurred", e);
        System.out.println(userFriendlyMessage);
    }
}

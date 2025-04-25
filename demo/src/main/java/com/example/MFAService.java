package com.example;

import java.util.Random;

public class MFAService {
    private static final int CODE_LENGTH = 6;
    private static final int CODE_VALIDITY_MINUTES = 5;
    

    public static String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    public static boolean sendVerificationCode(String accountNo, String code) {
        // In real implementation, send via SMS/email
        System.out.println("MFA code for " + accountNo + ": " + code);
        System.out.println("This code is valid for " + CODE_VALIDITY_MINUTES + " minutes");
        return true;
    }

    public static boolean verifyCode(String storedCode, String enteredCode) {
        return storedCode != null && storedCode.equals(enteredCode);
    }
}

package io.github.cogitatortech.tamarind.test;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Quick test to verify BCrypt hash for "admin" password. */
public class BCryptHashTest {
  public static void main(String[] args) {
    String password = "admin";

    // Generate a fresh hash
    String newHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
    System.out.println("Newly generated hash for 'admin':");
    System.out.println(newHash);
    System.out.println();

    // Test the hash I provided earlier
    String providedHash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.KePhq6";
    System.out.println("Testing provided hash:");
    System.out.println(providedHash);
    BCrypt.Result result1 = BCrypt.verifyer().verify(password.toCharArray(), providedHash);
    System.out.println("Verifies: " + result1.verified);
    System.out.println();

    // Test with the newly generated hash
    System.out.println("Testing newly generated hash:");
    BCrypt.Result result2 = BCrypt.verifyer().verify(password.toCharArray(), newHash);
    System.out.println("Verifies: " + result2.verified);
    System.out.println();

    // Generate multiple hashes to show they're all different but all valid
    System.out.println("Generating 3 more valid hashes for 'admin':");
    for (int i = 0; i < 3; i++) {
      String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
      BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
      System.out.println((i + 1) + ". " + hash + " -> " + result.verified);
    }
  }
}

package io.github.cogitatortech.tamarind.test;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Verify the specific hash we're using works for "admin" */
public class VerifyHash {
  public static void main(String[] args) {
    String password = "admin";

    // The hash we're now using
    String hash = "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW";

    System.out.println("Password: " + password);
    System.out.println("Hash: " + hash);

    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
    System.out.println("Verification result: " + result.verified);
    System.out.println();

    if (result.verified) {
      System.out.println("✅ SUCCESS! This hash correctly verifies password 'admin'");
      System.out.println("You can now login with username: admin, password: admin");
    } else {
      System.out.println("❌ FAILED! This hash does NOT match password 'admin'");
      System.out.println("Generating a new valid hash:");
      String newHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
      System.out.println(newHash);
    }
  }
}

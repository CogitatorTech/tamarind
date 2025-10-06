package io.github.cogitatortech.tamarind;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/** The main entry point for the Tamarind data lakehouse server. */
@QuarkusMain
public final class TamarindServer {

  private TamarindServer() {}

  /**
   * The main method that serves as the application's entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    Quarkus.run(args);
  }
}

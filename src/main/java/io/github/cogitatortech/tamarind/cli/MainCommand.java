package io.github.cogitatortech.tamarind.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** The main command for the Tamarind CLI. */
@Command(
    name = "tamarind",
    mixinStandardHelpOptions = true,
    version = "Tamarind 0.1.0-SNAPSHOT",
    description = "A fast and flexible data lakehouse server built with DuckDB and Java",
    subcommands = {QueryCommand.class})
public class MainCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println("Tamarind Data Lakehouse Server");
    System.out.println("Use --help to see available commands");
    return 0;
  }
}

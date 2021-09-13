package com.uci.utils.telemetry.util;

/**
 * enum for telemetry events
 *
 * @author arvind.
 */
public enum TelemetryEvents {
  AUDIT("AUDIT"),
  ASSESS("ASSESS"),
  SEARCH("SEARCH"),
  LOG("LOG"),
  ERROR("ERROR"),
  START("START"),
  INTERACT("INTERACT");
  private String name;

  TelemetryEvents(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}

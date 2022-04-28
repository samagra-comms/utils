package com.uci.utils.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Telemetry V3 POJO to generate telemetry event. */
@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
public class Telemetry {

  private static final ObjectMapper mapper = new ObjectMapper();

  private String eid;

  private long ets ;

  private String ver;

  private String mid;

  private Actor actor;

  private Context context;

  private Target object;

  private Map<String, Object> edata;

  private List<String> tags;

  private String type;
  
  private List<String> flags;
  
  private String timestamp;
  
  public static String getTelemetryRequestData(Telemetry telemetry) {
    String event = "";
    try {
      event = mapper.writeValueAsString(telemetry);
    } catch (Exception e) {
    }
    return event;
  }
}

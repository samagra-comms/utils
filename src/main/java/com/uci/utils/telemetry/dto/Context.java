/** */
package com.uci.utils.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@Builder
public class Context {

  private String channel;
  private Producer pdata;
  private String env;
  private String did;
  private String sid;
  private List<Map<String, Object>> cdata;
  private Map<String, String> rollup;

}

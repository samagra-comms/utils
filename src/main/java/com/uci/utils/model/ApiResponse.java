package com.uci.utils.model;

import com.uci.utils.bot.util.DateUtil;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Setter
@Getter
public class ApiResponse {
    public String id;
    @Builder.Default
    public String ver = "3.0";
    @Builder.Default
    public String ts = DateUtil.convertLocalDateTimeToFormat(LocalDateTime.now());
    public ApiResponseParams params;
    public String responseCode;
    public Object result;
}

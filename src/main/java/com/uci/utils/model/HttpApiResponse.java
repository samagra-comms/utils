package com.uci.utils.model;

import com.uci.utils.bot.util.DateUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class HttpApiResponse {
    @Builder.Default
    public String timestamp = DateUtil.convertLocalDateTimeToFormat(LocalDateTime.now());
    public Integer status;
    public String error;
    public String message;
    public String path;
    public Object result;
}

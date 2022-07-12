package com.uci.utils.model;

import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ApiResponseParams {
    @Builder.Default
    public String resmsgid = UUID.randomUUID().toString();
    public String msgid;
    @Builder.Default
    public String status = "success";
    public String err;
    public String errmsg;
}

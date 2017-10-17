package com.rccl.middleware.guest.accounts.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HtmlEmailNotification {
    
    String sender;
    
    String recipient;
    
    String subject;
    
    String content;
    
    @JsonProperty("cc")
    String carbonCopy;
    
    @JsonProperty("bcc")
    String blindCarbonCopy;
    
    @Builder.Default
    String encoding = "UTF-8";
    
    @Builder.Default
    String contentType = "text/html; charset=utf-8";
}

package com.rccl.middleware.guest.accounts.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailNotification implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
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
    String contentType = "multipart/related; type=\"text/html\"";
}

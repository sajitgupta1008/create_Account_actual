package com.rccl.middleware.guest.saviynt.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * The representation of a Saviynt error payload.
 * <p>
 * <b>Note:</b> Saviynt's error payloads are unstable and occasionally come in one of the following
 * three fields: <i>errorMessage</i>, <i>errormessage</i>, or <i>message</i>.
 * <p>
 * Additional, unknown properties are ignored, should there be any.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ErrorPayload {
    
    Integer errorCode;
    
    String errorMessage;
    
    @JsonProperty("SavCode")
    String saviyntErrorCode;
    
    Integer statusCode;
}

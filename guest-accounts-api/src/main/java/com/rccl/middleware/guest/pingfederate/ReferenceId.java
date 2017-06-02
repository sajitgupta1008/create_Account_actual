package com.rccl.middleware.guest.pingfederate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * A reference ID returned by Ping Federate.
 *
 * @author unascribed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class ReferenceId {
    
    @JsonProperty("REF")
    String value;
}

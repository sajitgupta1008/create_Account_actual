package com.rccl.middleware.guest.accounts.enriched;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.Brand;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebshopperInformation implements Jsonable {
    
    private static final long serialVersionUID = 1L;
    
    @Pattern(regexp = "\\d*", message = "Webshopper ID must be in numeric format.")
    String shopperId;
    
    @Brand
    Character brand;
}

package com.rccl.middleware.guest.accounts.enriched;

import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.common.validation.validator.Brand;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
public class WebshopperInformation implements Jsonable {
    
    @Pattern(regexp = "\\d*", message = "Webshopper ID must be in numeric format.")
    String shopperId;
    
    @Brand
    Character brand;
}

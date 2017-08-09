package com.rccl.middleware.guest.accounts.enriched;

import com.rccl.middleware.common.validation.validator.Brand;

import javax.validation.constraints.Pattern;

public class WebshopperInformation {
    
    @Pattern(regexp = "\\d*", message = "Webshopper ID must be in numeric format.")
    String shopperId;
    
    @Brand
    Character brand;
}

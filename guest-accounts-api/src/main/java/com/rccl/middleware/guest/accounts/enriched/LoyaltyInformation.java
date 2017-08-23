package com.rccl.middleware.guest.accounts.enriched;

import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
public class LoyaltyInformation implements Jsonable {
    
    @Pattern(regexp = "\\d*", message = "Consumer ID must be in numeric format.")
    String consumerId;
    
    @Pattern(regexp = "\\d*", message = "Crown and Anchor Loyalty ID must be in numeric format.")
    String crownAndAnchorId;
    
    @Pattern(regexp = "\\d*", message = "Captains Club Loyalty ID must be in numeric format.")
    String captainsClubId;
    
    @Pattern(regexp = "\\d*", message = "Azamara Loyalty ID must be in numeric format.")
    String azamaraLoyaltyId;
    
    @Pattern(regexp = "\\d*", message = "Club Royale Loyalty ID must be in numeric format.")
    String clubRoyaleId;
    
    @Pattern(regexp = "\\d*", message = "Celebrity Blue Chip Loyalty ID must be in numeric format.")
    String celebrityBlueChipId;
}

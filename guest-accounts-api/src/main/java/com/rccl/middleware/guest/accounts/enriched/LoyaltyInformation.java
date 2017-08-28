package com.rccl.middleware.guest.accounts.enriched;

import com.lightbend.lagom.serialization.Jsonable;
import com.rccl.middleware.guestprofiles.models.CaptainsClubLoyaltyTier;
import com.rccl.middleware.guestprofiles.models.CelebrityBlueChipLoyaltyTier;
import com.rccl.middleware.guestprofiles.models.ClubRoyaleLoyaltyTier;
import com.rccl.middleware.guestprofiles.models.CrownAndAnchorSocietyLoyaltyTier;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Pattern;

@Builder
@Value
public class LoyaltyInformation implements Jsonable {
    
    @Pattern(regexp = "\\d*", message = "Crown and Anchor Loyalty ID must be in numeric format.")
    String crownAndAnchorId;
    
    CrownAndAnchorSocietyLoyaltyTier crownAndAnchorSocietyLoyaltyTier;
    
    Integer crownAndAnchorSocietyLoyaltyIndividualPoints;
    
    Integer crownAndAnchorSocietyLoyaltyRelationshipPoints;
    
    @Pattern(regexp = "\\d*", message = "Captains Club Loyalty ID must be in numeric format.")
    String captainsClubId;
    
    CaptainsClubLoyaltyTier captainsClubLoyaltyTier;
    
    Integer captainsClubLoyaltyIndividualPoints;
    
    Integer captainsClubLoyaltyRelationshipPoints;
    
    @Pattern(regexp = "\\d*", message = "Azamara Loyalty ID must be in numeric format.")
    String azamaraLoyaltyId;
    
    @Pattern(regexp = "\\d*", message = "Club Royale Loyalty ID must be in numeric format.")
    String clubRoyaleId;
    
    ClubRoyaleLoyaltyTier clubRoyaleLoyaltyTier;
    
    Integer clubRoyaleLoyaltyIndividualPoints;
    
    Integer clubRoyaleLoyaltyRelationshipPoints;
    
    @Pattern(regexp = "\\d*", message = "Celebrity Blue Chip Loyalty ID must be in numeric format.")
    String celebrityBlueChipId;
    
    CelebrityBlueChipLoyaltyTier celebrityBlueChipLoyaltyTier;
    
    Integer celebrityBlueChipLoyaltyIndividualPoints;
    
    Integer celebrityBlueChipLoyaltyRelationshipPoints;
}

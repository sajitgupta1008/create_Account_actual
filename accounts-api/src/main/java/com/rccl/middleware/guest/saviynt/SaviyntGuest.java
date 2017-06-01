package com.rccl.middleware.guest.saviynt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaviyntGuest implements Jsonable {
    
    /**
     * The account first name.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    String firstname;
    
    /**
     * The account last name.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    String lastname;
    
    /**
     * The displayed name: first + last.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    String displayname;
    
    /**
     * The account username.
     * <p>
     * According to Saviynt, this is: firstname + " " + lastname
     */
    @JsonProperty("Username")
    String username;
    
    /**
     * The account email and main identifier.
     */
    String email;
    
    /**
     * The account password.
     */
    char[] password;
    
    /**
     * This is the security question.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    String securityquestion;
    
    /**
     * This is the security answer.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    String securityanswer;
    
    /**
     * The UCM Consumer ID
     */
    @JsonProperty("customproperty1")
    String consumerId;
    
    /**
     * This is the list of Crown And Anchor loyalty Ids of program type 1.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("loyaltyidsbyprogramtype1")
    List<String> crownAndAnchorIds;
    
    /**
     * This is the list of Captains Club loyalty Ids of program type 1.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("loyaltyidsbyprogramtype2")
    List<String> captainsClubIds;
    
    /**
     * This is the list of Azamara loyalty Ids of program type 1.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("loyaltyidsbyprogramtype3")
    List<String> azamaraLoyaltyIds;
    
    /**
     * This is the list of Club Royale loyalty Ids of program type 1.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("loyaltyidsbyprogramtype4")
    List<String> clubRoyaleIds;
    
    /**
     * This is the list of Celebrity Blue Chip loyalty Ids of program type 1.
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("loyaltyidsbyprogramtype5")
    List<String> celebrityBlueChipIds;
    
    /**
     * This is the list of Royal booking IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("bookingidsbybrandroyal")
    List<String> royalBookingIds;
    
    /**
     * This is the list of Celebrity booking IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("bookingidsbybrandcelebrity")
    List<String> celebrityBookingIds;
    
    /**
     * This is the list of Azamara booking IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("bookingidsbybrandazamara")
    List<String> azamaraBookingIds;
    
    /**
     * This is the list of Royal WebShopper IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("shopperidsbybrandroyal")
    List<String> royalWebShopperIds;
    
    /**
     * This is the list of Celebrity WebShopper IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("shopperidsbybrandcelebrity")
    List<String> celebrityWebShopperIds;
    
    /**
     * This is the list of Azamara WebShopper IDs
     * <p>
     * The lack of capitalization is intentional on Saviynt's part.
     */
    @JsonProperty("shopperidsbybrandazamara")
    List<String> azamaraWebShopperIds;
    
    /**
     * Saviynt uses this property to determine how to "find" a user.
     * <p>
     * It should not have to be set.
     */
    final String propertytosearch = "email";
}

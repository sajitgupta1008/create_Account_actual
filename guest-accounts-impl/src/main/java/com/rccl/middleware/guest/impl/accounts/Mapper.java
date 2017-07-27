package com.rccl.middleware.guest.impl.accounts;

import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.saviynt.api.SaviyntGuest;
import com.rccl.middleware.saviynt.api.SaviyntUserType;

import java.util.List;
import java.util.stream.Collectors;

public class Mapper {
    
    /**
     * Includes VDS ID argument value into {@link Guest} model.
     *
     * @param vdsId the given VDS ID of the user.
     * @param guest the {@link Guest} model.
     * @return {@Guest} with vdsId attribute populated.
     */
    public static Guest mapVdsIdWithGuest(String vdsId, Guest guest) {
        return Guest.builder()
                .header(guest.getHeader())
                .vdsId(vdsId)
                .email(guest.getEmail())
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .birthdate(guest.getBirthdate())
                .phoneNumber(guest.getPhoneNumber())
                .securityQuestions(guest.getSecurityQuestions())
                .consumerId(guest.getConsumerId())
                .crownAndAnchorIds(guest.getCrownAndAnchorIds())
                .captainsClubIds(guest.getCaptainsClubIds())
                .azamaraLoyaltyIds(guest.getAzamaraLoyaltyIds())
                .clubRoyaleIds(guest.getClubRoyaleIds())
                .celebrityBlueChipIds(guest.getCelebrityBlueChipIds())
                .azamaraBookingIds(guest.getAzamaraBookingIds())
                .celebrityBookingIds(guest.getCelebrityBookingIds())
                .royalBookingIds(guest.getRoyalBookingIds())
                .azamaraWebShopperIds(guest.getAzamaraWebShopperIds())
                .celebrityWebShopperIds(guest.getCelebrityWebShopperIds())
                .royalWebShopperIds(guest.getRoyalWebShopperIds())
                .royalPrimaryBookingId(guest.getRoyalPrimaryBookingId())
                .celebrityPrimaryBookingId(guest.getCelebrityPrimaryBookingId())
                .azamaraPrimaryBookingId(guest.getAzamaraPrimaryBookingId())
                .termsAndConditionsAgreement(guest.getTermsAndConditionsAgreement())
                .optins(guest.getOptins())
                .build();
    }
    
    /**
     * Includes email argument value into {@link Guest} model.
     *
     * @param email the email address of the user.
     * @param guest the {@link Guest} model.
     * @return {@Guest} with email attribute populated.
     */
    public static Guest mapEmailWithGuest(String email, Guest guest) {
        return Guest.builder()
                .header(guest.getHeader())
                .email(email)
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .birthdate(guest.getBirthdate())
                .phoneNumber(guest.getPhoneNumber())
                .securityQuestions(guest.getSecurityQuestions())
                .consumerId(guest.getConsumerId())
                .crownAndAnchorIds(guest.getCrownAndAnchorIds())
                .captainsClubIds(guest.getCaptainsClubIds())
                .azamaraLoyaltyIds(guest.getAzamaraLoyaltyIds())
                .clubRoyaleIds(guest.getClubRoyaleIds())
                .celebrityBlueChipIds(guest.getCelebrityBlueChipIds())
                .azamaraBookingIds(guest.getAzamaraBookingIds())
                .celebrityBookingIds(guest.getCelebrityBookingIds())
                .royalBookingIds(guest.getRoyalBookingIds())
                .azamaraWebShopperIds(guest.getAzamaraWebShopperIds())
                .celebrityWebShopperIds(guest.getCelebrityWebShopperIds())
                .royalWebShopperIds(guest.getRoyalWebShopperIds())
                .royalPrimaryBookingId(guest.getRoyalPrimaryBookingId())
                .celebrityPrimaryBookingId(guest.getCelebrityPrimaryBookingId())
                .azamaraPrimaryBookingId(guest.getAzamaraPrimaryBookingId())
                .termsAndConditionsAgreement(guest.getTermsAndConditionsAgreement())
                .optins(guest.getOptins())
                .build();
    }
    
    /**
     * Creates a builder which maps the appropriate {@link Guest} values into {@link SaviyntGuest} object
     * based on the action taken.
     *
     * @param guest    the {@link Guest} model
     * @param isCreate determines the request being taken whether it is create or update guest account
     * @return {@link SaviyntGuest.SaviyntGuestBuilder}
     */
    public static SaviyntGuest.SaviyntGuestBuilder mapGuestToSaviyntGuest(Guest guest, boolean isCreate) {
        SaviyntGuest.SaviyntGuestBuilder builder = SaviyntGuest.builder()
                .firstname(guest.getFirstName())
                .lastname(guest.getLastName())
                .displayname(guest.getFirstName() + " " + guest.getLastName())
                .email(guest.getEmail())
                .password(guest.getPassword())
                .dateofBirth(guest.getBirthdate())
                .phoneNumber(guest.getPhoneNumber())
                .consumerId(guest.getConsumerId())
                .crownAndAnchorIds(mapValuesToSaviyntStringFormat(guest.getCrownAndAnchorIds()))
                .captainsClubIds(mapValuesToSaviyntStringFormat(guest.getCaptainsClubIds()))
                .azamaraLoyaltyIds(mapValuesToSaviyntStringFormat(guest.getAzamaraLoyaltyIds()))
                .clubRoyaleIds(mapValuesToSaviyntStringFormat(guest.getClubRoyaleIds()))
                .celebrityBlueChipIds(mapValuesToSaviyntStringFormat(guest.getCelebrityBlueChipIds()))
                .azamaraBookingIds(mapValuesToSaviyntStringFormat(guest.getAzamaraBookingIds()))
                .celebrityBookingIds(mapValuesToSaviyntStringFormat(guest.getCelebrityBookingIds()))
                .royalBookingIds(mapValuesToSaviyntStringFormat(guest.getRoyalBookingIds()))
                .azamaraWebShopperIds(mapValuesToSaviyntStringFormat(guest.getAzamaraWebShopperIds()))
                .celebrityWebShopperIds(mapValuesToSaviyntStringFormat(guest.getCelebrityWebShopperIds()))
                .royalWebShopperIds(mapValuesToSaviyntStringFormat(guest.getRoyalWebShopperIds()))
                .royalPrimaryBookingId(guest.getRoyalPrimaryBookingId())
                .celebrityPrimaryBookingId(guest.getCelebrityPrimaryBookingId())
                .azamaraPrimaryBookingId(guest.getAzamaraPrimaryBookingId())
                .propertytosearch("email");
        
        // only map the account creation specific attributes
        if (isCreate) {
            builder.username(guest.getEmail())
                    .userType(SaviyntUserType.Guest);
        }
        
        List<SecurityQuestion> securityQuestions = guest.getSecurityQuestions();
        
        if (securityQuestions != null && !securityQuestions.isEmpty()) {
            SecurityQuestion sq = securityQuestions.get(0);
            
            builder.securityquestion(sq.getQuestion()).securityanswer(sq.getAnswer());
        }
        
        if (guest.getTermsAndConditionsAgreement() != null) {
            builder.termsAndConditionsVersion(guest.getTermsAndConditionsAgreement().getVersion());
        }
        
        return builder;
    }
    
    /**
     * Wraps each {@link List} value with quotation marks to satisfy Saviynt's requirement.
     *
     * @param attributeList {@code List<String>}
     * @return {@code List<String>}
     */
    private static List<String> mapValuesToSaviyntStringFormat(List<String> attributeList) {
        if (attributeList != null && !attributeList.isEmpty()) {
            return attributeList
                    .stream()
                    .map(val -> "\"" + val + "\"")
                    .collect(Collectors.toList());
        }
        
        return null;
    }
}

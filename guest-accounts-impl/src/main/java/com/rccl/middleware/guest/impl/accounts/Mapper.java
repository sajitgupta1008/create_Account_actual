package com.rccl.middleware.guest.impl.accounts;

import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.enriched.ContactInformation;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.enriched.LoyaltyInformation;
import com.rccl.middleware.guest.accounts.enriched.PersonalInformation;
import com.rccl.middleware.guest.accounts.enriched.SignInInformation;
import com.rccl.middleware.guest.accounts.enriched.TravelDocumentInformation;
import com.rccl.middleware.guest.accounts.enriched.WebshopperInformation;
import com.rccl.middleware.guest.optin.Optin;
import com.rccl.middleware.guest.optin.Optins;
import com.rccl.middleware.guestprofiles.models.Profile;
import com.rccl.middleware.saviynt.api.SaviyntGuest;
import com.rccl.middleware.saviynt.api.SaviyntUserType;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class Mapper {
    
    /**
     * Includes VDS ID argument value into {@link Guest} model.
     *
     * @param vdsId the given VDS ID of the user.
     * @param guest the {@link Guest} model.
     * @return {@link Guest} with vdsId attribute populated.
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
                .crownAndAnchorId(guest.getCrownAndAnchorId())
                .captainsClubId(guest.getCaptainsClubId())
                .azamaraLoyaltyId(guest.getAzamaraLoyaltyId())
                .clubRoyaleId(guest.getClubRoyaleId())
                .celebrityBlueChipId(guest.getCelebrityBlueChipId())
                .webshopperId(guest.getWebshopperId())
                .webshopperBrand(guest.getWebshopperBrand())
                .termsAndConditionsAgreement(guest.getTermsAndConditionsAgreement())
                .optins(guest.getOptins())
                .build();
    }
    
    /**
     * Includes email argument value into {@link Guest} model.
     *
     * @param email the email address of the user.
     * @param guest the {@link Guest} model.
     * @return {@link Guest} with email attribute populated.
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
                .crownAndAnchorId(guest.getCrownAndAnchorId())
                .captainsClubId(guest.getCaptainsClubId())
                .azamaraLoyaltyId(guest.getAzamaraLoyaltyId())
                .clubRoyaleId(guest.getClubRoyaleId())
                .celebrityBlueChipId(guest.getCelebrityBlueChipId())
                .webshopperId(guest.getWebshopperId())
                .webshopperBrand(guest.getWebshopperBrand())
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
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .displayName(guest.getFirstName() + " " + guest.getLastName())
                .email(guest.getEmail())
                .password(guest.getPassword())
                .birthdate(guest.getBirthdate())
                .phoneNumber(guest.getPhoneNumber())
                .consumerId(guest.getConsumerId())
                .crownAndAnchorIds(mapStringToSaviyntStringList(guest.getCrownAndAnchorId()))
                .captainsClubIds(mapStringToSaviyntStringList(guest.getCaptainsClubId()))
                .azamaraLoyaltyIds(mapStringToSaviyntStringList(guest.getAzamaraLoyaltyId()))
                .clubRoyaleIds(mapStringToSaviyntStringList(guest.getClubRoyaleId()))
                .celebrityBlueChipIds(mapStringToSaviyntStringList(guest.getCelebrityBlueChipId()))
                .webshopperId(guest.getWebshopperId())
                .webshopperBrand(guest.getWebshopperBrand())
                .vdsId(guest.getVdsId())
                .propertyToSearch("systemUserName");
        
        // only map the account creation specific attributes
        if (isCreate) {
            builder.username(guest.getEmail())
                    .userType(SaviyntUserType.Guest);
        }
        
        List<SecurityQuestion> securityQuestions = guest.getSecurityQuestions();
        
        if (securityQuestions != null && !securityQuestions.isEmpty()) {
            SecurityQuestion sq = securityQuestions.get(0);
            
            builder.securityQuestion(sq.getQuestion()).securityAnswer(sq.getAnswer());
        }
        
        if (guest.getTermsAndConditionsAgreement() != null) {
            builder.termsAndConditionsVersion(guest.getTermsAndConditionsAgreement().getVersion());
        }
        
        return builder;
    }
    
    /**
     * Extracts all necessary information from {@link EnrichedGuest} and maps those attributes
     * in {@link Guest} for Update Guest Account service.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link Guest.GuestBuilder}
     */
    public static Guest.GuestBuilder mapEnrichedGuestToGuest(EnrichedGuest guest) {
        PersonalInformation personalInfo = guest.getPersonalInformation();
        LoyaltyInformation loyaltyInfo = guest.getLoyaltyInformation();
        ContactInformation contactInfo = guest.getContactInformation();
        WebshopperInformation webshopperInfo = guest.getWebshopperInformation();
        SignInInformation signInInfo = guest.getSignInInformation();
        Guest.GuestBuilder builder = Guest.builder();
        
        if (personalInfo != null) {
            builder.firstName(personalInfo.getFirstName())
                    .lastName(personalInfo.getLastName())
                    .middleName(personalInfo.getMiddleName())
                    .suffix(personalInfo.getSuffix())
                    .birthdate(personalInfo.getBirthdate());
        }
        
        if (signInInfo != null) {
            builder.email(signInInfo.getEmail())
                    .password(signInInfo.getPassword())
                    .securityQuestions(signInInfo.getSecurityQuestions());
        }
        
        if (loyaltyInfo != null) {
            builder.crownAndAnchorId(loyaltyInfo.getCrownAndAnchorId())
                    .captainsClubId(loyaltyInfo.getCaptainsClubId())
                    .azamaraLoyaltyId(loyaltyInfo.getAzamaraLoyaltyId())
                    .clubRoyaleId(loyaltyInfo.getClubRoyaleId())
                    .celebrityBlueChipId(loyaltyInfo.getCelebrityBlueChipId());
        }
        
        if (contactInfo != null) {
            // TODO add phone country code here.
            builder.phoneNumber(contactInfo.getPhoneNumber());
        }
        
        if (webshopperInfo != null) {
            builder.webshopperId(webshopperInfo.getShopperId())
                    .webshopperBrand(webshopperInfo.getBrand());
        }
        
        return builder;
    }
    
    /**
     * Extracts all necessary information from {@link EnrichedGuest} and maps those attributes
     * in {@link Profile} for Update Profile service.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link Profile.ProfileBuilder}
     */
    public static Profile.ProfileBuilder mapEnrichedGuestToProfile(EnrichedGuest guest) {
        PersonalInformation personalInfo = guest.getPersonalInformation();
        ContactInformation contactInfo = guest.getContactInformation();
        TravelDocumentInformation travelInfo = guest.getTravelDocumentInformation();
        Profile.ProfileBuilder builder = Profile.builder();
        
        if (personalInfo != null) {
            builder.avatar(personalInfo.getAvatar())
                    .nickname(personalInfo.getNickname())
                    .gender(personalInfo.getGender());
            
        }
        
        if (contactInfo != null) {
            builder.address(contactInfo.getAddress());
        }
        
        if (travelInfo != null) {
            builder.birthCountryCode(travelInfo.getBirthCountryCode())
                    .citizenshipCountryCode(travelInfo.getCitizenshipCountryCode());
        }
        
        if (guest.getEmergencyContact() != null) {
            builder.emergencyContact(guest.getEmergencyContact());
        }
        
        return builder;
    }
    
    /**
     * Extracts all necessary information from {@link EnrichedGuest} and maps those attributes
     * in {@link Optins} for Update Optins service.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link Optins}
     */
    public static Optins mapEnrichedGuestToOptins(EnrichedGuest guest) {
        List<Optin> optins = guest.getOptins();
        
        if (optins != null && !optins.isEmpty()) {
            return Optins.builder()
                    .optins(optins)
                    .email(guest.getSignInInformation().getEmail())
                    .header(guest.getHeader())
                    .build();
        }
        
        return null;
    }
    
    /**
     * Wraps each {@link String} value with quotation marks to satisfy Saviynt's requirement.
     *
     * @param attribute {@code String}
     * @return {@code List<String>}
     */
    private static List<String> mapStringToSaviyntStringList(String attribute) {
        if (StringUtils.isNotBlank(attribute)) {
            return Arrays.asList("\"" + attribute + "\"");
        }
        
        return null;
    }
}

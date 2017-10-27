package com.rccl.middleware.guest.impl.accounts;

import com.rccl.middleware.common.response.ResponseBody;
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
import com.rccl.middleware.saviynt.api.requests.SaviyntGuest;
import com.rccl.middleware.saviynt.api.requests.SaviyntUserType;
import com.rccl.middleware.saviynt.api.responses.AccountInformation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Mapper {
    
    private Mapper() {
        // No-op
    }
    
    /**
     * Includes VDS ID argument value into {@link Guest} model as well as the creation timestamp
     * which is for the Kafka event.
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
                .creationTimestamp(DateTimeFormatter.ofPattern("yyyyMMdd'T'hhmmssz")
                        .withZone(ZoneId.of("UTC")).format(ZonedDateTime.now()))
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
                .middleName(guest.getMiddleName())
                .suffix(guest.getSuffix())
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
                .passportNumber(guest.getPassportNumber())
                .passportExpirationDate(guest.getPassportExpirationDate())
                .vdsId(guest.getVdsId())
                .propertyToSearch("systemUserName");
        
        // only map the account creation specific attributes
        if (isCreate) {
            builder.userType(SaviyntUserType.Guest);
        }
        
        // if password is specified for update service, map the following attributes with values which 
        // are not supposed to change. See {@link SaviyntGuest} Java-Doc for documentation.
        if (!isCreate && guest.getPassword() != null && StringUtils.isNotBlank(guest.getPassword().toString())) {
            builder.passwordInetUserStatus("Active")
                    .passwordInvalidAttemptsData("[]")
                    .passwordAccountLock("False")
                    .passwordReset("false");
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
     * Creates a {@link Guest} object which maps the appropriate {@link SaviyntGuest} attributes.
     *
     * @param accountInformation the {@link AccountInformation} Saviynt response object
     * @return {@link Guest}
     */
    public static Guest mapSaviyntGuestToGuest(AccountInformation accountInformation) {
        SaviyntGuest saviyntGuest = accountInformation.getGuest();
        return Guest.builder()
                .firstName(saviyntGuest.getFirstName())
                .lastName(saviyntGuest.getLastName())
                .middleName(saviyntGuest.getMiddleName())
                .suffix(saviyntGuest.getSuffix())
                .email(saviyntGuest.getEmail())
                .phoneNumber(saviyntGuest.getPhoneNumber())
                .vdsId(saviyntGuest.getVdsId())
                .password(saviyntGuest.getPassword())
                .birthdate(saviyntGuest.getBirthdate())
                .consumerId(saviyntGuest.getConsumerId())
                .crownAndAnchorId(CollectionUtils.isEmpty(saviyntGuest.getCrownAndAnchorIds())
                        ? null : saviyntGuest.getCrownAndAnchorIds().get(0))
                .captainsClubId(CollectionUtils.isEmpty(saviyntGuest.getCaptainsClubIds())
                        ? null : saviyntGuest.getCaptainsClubIds().get(0))
                .azamaraLoyaltyId(CollectionUtils.isEmpty(saviyntGuest.getAzamaraLoyaltyIds())
                        ? null : saviyntGuest.getAzamaraLoyaltyIds().get(0))
                .clubRoyaleId(CollectionUtils.isEmpty(saviyntGuest.getClubRoyaleIds())
                        ? null : saviyntGuest.getClubRoyaleIds().get(0))
                .celebrityBlueChipId(CollectionUtils.isEmpty(saviyntGuest.getCelebrityBlueChipIds())
                        ? null : saviyntGuest.getCelebrityBlueChipIds().get(0))
                .webshopperId(saviyntGuest.getWebshopperId())
                .webshopperBrand(saviyntGuest.getWebshopperBrand())
                .passportNumber(saviyntGuest.getPassportNumber())
                .passportExpirationDate(saviyntGuest.getPassportExpirationDate())
                .build();
    }
    
    /**
     * Extracts all necessary information from {@link EnrichedGuest} and maps those attributes
     * in {@link Guest} for Update Guest Account service.
     * <p>
     * Note that this does not include the {@link LoyaltyInformation} part of {@link EnrichedGuest} object
     * since it needs to undergo Siebel validation first.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link Guest.GuestBuilder}
     */
    public static Guest.GuestBuilder mapEnrichedGuestToGuest(EnrichedGuest guest) {
        Guest.GuestBuilder builder = Guest.builder();
        
        PersonalInformation personalInfo = guest.getPersonalInformation();
        if (personalInfo != null) {
            // as per business rule, last name should not be allowed to be updated.
            builder.firstName(personalInfo.getFirstName())
                    .middleName(personalInfo.getMiddleName())
                    .suffix(personalInfo.getSuffix())
                    .birthdate(personalInfo.getBirthdate());
        }
        
        SignInInformation signInInfo = guest.getSignInInformation();
        if (signInInfo != null) {
            builder.password(signInInfo.getPassword())
                    .securityQuestions(signInInfo.getSecurityQuestions());
        }
        
        ContactInformation contactInfo = guest.getContactInformation();
        if (contactInfo != null) {
            // TODO add phone country code here.
            builder.phoneNumber(contactInfo.getPhoneNumber());
        }
        
        TravelDocumentInformation travelDocInfo = guest.getTravelDocumentInformation();
        if (travelDocInfo != null) {
            builder.passportNumber(travelDocInfo.getPassportNumber())
                    .passportExpirationDate(travelDocInfo.getPassportExpirationDate());
        }
        
        WebshopperInformation webshopperInfo = guest.getWebshopperInformation();
        if (webshopperInfo != null) {
            builder.webshopperId(webshopperInfo.getShopperId())
                    .webshopperBrand(webshopperInfo.getBrand());
        }
        
        if (StringUtils.isNotBlank(guest.getEmail())) {
            builder.email(guest.getEmail());
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
        Profile.ProfileBuilder builder = Profile.builder();
        
        PersonalInformation personalInfo = guest.getPersonalInformation();
        if (personalInfo != null) {
            builder.avatar(personalInfo.getAvatar())
                    .nickname(personalInfo.getNickname())
                    .gender(personalInfo.getGender());
            
        }
        
        ContactInformation contactInfo = guest.getContactInformation();
        if (contactInfo != null) {
            builder.address(contactInfo.getAddress());
        }
        
        TravelDocumentInformation travelInfo = guest.getTravelDocumentInformation();
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
                    .email(guest.getEmail())
                    .header(guest.getHeader())
                    .build();
        }
        
        return null;
    }
    
    /**
     * Maps the individual model values into the {@link EnrichedGuest} model.
     *
     * @param guest               the {@link Guest} model from accounts service.
     * @param profileResponseBody the {@link ResponseBody}<{@link Profile}> model from profiles service.
     * @param optinsResponseBody  the {@link ResponseBody}<{@link Optins}> model from optins service.
     * @return {@link EnrichedGuest}
     */
    public static EnrichedGuest mapToEnrichedGuest(Guest guest,
                                                   ResponseBody<Profile> profileResponseBody,
                                                   ResponseBody<Optins> optinsResponseBody) {
        
        PersonalInformation.PersonalInformationBuilder personalInformationBuilder = PersonalInformation.builder();
        ContactInformation.ContactInformationBuilder contactInformationBuilder = ContactInformation.builder();
        TravelDocumentInformation.TravelDocumentInformationBuilder
                travelDocumentInformation = TravelDocumentInformation.builder();
        LoyaltyInformation.LoyaltyInformationBuilder loyaltyInformationBuilder = LoyaltyInformation.builder();
        
        EnrichedGuest.EnrichedGuestBuilder enrichedGuestBuilder = EnrichedGuest.builder();
        
        if (guest != null) {
            personalInformationBuilder.firstName(guest.getFirstName())
                    .lastName(guest.getLastName())
                    .middleName(guest.getMiddleName())
                    .suffix(guest.getSuffix())
                    .birthdate(guest.getBirthdate());
            
            contactInformationBuilder.phoneNumber(guest.getPhoneNumber())
                    .phoneCountryCode("");
            
            travelDocumentInformation.passportNumber(guest.getPassportNumber())
                    .passportExpirationDate(guest.getPassportExpirationDate());
            
            loyaltyInformationBuilder.crownAndAnchorId(guest.getCrownAndAnchorId())
                    .captainsClubId(guest.getCaptainsClubId())
                    .azamaraLoyaltyId(guest.getAzamaraLoyaltyId())
                    .clubRoyaleId(guest.getClubRoyaleId())
                    .celebrityBlueChipId(guest.getCelebrityBlueChipId());
            
            enrichedGuestBuilder.vdsId(guest.getVdsId())
                    .email(guest.getEmail())
                    .consumerId(guest.getConsumerId());
        }
        
        if (profileResponseBody != null && profileResponseBody.getPayload() != null) {
            Profile profile = profileResponseBody.getPayload();
            personalInformationBuilder.avatar(profile.getAvatar())
                    .nickname(profile.getNickname())
                    .gender(profile.getGender());
            
            contactInformationBuilder.address(profile.getAddress());
            
            travelDocumentInformation.citizenshipCountryCode(profile.getCitizenshipCountryCode())
                    .birthCountryCode(profile.getBirthCountryCode());
            
            loyaltyInformationBuilder.crownAndAnchorSocietyLoyaltyTier(profile.getCrownAndAnchorSocietyLoyaltyTier())
                    .crownAndAnchorSocietyLoyaltyIndividualPoints(profile
                            .getCrownAndAnchorSocietyLoyaltyIndividualPoints())
                    .crownAndAnchorSocietyLoyaltyRelationshipPoints(profile
                            .getCrownAndAnchorSocietyLoyaltyRelationshipPoints())
                    .captainsClubLoyaltyTier(profile.getCaptainsClubLoyaltyTier())
                    .captainsClubLoyaltyIndividualPoints(profile.getCaptainsClubLoyaltyIndividualPoints())
                    .captainsClubLoyaltyRelationshipPoints(profile.getCaptainsClubLoyaltyRelationshipPoints())
                    .clubRoyaleLoyaltyTier(profile.getClubRoyaleLoyaltyTier())
                    .clubRoyaleLoyaltyIndividualPoints(profile.getClubRoyaleLoyaltyIndividualPoints())
                    .clubRoyaleLoyaltyRelationshipPoints(profile.getClubRoyaleLoyaltyRelationshipPoints())
                    .celebrityBlueChipLoyaltyTier(profile.getCelebrityBlueChipLoyaltyTier())
                    .celebrityBlueChipLoyaltyIndividualPoints(profile.getCelebrityBlueChipLoyaltyIndividualPoints())
                    .celebrityBlueChipLoyaltyRelationshipPoints(profile
                            .getCelebrityBlueChipLoyaltyRelationshipPoints());
            
            enrichedGuestBuilder.emergencyContact(profile.getEmergencyContact());
        }
        
        if (optinsResponseBody != null && optinsResponseBody.getPayload() != null) {
            enrichedGuestBuilder.optins(optinsResponseBody.getPayload().getOptins());
        }
        
        return enrichedGuestBuilder
                .personalInformation(personalInformationBuilder.build())
                .contactInformation(contactInformationBuilder.build())
                .travelDocumentInformation(travelDocumentInformation.build())
                .loyaltyInformation(loyaltyInformationBuilder.build())
                .build();
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
        
        return Collections.emptyList();
    }
}

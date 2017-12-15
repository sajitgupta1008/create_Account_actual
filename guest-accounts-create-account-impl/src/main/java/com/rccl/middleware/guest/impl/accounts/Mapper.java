package com.rccl.middleware.guest.impl.accounts;

import com.rccl.middleware.common.response.ResponseBody;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.PrivacyPolicyAgreement;
import com.rccl.middleware.guest.accounts.SecurityQuestion;
import com.rccl.middleware.guest.accounts.TermsAndConditionsAgreement;
import com.rccl.middleware.guest.accounts.enriched.ContactInformation;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.enriched.LoyaltyInformation;
import com.rccl.middleware.guest.accounts.enriched.PersonalInformation;
import com.rccl.middleware.guest.accounts.enriched.SignInInformation;
import com.rccl.middleware.guest.accounts.enriched.TravelDocumentInformation;
import com.rccl.middleware.guest.accounts.enriched.WebshopperInformation;
import com.rccl.middleware.guest.optin.EmailOptin;
import com.rccl.middleware.guest.optin.EmailOptins;
import com.rccl.middleware.guest.optin.PostalOptin;
import com.rccl.middleware.guest.optin.PostalOptins;
import com.rccl.middleware.guestprofiles.models.Profile;
import com.rccl.middleware.saviynt.api.requests.SaviyntGuest;
import com.rccl.middleware.saviynt.api.requests.SaviyntUserType;
import com.rccl.middleware.saviynt.api.responses.AccountInformation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Mapper {
    
    private Mapper() {
        // No-op
    }
    
    /**
     * Includes VDS ID argument value into {@link Guest} model as well as the creation timestamp
     * which is for the Kafka event message.
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
                .password(guest.getPassword())
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
                .creationTimestamp(DateTimeFormatter.ofPattern("yyyyMMdd'T'hhmmssz")
                        .withZone(ZoneId.of("UTC")).format(ZonedDateTime.now()))
                .build();
    }
    
    /**
     * Maps old {@link Guest} into a new {@link Guest} with updated attribute values where newGuest
     * is taking precedence. If newGuest's attribute is null, oldGuest value will take place.
     *
     * @param oldGuest the current {@link Guest} from service invocation request.
     * @param newGuest the updated {@link Guest} object.
     * @return {@link Guest}
     */
    public static Guest mapCurrentGuestToUpdatedGuest(Guest oldGuest, Guest newGuest) {
        return Guest.builder()
                .header(newGuest.getHeader() == null ? oldGuest.getHeader() : newGuest.getHeader())
                .vdsId(newGuest.getVdsId() == null ? oldGuest.getVdsId() : newGuest.getVdsId())
                .email(newGuest.getEmail() == null ? oldGuest.getEmail() : newGuest.getEmail())
                .firstName(newGuest.getFirstName() == null ? oldGuest.getFirstName() : newGuest.getFirstName())
                .lastName(newGuest.getLastName() == null ? oldGuest.getLastName() : newGuest.getLastName())
                .birthdate(newGuest.getBirthdate() == null ? oldGuest.getBirthdate() : newGuest.getBirthdate())
                .phoneNumber(newGuest.getPassportNumber() == null ? oldGuest.getPhoneNumber()
                        : newGuest.getPhoneNumber())
                .securityQuestions(CollectionUtils.isEmpty(newGuest.getSecurityQuestions())
                        ? oldGuest.getSecurityQuestions() : newGuest.getSecurityQuestions())
                .consumerId(newGuest.getConsumerId() == null ? oldGuest.getConsumerId() : newGuest.getConsumerId())
                .crownAndAnchorId(newGuest.getCrownAndAnchorId() == null ? oldGuest.getCrownAndAnchorId()
                        : newGuest.getCrownAndAnchorId())
                .captainsClubId(newGuest.getCaptainsClubId() == null ? oldGuest.getCaptainsClubId() :
                        newGuest.getCaptainsClubId())
                .azamaraLoyaltyId(newGuest.getAzamaraLoyaltyId() == null ? oldGuest.getAzamaraLoyaltyId()
                        : newGuest.getAzamaraLoyaltyId())
                .clubRoyaleId(newGuest.getClubRoyaleId() == null ? oldGuest.getClubRoyaleId() :
                        newGuest.getClubRoyaleId())
                .celebrityBlueChipId(newGuest.getCelebrityBlueChipId() == null
                        ? oldGuest.getCelebrityBlueChipId() : newGuest.getCelebrityBlueChipId())
                .webshopperId(newGuest.getWebshopperId() == null ? oldGuest.getWebshopperId()
                        : newGuest.getWebshopperId())
                .webshopperBrand(newGuest.getWebshopperBrand() == null ? oldGuest.getWebshopperBrand()
                        : newGuest.getWebshopperBrand())
                .privacyPolicyAgreement(newGuest.getPrivacyPolicyAgreement() == null ?
                        oldGuest.getPrivacyPolicyAgreement() : newGuest.getPrivacyPolicyAgreement())
                .termsAndConditionsAgreement(newGuest.getTermsAndConditionsAgreement() == null
                        ? oldGuest.getTermsAndConditionsAgreement() : newGuest.getTermsAndConditionsAgreement())
                .creationTimestamp(newGuest.getCreationTimestamp() == null
                        ? oldGuest.getCreationTimestamp() : newGuest.getCreationTimestamp())
                .passportNumber(newGuest.getPassportNumber() == null ? oldGuest.getPassportNumber()
                        : newGuest.getPassportNumber())
                .passportExpirationDate(newGuest.getPassportExpirationDate() == null ?
                        oldGuest.getPassportExpirationDate() : newGuest.getPassportExpirationDate())
                .build();
    }
    
    /**
     * Creates a builder which maps the appropriate {@link Guest} values into {@link SaviyntGuest} object
     * based on the action taken.
     * <p>
     * Note: Loyalty IDs should NOT be mapped directly to create or update account Saviynt service unless
     * those are being updated to empty string "". Any addition or modification of loyalty IDs must
     * go to a verification process through Kafka message going to Tibco.
     * </p>
     *
     * @param guest    the {@link Guest} model
     * @param isCreate determines the request being taken whether it is create or update guest account
     * @return {@link SaviyntGuest.SaviyntGuestBuilder}
     */
    public static SaviyntGuest.SaviyntGuestBuilder mapGuestToSaviyntGuest(Guest guest,
                                                                          boolean isCreate) {
        SaviyntGuest.SaviyntGuestBuilder builder = SaviyntGuest.builder()
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .middleName(guest.getMiddleName())
                .suffix(guest.getSuffix())
                .email(guest.getEmail())
                .birthdate(guest.getBirthdate())
                .phoneNumber(guest.getPhoneNumber())
                .consumerId(guest.getConsumerId())
                .webshopperId(guest.getWebshopperId())
                .webshopperBrand(guest.getWebshopperBrand())
                .passportNumber(guest.getPassportNumber())
                .passportExpirationDate(guest.getPassportExpirationDate())
                .vdsId(guest.getVdsId())
                .propertyToSearch("systemUserName");
        
        // Only map the account creation specific attributes.
        if (isCreate) {
            builder.password(guest.getPassword())
                    .userType(SaviyntUserType.Guest);
        } else {
            //map loyalty IDs ONLY if those have empty strings during Update process.
            if ("".equals(guest.getCrownAndAnchorId())) {
                builder.crownAndAnchorId("");
            }
            
            if ("".equals(guest.getCaptainsClubId())) {
                builder.captainsClubId("");
            }
            
            if ("".equals(guest.getAzamaraLoyaltyId())) {
                builder.azamaraLoyaltyId("");
            }
            
            if ("".equals(guest.getCelebrityBlueChipId())) {
                builder.celebrityBlueChipId("");
            }
            
            if ("".equals(guest.getClubRoyaleId())) {
                builder.clubRoyaleId("");
            }
        }
        
        // if password is specified for update service, map the following attributes with values which 
        // are not supposed to change. See {@link SaviyntGuest} Java-Doc for documentation.
        if (!isCreate && guest.getPassword() != null && StringUtils.isNotBlank(String.valueOf(guest.getPassword()))) {
            builder.passwordReset("false");
        }
        
        List<SecurityQuestion> securityQuestions = guest.getSecurityQuestions();
        
        if (securityQuestions != null && !securityQuestions.isEmpty()) {
            SecurityQuestion sq = securityQuestions.get(0);
            
            builder.securityQuestion(sq.getQuestion()).securityAnswer(sq.getAnswer());
        }
        
        TermsAndConditionsAgreement tac = guest.getTermsAndConditionsAgreement();
        if (tac != null) {
            builder.termsAndConditionsAcceptTime(tac.getAcceptTime());
            builder.termsAndConditionsVersion(tac.getVersion());
        }
        
        PrivacyPolicyAgreement ppa = guest.getPrivacyPolicyAgreement();
        if (ppa != null) {
            builder.privacyPolicyAcceptTime(ppa.getAcceptTime());
            builder.privacyPolicyVersion(ppa.getVersion());
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
        SaviyntGuest sg = accountInformation.getGuest();
        
        Guest.GuestBuilder builder = Guest.builder()
                .firstName(sg.getFirstName())
                .lastName(sg.getLastName())
                .middleName(sg.getMiddleName())
                .suffix(sg.getSuffix())
                .email(sg.getEmail())
                .phoneNumber(sg.getPhoneNumber())
                .vdsId(sg.getVdsId())
                .password(sg.getPassword())
                .birthdate(sg.getBirthdate())
                .consumerId(sg.getConsumerId())
                .crownAndAnchorId(sg.getCrownAndAnchorId())
                .captainsClubId(sg.getCaptainsClubId())
                .azamaraLoyaltyId(sg.getAzamaraLoyaltyId())
                .clubRoyaleId(sg.getClubRoyaleId())
                .celebrityBlueChipId(sg.getCelebrityBlueChipId())
                .webshopperId(sg.getWebshopperId())
                .webshopperBrand(sg.getWebshopperBrand())
                .passportNumber(sg.getPassportNumber())
                .passportExpirationDate(sg.getPassportExpirationDate());
        
        TermsAndConditionsAgreement tac = TermsAndConditionsAgreement.builder()
                .acceptTime(sg.getTermsAndConditionsAcceptTime())
                .version(sg.getTermsAndConditionsVersion())
                .build();
        builder.termsAndConditionsAgreement(tac);
        
        PrivacyPolicyAgreement ppa = PrivacyPolicyAgreement.builder()
                .acceptTime(sg.getPrivacyPolicyAcceptTime())
                .version(sg.getPrivacyPolicyVersion())
                .build();
        builder.privacyPolicyAgreement(ppa);
        
        return builder.build();
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
            builder.firstName(personalInfo.getFirstName())
                    .lastName(personalInfo.getLastName())
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
        
        // we are mapping loyalty information here for verification purposes only.
        // see this.mapGuestToSaviyntGuest() and Impl's verifyLoyaltyInformation() Java Doc for more info.
        LoyaltyInformation loyaltyInfo = guest.getLoyaltyInformation();
        if (loyaltyInfo != null) {
            builder.crownAndAnchorId(loyaltyInfo.getCrownAndAnchorId())
                    .captainsClubId(loyaltyInfo.getCaptainsClubId())
                    .azamaraLoyaltyId(loyaltyInfo.getAzamaraLoyaltyId())
                    .clubRoyaleId(loyaltyInfo.getClubRoyaleId())
                    .celebrityBlueChipId(loyaltyInfo.getCelebrityBlueChipId());
        }
        
        WebshopperInformation webshopperInfo = guest.getWebshopperInformation();
        if (webshopperInfo != null) {
            builder.webshopperId(webshopperInfo.getShopperId())
                    .webshopperBrand(webshopperInfo.getBrand());
        }
        
        TermsAndConditionsAgreement tac = guest.getTermsAndConditionsAgreement();
        if (tac != null) {
            builder.termsAndConditionsAgreement(tac);
        }
        
        PrivacyPolicyAgreement ppa = guest.getPrivacyPolicyAgreement();
        if (ppa != null) {
            builder.privacyPolicyAgreement(ppa);
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
     * in {@link EmailOptins} for Update Optins service.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link EmailOptins}
     */
    public static EmailOptins mapEnrichedGuestToEmailOptins(EnrichedGuest guest) {
        List<EmailOptin> emailOptins = guest.getEmailOptins();
        
        if (emailOptins != null && !emailOptins.isEmpty()) {
            return EmailOptins.builder()
                    .optins(emailOptins)
                    .email(guest.getEmail())
                    .header(guest.getHeader())
                    .build();
        }
        
        return null;
    }
    
    /**
     * Extracts all necessary information from {@link EnrichedGuest} and maps those attributes
     * in {@link PostalOptins} for Update Optins service.
     *
     * @param guest {@link EnrichedGuest}
     * @return {@link PostalOptins}
     */
    public static PostalOptins mapEnrichedGuestToPostalOptins(EnrichedGuest guest) {
        List<PostalOptin> postalOptins = guest.getPostalOptins();
        
        if (postalOptins != null && !postalOptins.isEmpty()) {
            return PostalOptins.builder()
                    .optins(postalOptins)
                    .vdsId(guest.getVdsId())
                    .header(guest.getHeader())
                    .build();
        }
        
        return null;
    }
    
    /**
     * Maps the individual model values into the {@link EnrichedGuest} model.
     *
     * @param guest                    the {@link Guest} model from accounts service.
     * @param profileResponseBody      the {@link ResponseBody}<{@link Profile}> model from profiles service.
     * @param emailOptinsResponseBody  the {@link ResponseBody}<{@link EmailOptins}> model from optins service.
     * @param postalOptinsResponseBody the {@link ResponseBody}<{@link PostalOptins}> model from optins service.
     * @return {@link EnrichedGuest}
     */
    public static EnrichedGuest mapToEnrichedGuest(Guest guest,
                                                   ResponseBody<Profile> profileResponseBody,
                                                   ResponseBody<EmailOptins> emailOptinsResponseBody,
                                                   ResponseBody<PostalOptins> postalOptinsResponseBody) {
        
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
                    .phoneCountryCode(StringUtils.defaultIfBlank(guest.getPhoneCountryCode(), ""));
            
            travelDocumentInformation.passportNumber(guest.getPassportNumber())
                    .passportExpirationDate(guest.getPassportExpirationDate());
            
            loyaltyInformationBuilder.crownAndAnchorId(guest.getCrownAndAnchorId())
                    .captainsClubId(guest.getCaptainsClubId())
                    .azamaraLoyaltyId(guest.getAzamaraLoyaltyId())
                    .clubRoyaleId(guest.getClubRoyaleId())
                    .celebrityBlueChipId(guest.getCelebrityBlueChipId());
            
            enrichedGuestBuilder.privacyPolicyAgreement(guest.getPrivacyPolicyAgreement());
            enrichedGuestBuilder.termsAndConditionsAgreement(guest.getTermsAndConditionsAgreement());
            
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
        
        if (emailOptinsResponseBody != null && emailOptinsResponseBody.getPayload() != null) {
            enrichedGuestBuilder.emailOptins(emailOptinsResponseBody.getPayload().getOptins());
        }
        
        if (postalOptinsResponseBody != null && postalOptinsResponseBody.getPayload() != null) {
            enrichedGuestBuilder.postalOptins(postalOptinsResponseBody.getPayload().getOptins());
        }
        
        return enrichedGuestBuilder
                .personalInformation(personalInformationBuilder.build())
                .contactInformation(contactInformationBuilder.build())
                .travelDocumentInformation(travelDocumentInformation.build())
                .loyaltyInformation(loyaltyInformationBuilder.build())
                .build();
    }
}

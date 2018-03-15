package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
import com.rccl.middleware.notifications.EmailNotification;
import com.rccl.middleware.notifications.NotificationsService;
import com.rccl.middleware.saviynt.api.SaviyntService;
import com.rccl.middleware.saviynt.api.exceptions.SaviyntExceptionFactory;
import com.rccl.middleware.saviynt.api.responses.AccountInformation;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class EmailUpdatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(EmailUpdatedConfirmationEmail.class);
    
    private AemEmailService aemEmailService;
    
    private SaviyntService saviyntService;
    
    private NotificationsService notificationsService;
    
    @Inject
    public EmailUpdatedConfirmationEmail(AemEmailService aemEmailService,
                                         SaviyntService saviyntService,
                                         NotificationsService notificationsService) {
        this.aemEmailService = aemEmailService;
        this.saviyntService = saviyntService;
        this.notificationsService = notificationsService;
    }
    
    /**
     * Retrieves Email template from AEM and then publishes the email content JSON as Kafka message, one for
     * old email and another for the new email, for email notification.
     *
     * @param oldEmail      - the original email prior to email update.
     * @param eg            - the {@link EnrichedGuest} from service request invocation.
     * @param requestHeader - the {@link RequestHeader} from service request invocation.
     */
    public void send(String oldEmail, EnrichedGuest eg, RequestHeader requestHeader) {
        if (eg == null) {
            throw new IllegalArgumentException("The EnrichedGuest argument is required.");
        }
        
        if (eg.getHeader() == null) {
            throw new IllegalArgumentException("The header property in the EnrichedGuest must not be null.");
        }
        
        LOGGER.info("#send - Attempting to send the email to: " + eg.getEmail());
        
        this.getGuestInformation(eg)
                .thenAccept(accountInformation -> this.getEmailContent(eg.getHeader().getBrand(),
                        accountInformation.getGuest().getFirstName(), requestHeader)
                        .thenAccept(htmlEmailTemplate -> {
                            String content = htmlEmailTemplate.getHtmlMessage();
                            String sender = htmlEmailTemplate.getSender() == null
                                    ? EmailBrandSenderEnum.getEmailAddressFromBrand(eg.getHeader().getBrand())
                                    : htmlEmailTemplate.getSender();
                            String subject = htmlEmailTemplate.getSubject();
                            
                            // send email to both old and new emails
                            for (String email : Arrays.asList(oldEmail, eg.getEmail())) {
                                EmailNotification en = EmailNotification.builder()
                                        .recipient(email)
                                        .sender(sender)
                                        .subject(subject)
                                        .content(content)
                                        .build();
                                
                                this.sendEmailNotification(en);
                            }
                        }));
    }
    
    private CompletionStage<AccountInformation> getGuestInformation(EnrichedGuest eg) {
        if (StringUtils.isBlank(eg.getVdsId())) {
            throw new IllegalArgumentException("The vdsId in the EnrichedGuest is required.");
        }
        
        return saviyntService.getGuestAccount("systemUserName", Optional.empty(), Optional.of(eg.getVdsId()))
                .invoke()
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause();
                    
                    if (cause instanceof SaviyntExceptionFactory.ExistingGuestException
                            || cause instanceof SaviyntExceptionFactory.NoSuchGuestException) {
                        throw new GuestNotFoundException();
                    }
                    
                    throw new MiddlewareTransportException(TransportErrorCode.BadRequest, throwable);
                });
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Character brand, String firstName,
                                                               RequestHeader aemEmailRequestHeader) {
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the EnrichedGuest must not be null.");
        }
        
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), throwable);
        };
        
        String acceptLanguage = aemEmailRequestHeader.getHeader("Accept-Language").orElse("");
        Function<RequestHeader, RequestHeader> aemEmailServiceHeader = rh -> rh
                .withHeader("Accept-Language", acceptLanguage);
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityEmailUpdatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalEmailUpdatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        }
        
        throw new IllegalArgumentException("An invalid brand value was encountered: " + brand);
    }
    
    private void sendEmailNotification(EmailNotification emailNotification) {
        notificationsService
                .sendEmail()
                .invoke(emailNotification)
                .exceptionally(throwable -> {
                    LOGGER.error(throwable.getMessage());
                    throw new MiddlewareTransportException(TransportErrorCode.InternalServerError, throwable);
                });
    }
}

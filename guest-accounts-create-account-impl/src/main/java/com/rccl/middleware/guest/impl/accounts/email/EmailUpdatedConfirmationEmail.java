package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.email.EmailNotification;
import com.rccl.middleware.guest.accounts.enriched.EnrichedGuest;
import com.rccl.middleware.guest.accounts.exceptions.GuestNotFoundException;
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
    
    private PersistentEntityRegistry persistentEntityRegistry;
    
    private SaviyntService saviyntService;
    
    @Inject
    public EmailUpdatedConfirmationEmail(AemEmailService aemEmailService,
                                         PersistentEntityRegistry persistentEntityRegistry,
                                         SaviyntService saviyntService) {
        this.aemEmailService = aemEmailService;
        this.persistentEntityRegistry = persistentEntityRegistry;
        this.saviyntService = saviyntService;
    }
    
    /**
     * Retrieves Email template from AEM and then publishes the email content JSON as Kafka message, one for
     * old email and another for the new email, for email notification.
     *
     * @param oldEmail - the original email prior to email update.
     * @param eg       - the {@link EnrichedGuest} from service request invocation.
     */
    public void send(String oldEmail, EnrichedGuest eg) {
        if (eg == null) {
            throw new IllegalArgumentException("The EnrichedGuest argument is required.");
        }
        
        if (eg.getHeader() == null) {
            throw new IllegalArgumentException("The header property in the EnrichedGuest must not be null.");
        }
        
        LOGGER.info("#send - Attempting to send the email to: " + eg.getEmail());
        
        this.getGuestInformation(eg)
                .thenAccept(accountInformation -> this.getEmailContent(eg.getHeader().getBrand(),
                        accountInformation.getGuest().getFirstName())
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
                                
                                this.sendToTopic(en);
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
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Character brand, String firstName) {
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the EnrichedGuest must not be null.");
        }
        
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), throwable);
        };
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityEmailUpdatedConfirmationEmailContent(firstName)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalEmailUpdatedConfirmationEmailContent(firstName)
                    .invoke()
                    .exceptionally(exceptionally);
        }
        
        throw new IllegalArgumentException("An invalid brand value was encountered: " + brand);
    }
    
    private void sendToTopic(EmailNotification emailNotification) {
        persistentEntityRegistry
                .refFor(EmailNotificationEntity.class, emailNotification.getRecipient())
                .ask(new EmailNotificationCommand.SendEmailNotification(emailNotification));
    }
}

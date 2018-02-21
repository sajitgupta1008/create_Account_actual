package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.email.EmailNotification;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class AccountCreatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(AccountCreatedConfirmationEmail.class);
    
    private AemEmailService aemEmailService;
    
    private PersistentEntityRegistry persistentEntityRegistry;
    
    @Inject
    public AccountCreatedConfirmationEmail(AemEmailService aemEmailService,
                                           PersistentEntityRegistry persistentEntityRegistry) {
        this.aemEmailService = aemEmailService;
        this.persistentEntityRegistry = persistentEntityRegistry;
    }
    
    public void send(Guest guest, String languageCode) {
        if (guest == null) {
            throw new IllegalArgumentException("The Guest argument is required.");
        }
        
        LOGGER.info("#send - Attempting to send the email to: " + guest.getEmail());
        
        try {
            this.getEmailContent(guest, languageCode)
                    .thenAccept(htmlEmailTemplate -> {
                        if (htmlEmailTemplate != null) {
                            String content = htmlEmailTemplate.getHtmlMessage();
                            String sender = htmlEmailTemplate.getSender();
                            String subject = htmlEmailTemplate.getSubject();
                            
                            EmailNotification en = EmailNotification.builder()
                                    .recipient(guest.getEmail())
                                    .sender(sender)
                                    .subject(subject)
                                    .content(content)
                                    .build();
                            
                            this.sendToTopic(en);
                        }
                    });
        } catch (Exception e) {
            LOGGER.info("An error was encountered when retrieving email content.", e);
        }
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Guest guest, String languageCode) {
        if (guest.getHeader() == null) {
            throw new IllegalArgumentException("The header property in the Guest must not be null.");
        }
        
        Character brand = guest.getHeader().getBrand();
        
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the Guest must not be null.");
        }
        
        String firstName = guest.getFirstName();
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), throwable);
        };
        
        Function<RequestHeader, RequestHeader> acceptLanguageHeader = rh ->
                rh.withHeader("Accept-Language", languageCode);
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityAccountCreatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(acceptLanguageHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalAccountCreatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(acceptLanguageHeader)
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

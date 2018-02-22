package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.email.EmailNotification;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class PasswordUpdatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(PasswordUpdatedConfirmationEmail.class);
    
    private AemEmailService aemEmailService;
    
    private PersistentEntityRegistry persistentEntityRegistry;
    
    @Inject
    public PasswordUpdatedConfirmationEmail(AemEmailService aemEmailService,
                                            PersistentEntityRegistry persistentEntityRegistry) {
        this.aemEmailService = aemEmailService;
        this.persistentEntityRegistry = persistentEntityRegistry;
    }
    
    public void send(String email, String firstName, Header header, RequestHeader aemEmailRequestHeader) {
        LOGGER.info("#send - Attempting to send the email to: " + email);
        
        this.getEmailContent(firstName, header, aemEmailRequestHeader).thenAccept(htmlEmailTemplate -> {
            String content = htmlEmailTemplate.getHtmlMessage();
            String sender = htmlEmailTemplate.getSender();
            String subject = htmlEmailTemplate.getSubject();
            
            EmailNotification en = EmailNotification.builder()
                    .content(content)
                    .recipient(email)
                    .sender(sender)
                    .subject(subject)
                    .build();
            
            this.sendToTopic(en);
        });
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(String firstName, Header header,
                                                               RequestHeader aemEmailRequestHeader) {
        if (header == null) {
            throw new IllegalArgumentException("The header property in the EnrichedGuest must not be null.");
        }
        
        Character brand = header.getBrand();
        
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the "
                    + "EnrichedGuest must not be null.");
        }
        
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), throwable);
        };
        
        Function<RequestHeader, RequestHeader> aemEmailServiceHeader = rh -> aemEmailRequestHeader;
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityPasswordUpdatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalPasswordUpdatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
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

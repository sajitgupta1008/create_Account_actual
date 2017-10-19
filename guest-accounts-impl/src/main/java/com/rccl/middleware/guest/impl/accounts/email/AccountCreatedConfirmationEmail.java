package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
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
    
    public void send(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("The Guest argument is required.");
        }
        
        this.getEmailContent(guest)
                .thenAccept(htmlEmailTemplate -> {
                    String content = htmlEmailTemplate.getHtmlMessage();
                    String sender = "notifications@rccl.com";
                    String subject = htmlEmailTemplate.getSubject();
                    
                    EmailNotification en = EmailNotification.builder()
                            .recipient(guest.getEmail())
                            .sender(sender)
                            .subject(subject)
                            .content(content)
                            .build();
                    
                    this.sendToTopic(en);
                });
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Guest guest) {
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
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityAccountCreatedConfirmationEmailContent(firstName)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalAccountCreatedConfirmationEmailContent(firstName)
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

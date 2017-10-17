package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.rccl.middleware.aem.api.AemService;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.guest.accounts.email.EmailNotification;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class EmailUpdatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(EmailUpdatedConfirmationEmail.class);
    
    private AemService aemService;
    
    private PersistentEntityRegistry persistentEntityRegistry;
    
    @Inject
    public EmailUpdatedConfirmationEmail(AemService aemService,
                                           PersistentEntityRegistry persistentEntityRegistry) {
        this.aemService = aemService;
        this.persistentEntityRegistry = persistentEntityRegistry;
    }
    
    public void send(Guest guest) {
        this.getEmailUpdatedConfirmationEmailTemplate()
                .thenAccept(aemTemplateResponse -> {
                    String content = this.getPopulatedEmailTemplate(aemTemplateResponse);
                    String sender = "notifications@rccl.com";
                    String subject = aemTemplateResponse.get("subject").asText();
                    
                    EmailNotification en = EmailNotification.builder()
                            .recipient(guest.getEmail())
                            .sender(sender)
                            .subject(subject)
                            .content(content)
                            .build();
                    
                    this.sendToTopic(en);
                });
    }
    
    private CompletionStage<JsonNode> getEmailUpdatedConfirmationEmailTemplate() {
        // TODO: Add new call for new template.
        // TODO: aemService.getEmailUpdatedConfirmationEmailTemplate()
        return aemService.getResetPasswordEmailMigration()
                .invoke()
                .exceptionally(throwable -> {
                    LOGGER.error("#getEmailUpdatedConfirmationEmailTemplate:", throwable);
                    throw new MiddlewareTransportException(TransportErrorCode.fromHttp(500), throwable);
                });
    }
    
    private String getPopulatedEmailTemplate(JsonNode aemTemplateResponse) {
        // TODO: Add logic for populating.
        return aemTemplateResponse.get("htmlMessage").asText();
    }
    
    private void sendToTopic(EmailNotification emailNotification) {
        persistentEntityRegistry
                .refFor(EmailNotificationEntity.class, emailNotification.getRecipient())
                .ask(new EmailNotificationCommand.SendEmailNotification(emailNotification));
    }
}

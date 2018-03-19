package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.notifications.EmailNotification;

import javax.inject.Inject;

public class PasswordUpdatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(PasswordUpdatedConfirmationEmail.class);
    
    private AemEmailHelper aemEmailHelper;
    
    private NotificationsHelper notificationsHelper;
    
    @Inject
    public PasswordUpdatedConfirmationEmail(AemEmailHelper aemEmailHelper,
                                            NotificationsHelper notificationsHelper) {
        this.aemEmailHelper = aemEmailHelper;
        this.notificationsHelper = notificationsHelper;
    }
    
    public void send(String email, String firstName, Header header, RequestHeader aemEmailRequestHeader) {
        LOGGER.info("#send - Attempting to send the email to: " + email);
        
        if (header == null) {
            throw new IllegalArgumentException("The header property in the EnrichedGuest must not be null.");
        }
        
        Character brand = header.getBrand();
        
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the "
                    + "EnrichedGuest must not be null.");
        }
        aemEmailHelper.getEmailContent(brand, firstName, aemEmailRequestHeader)
                .thenAccept(htmlEmailTemplate -> {
                    EmailNotification emailNotification = notificationsHelper.createEmailNotification(htmlEmailTemplate,
                            header.getBrand(), email);
                    
                    notificationsHelper.sendEmailNotification(emailNotification);
                });
    }
}

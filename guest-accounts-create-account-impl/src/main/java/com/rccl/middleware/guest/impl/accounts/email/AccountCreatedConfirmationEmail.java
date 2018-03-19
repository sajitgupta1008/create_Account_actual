package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.notifications.EmailNotification;

import javax.inject.Inject;

public class AccountCreatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(AccountCreatedConfirmationEmail.class);
    
    private final AemEmailHelper aemEmailHelper;
    
    private final NotificationsHelper notificationsHelper;
    
    @Inject
    public AccountCreatedConfirmationEmail(AemEmailHelper aemEmailHelper,
                                           NotificationsHelper notificationsHelper) {
        
        this.notificationsHelper = notificationsHelper;
        this.aemEmailHelper = aemEmailHelper;
    }
    
    public void send(Guest guest, RequestHeader aemEmailRequestHeader) {
        if (guest == null) {
            throw new IllegalArgumentException("The Guest argument is required.");
        }
        
        if (guest.getHeader() == null) {
            throw new IllegalArgumentException("The header property in the Guest must not be null.");
        }
        
        Character brand = guest.getHeader().getBrand();
        if (brand == null) {
            throw new IllegalArgumentException("The brand header property in the Guest must not be null.");
        }
        
        LOGGER.info("#send - Attempting to send the email to: " + guest.getEmail());
        
        try {
            aemEmailHelper.getEmailContent(brand, guest.getFirstName(), aemEmailRequestHeader)
                    .thenAccept(htmlEmailTemplate -> {
                        if (htmlEmailTemplate != null) {
                            EmailNotification emailNotification = notificationsHelper.createEmailNotification(
                                    htmlEmailTemplate, brand, guest.getEmail());
                            notificationsHelper.sendEmailNotification(emailNotification);
                        }
                    });
        } catch (Exception e) {
            LOGGER.info("An error was encountered when retrieving email content.", e);
        }
    }
}

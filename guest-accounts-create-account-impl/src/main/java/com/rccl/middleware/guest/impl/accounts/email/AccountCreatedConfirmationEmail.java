package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.notifications.EmailNotification;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class AccountCreatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(AccountCreatedConfirmationEmail.class);
    
    private final AemEmailService aemEmailService;
    
    private final NotificationsHelper notificationsHelper;
    
    @Inject
    public AccountCreatedConfirmationEmail(AemEmailService aemEmailService,
                                           NotificationsHelper notificationsHelper) {
        
        this.notificationsHelper = notificationsHelper;
        this.aemEmailService = aemEmailService;
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
            this.getEmailContent(brand, guest.getFirstName(), aemEmailRequestHeader)
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
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Character brand,
                                                               String firstName,
                                                               RequestHeader aemEmailRequestHeader) {
        
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.InternalServerError, throwable);
        };
        
        String acceptLanguage = aemEmailRequestHeader.getHeader("Accept-Language").orElse("");
        Function<RequestHeader, RequestHeader> aemEmailServiceHeader = rh -> rh
                .withHeader("Accept-Language", acceptLanguage);
        
        if ('C' == brand || 'c' == brand) {
            return aemEmailService.getCelebrityAccountCreatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        } else if ('R' == brand || 'r' == brand) {
            return aemEmailService.getRoyalAccountCreatedConfirmationEmailContent(firstName)
                    .handleRequestHeader(aemEmailServiceHeader)
                    .invoke()
                    .exceptionally(exceptionally);
        }
        
        throw new IllegalArgumentException("An invalid brand value was encountered: " + brand);
    }
    
}

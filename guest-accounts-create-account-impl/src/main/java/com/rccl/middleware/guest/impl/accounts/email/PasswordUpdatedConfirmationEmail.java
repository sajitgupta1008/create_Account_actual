package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.header.Header;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.notifications.EmailNotification;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class PasswordUpdatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(PasswordUpdatedConfirmationEmail.class);
    
    private AemEmailService aemEmailService;
    
    private NotificationsHelper notificationsHelper;
    
    @Inject
    public PasswordUpdatedConfirmationEmail(AemEmailService aemEmailService,
                                            NotificationsHelper notificationsHelper) {
        this.aemEmailService = aemEmailService;
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
        this.getEmailContent(brand, firstName, aemEmailRequestHeader)
                .thenAccept(htmlEmailTemplate -> {
                    if (htmlEmailTemplate != null) {
                        EmailNotification emailNotification = notificationsHelper.createEmailNotification(
                                htmlEmailTemplate, header.getBrand(), email);
                        
                        notificationsHelper.sendEmailNotification(emailNotification);
                    }
                });
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Character brand, String firstName,
                                                               RequestHeader aemEmailRequestHeader) {
        
        Function<Throwable, ? extends HtmlEmailTemplate> exceptionally = throwable -> {
            LOGGER.error("#getEmailContent:", throwable);
            throw new MiddlewareTransportException(TransportErrorCode.InternalServerError, throwable);
        };
        
        String acceptLanguage = aemEmailRequestHeader.getHeader("Accept-Language").orElse("");
        Function<RequestHeader, RequestHeader> aemEmailServiceHeader = rh -> rh
                .withHeader("Accept-Language", acceptLanguage);
        
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
}

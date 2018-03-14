package com.rccl.middleware.guest.impl.accounts.email;

import ch.qos.logback.classic.Logger;
import com.lightbend.lagom.javadsl.api.transport.RequestHeader;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.rccl.middleware.aem.api.email.AemEmailService;
import com.rccl.middleware.aem.api.models.HtmlEmailTemplate;
import com.rccl.middleware.common.exceptions.MiddlewareTransportException;
import com.rccl.middleware.common.logging.RcclLoggerFactory;
import com.rccl.middleware.guest.accounts.Guest;
import com.rccl.middleware.notification.email.EmailNotification;
import com.rccl.middleware.notification.email.EmailNotificationService;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class AccountCreatedConfirmationEmail {
    
    private static final Logger LOGGER = RcclLoggerFactory.getLogger(AccountCreatedConfirmationEmail.class);
    
    private AemEmailService aemEmailService;
    
    private EmailNotificationService emailNotificationService;
    
    @Inject
    public AccountCreatedConfirmationEmail(AemEmailService aemEmailService,
                                           EmailNotificationService emailNotificationService) {
        this.aemEmailService = aemEmailService;
        this.emailNotificationService = emailNotificationService;
    }
    
    public void send(Guest guest, RequestHeader aemEmailRequestHeader) {
        if (guest == null) {
            throw new IllegalArgumentException("The Guest argument is required.");
        }
        
        LOGGER.info("#send - Attempting to send the email to: " + guest.getEmail());
        
        try {
            this.getEmailContent(guest, aemEmailRequestHeader)
                    .thenAccept(htmlEmailTemplate -> {
                        if (htmlEmailTemplate != null) {
                            String content = htmlEmailTemplate.getHtmlMessage();
                            String sender = htmlEmailTemplate.getSender() == null
                                    ? EmailBrandSenderEnum.getEmailAddressFromBrand(guest.getHeader().getBrand())
                                    : htmlEmailTemplate.getSender();
                            String subject = htmlEmailTemplate.getSubject();
                            
                            EmailNotification en = EmailNotification.builder()
                                    .recipient(guest.getEmail())
                                    .sender(sender)
                                    .subject(subject)
                                    .content(content)
                                    .build();
                            
                            this.sendEmailNotification(en);
                        }
                    });
        } catch (Exception e) {
            LOGGER.info("An error was encountered when retrieving email content.", e);
        }
    }
    
    private CompletionStage<HtmlEmailTemplate> getEmailContent(Guest guest, RequestHeader aemEmailRequestHeader) {
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
    
    private void sendEmailNotification(EmailNotification emailNotification) {
        emailNotificationService
                .notification()
                .invoke(emailNotification)
                .exceptionally(throwable -> {
                    LOGGER.error(throwable.getMessage());
                    throw new MiddlewareTransportException(TransportErrorCode.InternalServerError, throwable);
                });
    }
}

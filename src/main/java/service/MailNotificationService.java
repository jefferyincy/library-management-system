package service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import model.Book;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import util.FineCalculator;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Service
public class MailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(MailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromPersonal;

    public MailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${library.mail.from:}") String libraryFrom,
            @Value("${spring.mail.username:}") String springUsername,
            @Value("${library.mail.from-personal:Library}") String fromPersonal) {
        this.mailSender = mailSender;
        String from = libraryFrom != null && !libraryFrom.isBlank()
                ? libraryFrom.trim()
                : (springUsername != null ? springUsername.trim() : "");
        this.fromAddress = from;
        this.fromPersonal = fromPersonal != null ? fromPersonal.trim() : "Library";
    }

    public boolean isConfigured() {
        return mailSender != null && !fromAddress.isBlank();
    }

    /**
     * Sends a borrow confirmation. Fails softly (logs only) so issuing never breaks if mail is down.
     *
     * @return true if a message was handed off to the mail server successfully
     */
    public boolean sendBorrowConfirmation(User borrower, Book book, LocalDate dueDate) {
        if (!isConfigured()) {
            log.info("Borrow confirmation not sent: SMTP not set up. Uncomment and fill in spring.mail.* in application.properties (see comments there).");
            return false;
        }
        if (borrower == null || book == null || dueDate == null) {
            return false;
        }
        String to = borrower.getEmail();
        if (to == null || to.isBlank()) {
            log.info("Borrow confirmation not sent: user '{}' has no email (register again with email or update users.dat).", borrower.getUsername());
            return false;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US);
        String dueStr = dueDate.format(fmt);
        double finePerDay = FineCalculator.finePerDay();

        String subject = "Library: you borrowed \"" + book.getTitle() + "\"";
        String body = """
                Hi %s,

                Thanks for borrowing from our library.

                Book: %s
                Author: %s

                Please return this book on or before %s to avoid late fees (INR %.0f per day after the due date).

                Happy reading,
                Library Management System
                """.formatted(
                borrower.getName() != null ? borrower.getName() : borrower.getUsername(),
                book.getTitle(),
                book.getAuthor(),
                dueStr,
                finePerDay
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromPersonal);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Borrow confirmation sent to {}", to);
            return true;
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Could not send borrow confirmation to {}: {}", to, e.getMessage());
            return false;
        }
    }
}

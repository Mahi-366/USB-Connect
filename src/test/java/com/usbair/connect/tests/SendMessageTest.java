package com.usbair.connect.tests;

import com.microsoft.playwright.Response;
import com.usbair.connect.base.BaseTest;
import com.usbair.connect.config.Config;
import com.usbair.connect.pages.HistoryPage;
import com.usbair.connect.pages.HistoryPage.HistoryRow;
import com.usbair.connect.pages.LoginPage;
import com.usbair.connect.pages.SendMessagePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Health check: can we log in, send a WhatsApp template message with a PDF,
 * and does the newest history entry show anything other than "Failed"?
 */
public class SendMessageTest extends BaseTest {

    /** How long to wait for the message to show up in History with its final status. */
    private static final Duration STATUS_TIMEOUT = Duration.ofSeconds(60);

    @Test
    @DisplayName("Send template message with PDF and verify history status is not Failed")
    void sendMessageAndCheckHistory() throws Exception {
        String baseUrl = Config.get("base.url");
        String templateName = Config.get("template.name");

        // 1. Login
        LoginPage loginPage = new LoginPage(page);
        loginPage.open(baseUrl);
        loginPage.login(Config.get("login.email"), Config.get("login.password"));
        System.out.println("Logged in");

        // Remember the newest history ID so we can recognise our own message later
        HistoryPage historyPage = new HistoryPage(page, baseUrl);
        long previousId = historyPage.latestMessageId();
        System.out.println("Newest history ID before sending: " + previousId);

        // 2. Send Message
        SendMessagePage sendPage = new SendMessagePage(page);
        sendPage.open();
        sendPage.selectGroup(Config.get("group.name"));
        sendPage.selectContact(Config.get("contact.name"));
        sendPage.selectTemplate(templateName);
        sendPage.uploadFile(demoPdf());
        sendPage.fillTemplateParameter(Config.get("template.parameter"));

        assertTrue(sendPage.isSendButtonEnabled(), "Send Message button is disabled - form is not complete");
        Response response = sendPage.clickSend();
        System.out.println("Send request returned HTTP " + response.status());
        assertTrue(response.status() < 400, "Send failed with HTTP " + response.status());

        // 3. History -> check first row
        historyPage.open();
        HistoryRow row = historyPage.waitForNewMessage(previousId, STATUS_TIMEOUT);
        System.out.println("First history row: " + row);

        assertTrue(row.template().toLowerCase().contains(templateName.toLowerCase()),
                "First history row is not the message we sent: " + row);
        assertFalse(row.status().equalsIgnoreCase("Failed"),
                "Message status is Failed. WhatsApp error: " + row.error());
        System.out.println("PASS - status: " + row.status());
    }

    private static Path demoPdf() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(
                SendMessageTest.class.getResource("/files/demo.pdf"), "demo.pdf not found").toURI());
    }
}

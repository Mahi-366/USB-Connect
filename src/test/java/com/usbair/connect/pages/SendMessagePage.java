package com.usbair.connect.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.SelectOption;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class SendMessagePage {

    private final Page page;

    public SendMessagePage(Page page) {
        this.page = page;
    }

    public void open() {
        LoginPage.clickSidebar(page, "/messages/send");
        page.waitForURL("**/messages/send");
    }

    public void selectGroup(String groupName) {
        selectByText("#group-select", text -> text.equalsIgnoreCase(groupName));
    }

    /** Contacts load after the group is chosen. Option text looks like "MD Mahi Sarkar (8801684639746)". */
    public void selectContact(String contactName) {
        page.locator("#contact-select option").first().waitFor();
        selectByText("#contact-select", text -> text.toLowerCase().startsWith(contactName.toLowerCase()));
    }

    /** Accepts "updated_flight_schedule" or the full "updated_flight_schedule (MARKETING)". */
    public void selectTemplate(String templateName) {
        selectByText("#template-select", text -> text.equalsIgnoreCase(templateName)
                || text.toLowerCase().startsWith(templateName.toLowerCase() + " ("));
    }

    public void uploadFile(Path file) {
        Locator fileInput = page.locator("#file-input");
        fileInput.waitFor();
        fileInput.setInputFiles(file);
    }

    public void fillTemplateParameter(String value) {
        page.locator("input[name='parameters[]']").first().fill(value);
    }

    public boolean isSendButtonEnabled() {
        return page.locator("#submit-btn").isEnabled();
    }

    /** Clicks "Send Message" and returns the server's response to the form submit. */
    public Response clickSend() {
        Response response = page.waitForResponse(
                r -> r.request().method().equals("POST") && r.request().isNavigationRequest(),
                () -> page.click("#submit-btn"));
        page.waitForLoadState();
        return response;
    }

    private void selectByText(String selectCss, Predicate<String> matches) {
        List<String> options = page.locator(selectCss + " option").allTextContents();
        String label = options.stream()
                .map(String::trim)
                .filter(matches)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No matching option in " + selectCss + ". Available: " + options));
        page.selectOption(selectCss, new SelectOption().setLabel(label));
    }
}

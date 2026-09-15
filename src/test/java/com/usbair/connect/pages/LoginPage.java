package com.usbair.connect.pages;

import com.microsoft.playwright.Page;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public void open(String baseUrl) {
        page.navigate(baseUrl);
    }

    public void login(String email, String password) {
        page.fill("input[name='email']", email);
        page.fill("input[name='password']", password);
        page.click("button[type='submit']");
        page.waitForURL("**/dashboard");
    }

    /** Clicks a link in the left side bar. The page also has hidden mobile-menu copies, so only click the visible one. */
    public static void clickSidebar(Page page, String hrefEnding) {
        page.locator("a[href$='" + hrefEnding + "']:visible").first().click();
    }
}

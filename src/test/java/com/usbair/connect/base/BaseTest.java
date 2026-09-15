package com.usbair.connect.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.usbair.connect.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;

public abstract class BaseTest {

    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Config.getBoolean("headless"))
                .setSlowMo(Config.getBoolean("headless") ? 0 : 300));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900));
        page = context.newPage();
        page.setDefaultTimeout(30_000);
    }

    @AfterEach
    void closePage(TestInfo testInfo) {
        // Always keep a screenshot of the last screen: test-results/<test name>.png
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Path.of("test-results", testInfo.getTestMethod().orElseThrow().getName() + ".png"))
                .setFullPage(true));
        context.close();
    }
}

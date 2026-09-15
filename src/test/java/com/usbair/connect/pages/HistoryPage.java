package com.usbair.connect.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryPage {

    // Column order in the history table
    private static final int CONTACT_COLUMN = 1;
    private static final int TEMPLATE_COLUMN = 4;
    private static final int STATUS_COLUMN = 6;

    /** WhatsApp may first say "Sent" and change it to "Failed" a few seconds later, so wait for one of these. */
    private static final List<String> FINAL_STATUSES = List.of("delivered", "read", "received", "failed");

    /** Each row's "View Details" button holds the message data as JSON: viewDetails({"id":280, ...}) */
    private static final Pattern MESSAGE_ID = Pattern.compile("viewDetails\\(\\{(?:&quot;|\")id(?:&quot;|\"):(\\d+)");

    private static final String READ_ROW_DETAILS = """
            btn => {
              const s = btn.getAttribute('onclick');
              const data = JSON.parse(s.substring(s.indexOf('(') + 1, s.lastIndexOf(')')));
              let error = '';
              try {
                const e = JSON.parse(data.meta_response).status_update.errors[0];
                error = e.code + ' ' + e.title + ': ' + ((e.error_data && e.error_data.details) || e.message);
              } catch (ignored) {}
              return { id: data.id, error: error };
            }""";

    public record HistoryRow(long id, String contact, String template, String status, String error) {
    }

    private final Page page;
    private final String historyUrl;

    public HistoryPage(Page page, String baseUrl) {
        this.page = page;
        this.historyUrl = baseUrl.replaceAll("/+$", "") + "/messages/history";
    }

    public void open() {
        LoginPage.clickSidebar(page, "/messages/history");
        page.waitForURL("**/messages/history*");
    }

    /** ID of the newest history entry, read in the background without changing the browser page. */
    public long latestMessageId() {
        String html = page.context().request().get(historyUrl).text();
        Matcher matcher = MESSAGE_ID.matcher(html);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0;
    }

    /**
     * Reloads History until the first row is a message newer than {@code previousId}
     * and its status is final (or the timeout runs out).
     */
    public HistoryRow waitForNewMessage(long previousId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            HistoryRow row = firstRow();
            boolean isNew = row.id() > previousId;
            if (isNew && FINAL_STATUSES.contains(row.status().toLowerCase())) {
                return row;
            }
            if (System.currentTimeMillis() > deadline) {
                if (isNew) {
                    return row;
                }
                throw new AssertionError("The sent message did not appear in History within "
                        + timeout.toSeconds() + " seconds. First row: " + row);
            }
            page.waitForTimeout(5_000);
            page.reload();
        }
    }

    public HistoryRow firstRow() {
        Locator row = page.locator("table tbody tr").first();
        row.waitFor();
        Locator cells = row.locator("td");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>)
                row.locator("button[title='View Details']").evaluate(READ_ROW_DETAILS);
        return new HistoryRow(
                ((Number) details.get("id")).longValue(),
                cellText(cells.nth(CONTACT_COLUMN)),
                cellText(cells.nth(TEMPLATE_COLUMN)),
                cellText(cells.nth(STATUS_COLUMN)),
                (String) details.get("error"));
    }

    private static String cellText(Locator cell) {
        return cell.innerText().replaceAll("\\s+", " ").trim();
    }
}

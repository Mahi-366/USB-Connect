# USB-Connect – Automation Test (Playwright + Maven)

This test checks that **USBA Connect** works end to end. It:

1. Logs in to https://usbaconnect.usbair.com
2. Opens **Send Message**, picks group **Test**, then contact **Md Mahi Sarkar**
3. Picks template **updated_flight_schedule (MARKETING)**, uploads a small demo PDF, and enters **Mahi** as the template parameter
4. Clicks **Send Message**
5. Opens **History** from the side bar and reads the status of the **first row**
   - **PASS**: any status except Failed (Sent, Delivered, Read, Received…)
   - **FAIL**: the status is **Failed**, or any step above breaks

> ⚠️ Every run sends a **real** WhatsApp message.

---

## 1. Install these once on your laptop

| Tool | Where to get it | Check it works |
|---|---|---|
| **Git** | https://git-scm.com/downloads | `git --version` |
| **Java JDK 17** or newer | https://adoptium.net (Temurin 17) | `java -version` |
| **IntelliJ IDEA** (Community is fine) | https://www.jetbrains.com/idea/download | – |

You do **not** need to install Maven separately, because IntelliJ has Maven built in.
You do **not** need to install a browser either. Playwright downloads Chromium by itself the first time you run the test.

---

## 2. Get the code (clone)

**Option A: from IntelliJ (easiest)**
1. Open IntelliJ, then go to **File → New → Project from Version Control…**
   (on the welcome screen, click **Clone Repository**)
2. URL: `https://github.com/Mahi-366/USB-Connect.git`
3. Choose a folder and click **Clone**
4. If IntelliJ asks "Trust project?", click **Trust Project**
5. Wait until the progress bar at the bottom finishes. IntelliJ is downloading the Maven libraries.

**Option B: from a terminal**
```bash
git clone https://github.com/Mahi-366/USB-Connect.git
```
Then in IntelliJ: **File → Open…**, select the `USB-Connect` folder (the one with `pom.xml`), and click **Open**.

---

## 3. Set Java 17 in IntelliJ

1. **File → Project Structure… → Project**
2. **SDK**: pick a JDK 17 (or newer). If none is listed, click **Add SDK → Download JDK**, choose version **17** and vendor **Eclipse Temurin**, then click **Download**.
3. Click **OK**

---

## 4. Create your password file (important!)

Your password is **not** on GitHub, so you need to create the file once on each laptop.

1. In the IntelliJ Project panel (left side), right-click **`config.properties.example`**, then **Copy** and **Paste** it into the same folder
2. Name the copy **`config.properties`**
3. Open it and fill in:
   ```properties
   login.email=mahisarkar366@gmail.com
   login.password=YOUR_REAL_PASSWORD
   ```
4. Save (Ctrl+S)

`config.properties` is listed in `.gitignore`, so git will never push it.

---

## 5. Run the test

**Option A: IntelliJ**
1. Open `src/test/java/com/usbair/connect/tests/SendMessageTest.java`
2. Click the green ▶ next to `class SendMessageTest`, then **Run 'SendMessageTest'**
3. The first run is slower because Playwright downloads Chromium (about 150 MB)
4. With `headless=false` you can watch the browser do each step
5. Green ✔ at the bottom means **PASS**. Red ✘ means **FAIL**, and the reason is shown in the output.

**Option B: Maven panel in IntelliJ**
Open the **Maven** tab on the right side, then **usb-connect-automation → Lifecycle → test**, and double-click it.

**Option C: terminal** (needs Maven installed, or use IntelliJ's terminal)
```bash
mvn test                      # normal run
mvn test -Dheadless=true      # run without opening a browser window
```

After every run, a screenshot of the last screen is saved to `test-results/sendMessageAndCheckHistory.png`.

---

## 6. Get the latest code later

- IntelliJ: **Git → Pull…** (or press Ctrl+T), then **Pull**
- Terminal: `git pull`

Your `config.properties` stays as it is, so there's no need to create it again.

---

## Settings you can change (`config.properties`)

| Key | Meaning | Default |
|---|---|---|
| `base.url` | Website address | https://usbaconnect.usbair.com |
| `login.email` / `login.password` | Login | – |
| `group.name` | Group to pick | Test |
| `contact.name` | Contact to pick (start of the name is enough) | Md Mahi Sarkar |
| `template.name` | Template to pick | updated_flight_schedule |
| `template.parameter` | Value for template parameter 1 | Mahi |
| `headless` | `true` = no browser window | false |

Any setting can also be given on the command line (`-Dheadless=true`) or as an environment variable (`LOGIN_PASSWORD=...`).

---

## Common problems

| Problem | Fix |
|---|---|
| `Missing setting 'login.password'` | You skipped step 4: create `config.properties` |
| Red `import` lines / `cannot find symbol` | Right-click `pom.xml`, then **Maven → Reload project** |
| `release version 17 not supported` | Step 3: set the project SDK to JDK 17 |
| Browser download fails (office network or proxy) | Try another network, or run `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"` |
| `Message status is Failed. WhatsApp error: 131047 Re-engagement message…` | The website is working, but WhatsApp refused the message because the contact has not replied in the last 24 hours. From **Md Mahi Sarkar's** WhatsApp, send any message (for example "Hi") to the business number, then run the test again. |
| Test fails at "No matching option…" | The group, contact or template name changed on the website. The error lists the available names, so update `config.properties` to match. |

## Project structure

```
pom.xml                          Maven setup (Playwright + JUnit 5)
config.properties.example        Template for your local settings
src/test/java/com/usbair/connect/
  config/Config.java             Reads settings
  base/BaseTest.java             Opens and closes the browser
  pages/LoginPage.java           Login page actions
  pages/SendMessagePage.java     Send Message page actions
  pages/HistoryPage.java         History page actions
  tests/SendMessageTest.java     The test scenario
src/test/resources/files/demo.pdf  Small PDF that gets uploaded
```

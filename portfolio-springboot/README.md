# Tanvirul Hoque Faysal | Spring Boot Portfolio

Updated personal portfolio focused on corporate IT and infrastructure, with networking, cybersecurity, SDN and backend development projects.

## New: private owner inbox (Option B)

- A **secure, admin-only inbox** at `/admin/login` with Spring Security, session authentication, CSRF-protected actions, MongoDB message storage, read/unread tracking, and email-reply history.
- Existing **private contact form now saves messages** into the `private_messages` collection before attempting a Resend email alert. A temporary Resend outage no longer loses messages.
- Owner can reply through **Resend over HTTPS**, directly to the visitor email, from `/admin/inbox`. Reply text is recorded **only after** Resend accepts the request.
- Admin credentials are mandatory environment variables (no default login). See **[ADMIN_INBOX_SETUP.md](ADMIN_INBOX_SETUP.md)** for all setup, security, and sender-domain requirements.
- The public portfolio was redesigned using a clean white / ice-blue visual system, CSS-based floating project panels, subtle motion, and more concise sections. Private inbox, public comments, and the Corporate IT CV are retained.

## Features

- Responsive website with **verified student/project information** and original profile photo.
- One direct PDF download for the **Corporate IT & Infrastructure internship CV**, under `src/main/resources/static/cv/`. All CV links point to this file.
- Public guestbook: `POST /api/comments` saves validated comments in MongoDB; `GET /api/comments` returns the latest 12.
- **Caffeine in-process cache**: caches latest comments for 2 minutes and automatically clears it when a new comment is saved. This is free: no Redis service needed.
- On successful comment saves, attempts an **email notification to the portfolio owner** through Resend's HTTPS API. HTTP is preferable to SMTP on free platforms that block SMTP ports.
- The contact form **persists private messages in MongoDB**, then attempts a Resend notification. It reports success only after MongoDB confirms the save. A failed email alert does not discard the message.
- Basic anti-spam: honeypot field, input validation, and one comment/contact submission per visitor IP per 60 seconds, per application instance.
- Escaped comment rendering (`textContent`), no visitor email collection in the public guestbook, environment-only secrets.
- Optional Docker/Render deployment files.

## Requirements

- Java 21 and Maven 3.9+ (or a compatible IDE with Maven support).
- MongoDB **local** for development OR an **Atlas Free** MongoDB cluster.
- A Resend account and API key to activate email notifications and replies to visitors. You must verify your sending domain to email arbitrary visitors.
- Private admin username and strong password (or BCrypt hash), configured through environment variables.
- See `ADMIN_INBOX_SETUP.md` for required settings and privacy safeguards.

## Quick start (local)

1. Create a MongoDB database locally, or get a free cluster and connection URL from [MongoDB Atlas](https://www.mongodb.com/atlas/database).
2. Set environment variables in your terminal or IDE. Example using Bash:

   ```bash
   export MONGODB_URI='mongodb://localhost:27017/portfolio?serverSelectionTimeoutMS=3000'
   export RESEND_API_KEY='re_YOUR_KEY_HERE'
   export EMAIL_TO='thfaysal711@gmail.com'
   export EMAIL_FROM='onboarding@resend.dev'
   ```

   **Never commit real API keys or database passwords.** `.env.example` is only a reference; Spring Boot does not load `.env` automatically. Environment variables must be exported or configured in the host.
3. Run:

   ```bash
   mvn clean test
   mvn spring-boot:run
   ```

4. Open `http://localhost:8080`. The top **Download CV** button and the CV link inside the contact panel download the same Corporate IT PDF.
5. Submit a comment, inspect `portfolio.comments` in MongoDB, and check the owner email inbox.

The landing page and CV PDF still work **without** MongoDB. However public comments **and private contact-message storage** require a reachable MongoDB URL. If no `RESEND_API_KEY` is set, comments still save to MongoDB, but **no email is sent**. Private messages still save in MongoDB if Resend is not configured, but no email notification or reply can be sent until you configure it.

## Free email notifications: Resend over HTTPS

1. Register with [Resend](https://resend.com/), ideally using **the same email address** you set as `EMAIL_TO`.
2. Create an API key with **Sending access** and set `RESEND_API_KEY` as a host environment variable.
3. For testing, `EMAIL_FROM=onboarding@resend.dev` can send to **your Resend account's own email address**. When using this shared testing sender, sending to other recipients requires a verified domain. Make `EMAIL_TO` an unformatted email address (e.g., `user@example.com`, not `Name <user@example.com>`).
4. If you own and verify a domain in Resend, set `EMAIL_FROM` to an address on it instead.
5. Test with a visitor comment and contact message, then check your Resend dashboard for email API responses.

**Why not Gmail SMTP?** Render Free web services block outbound traffic on SMTP ports 25, 465, and 587. The Resend HTTPS API uses standard TLS/port 443 and can work on that host. Resend has a free sending allowance, subject to its current terms; do not assume it is unlimited.

> Alerts are **attempted after a successful comment write**. If the email provider is unavailable, the saved comment remains visible; the app logs a warning. The app does not have a persistent notification retry queue. For guaranteed notification delivery, add a queue/retry worker in a later version.

## MongoDB Atlas Free setup

1. Create a new free cluster at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register).
2. Create a database user **for this app** with a strong password and minimum necessary database permissions; don't use your Atlas login password.
3. Allow network access from your hosting provider, then copy the SRV URI (`mongodb+srv://.../portfolio?...`). If your host uses variable egress IPs and you must temporarily open Atlas access broadly, use a restricted database user, a strong password, and TLS; review the allowed IP addresses later.
4. Set `MONGODB_URI` as an environment variable (not in source code).
5. After your first valid comment, Atlas will show a collection named `comments` in database `portfolio`.

Atlas Free storage and request limits apply. In-memory caching reduces reads but resets on app restart.

## Deploy to Render with free resources (optional)

1. Push this project to **your own GitHub repository**. Do not push `.env` or secrets.
2. Create a web service on [Render](https://render.com/) using the included **Dockerfile** (or `render.yaml`). Choose the free tier if eligible.
3. Add `MONGODB_URI`, `RESEND_API_KEY`, `EMAIL_TO`, and `EMAIL_FROM` under Environment. Do not paste credentials in the source code.
4. Deploy and test `/`, the Corporate IT PDF link, `GET /api/comments`, a posted comment, and email notification.
5. Free instances may spin down when idle, and the first request may load slowly. The app uses `PORT` from Render automatically.

**No external services or secrets have been connected or deployed by this source-code package.** Provision Atlas and Resend in your own accounts before notifications will work.

## API reference

### Read latest public comments

```http
GET /api/comments
```

Returns a JSON array of up to 12 `{ "name", "message", "createdAt" }` records, newest first. Cache expiration: 2 minutes; a new comment invalidates the cache immediately. Returns `503` if the database is unavailable.

### Post public comment

```http
POST /api/comments
Content-Type: application/json

{"name":"Visitor","message":"Nice SDN project!","website":""}
```

Success: HTTP `201`. Validation failure: `400`. Too many submissions: `429`. Database unavailable: `503`. An email notification is attempted if `RESEND_API_KEY` is configured. The `website` field is a hidden spam trap.

### Send private contact message

```http
POST /contact
Content-Type: application/x-www-form-urlencoded

name=Visitor&email=visitor%40example.com&subject=Hello&message=Your+message+here
```

Success: `200` after **MongoDB saves the message** (the Resend alert is best-effort). MongoDB unavailable: `503`. Input errors: `400`. Login is never required for the public contact form.

## Project structure

```text
src/main/java/com/thfaysal/portfolio/
  config/CacheConfig.java            Caffeine cache manager
  controller/PortfolioController.java Website and private contact form
  controller/AdminInboxController.java Admin sign-in, inbox and replies
  controller/CommentController.java   Public comment API
  controller/ApiErrorHandler.java     Safe client-facing API errors
  dto/                               Form and comment validation
  model/PrivateMessage.java         MongoDB private message and sent replies
  repository/PrivateMessageRepository.java Admin-only private message storage
  service/PrivateInboxService.java   Save, read and reply actions
  security/AdminSecurityConfig.java  Admin authentication, CSRF, login throttling
  model/PortfolioComment.java        MongoDB document
  repository/CommentRepository.java  MongoDB repository
  service/CommentService.java        Reads, writes, cache eviction
  service/NotificationService.java   Resend HTTPS alerts and private email replies
  service/SubmissionLimiter.java     Best-effort per-IP cooldown
src/main/resources/
  templates/index.html              Updated portfolio
  templates/admin/                 Secure admin login, inbox and message pages
  static/assets/css/admin.css      Matching admin-only sky-blue UI
  static/assets/                   CSS, JS and images
  static/cv/                       One downloadable Corporate IT PDF
Dockerfile                          Optional multi-stage Java build
render.yaml                         Optional Render deployment blueprint
.env.example                        Safe example environment variables
ADMIN_INBOX_SETUP.md                Admin login, database, security, email setup
```

## Security and operations notes

- Public comments are immediately visible. For a high-traffic site, add CAPTCHA, manual moderation and a persistent/distributed rate limiter before opening to broad traffic.
- Rate limiting is local to a single Java instance and is not a replacement for platform-level abuse protection.
- The latest 12 comments are public; visitor IPs are not stored in MongoDB.
- Do not publish MongoDB or Resend credentials, including in screenshots.
- The old portfolio's inaccurate credentials, project details and placeholder links were removed.
- The Corporate IT CV PDF in `static/cv` is **public**; keep only information you want visitors to download.

## Tests

The repository includes JUnit/MockMvc tests. Run `mvn test` where Maven dependencies are available. The included environment used to prepare this archive did not have internet access to download Maven/Spring dependencies, so a full Java build and live MongoDB/Resend integration test **must be run after download**. Static HTML/JS, responsive screenshots, and CV link presence were checked during packaging.

## CV configuration (Corporate IT only)

- The only included CV is `static/cv/Tanvirul_Hoque_Faysal_Corporate_IT_CV.pdf`.
- All navigation, hero, section and contact CV links download that same PDF.
- Networking and Open RAN CV files and selectors have been removed, while comments, email notification, caching and site layout remain unchanged.


## IntelliJ IDEA Java 5 target error - quick fix

This project requires JDK 21. Its `pom.xml` explicitly sets the Maven compiler
release to Java 21; there is no Java 5 code or Java 5 build target.

If IntelliJ reports `Cannot compile module 'portfolio' configured for JVM target 5`:

1. Open **pom.xml** as a **Maven project** (right-click it and choose
   **Add as Maven Project**, if needed).
2. In **File > Project Structure > Project**, choose **JDK 21** for Project SDK
   and **21** (or SDK default) for language level.
3. In **Project Structure > Modules > portfolio > Sources**, use language level
   **21** (or Project default).
4. In **Settings > Build, Execution, Deployment > Compiler > Java Compiler**,
   change the portfolio module's target bytecode version from **1.5** to **21**.
5. In **Settings > Build Tools > Maven > Runner**, choose **JDK 21**.
   Then click **Reload All Maven Projects** in the Maven tool window.
6. Rebuild the project. If it still uses an old module level, close the
   project and reopen it by selecting **pom.xml** instead of importing as a
   plain Java project.

You do not need a JDK 8 install to compile this Spring Boot 3 application.
Spring Boot 3 requires Java 17 or later; this project uses Java 21.

## Visual theme and animation

The portfolio uses a **white → ice-blue** atmospheric gradient and dark-navy text with the accent `#3498E9`. White floating cards, diagonal illustrative project windows, and restrained blue borders keep it close to the supplied case-study reference without copying the Carex brand. The admin interface uses the same light-blue identity. See `DESIGN_SYSTEM.md` for exact design tokens.

Animations are limited to ambient showcase-card drift, gentle desktop pointer depth, one-time scroll reveal, and hover elevations. The heavy canvas network, typewriter, cursor aura, and mouse tilt were removed to reduce noise and CPU usage. The guestbook remains accessible but is hidden under a compact disclosure until opened. Reduced motion is supported. No animation library is required.

The rest of the portfolio (Corporate IT CV, projects, Spring Boot controllers, MongoDB comments, caching, Resend integration) is unchanged by this visual update.

## Certificates and TryHackMe practice (October 2026 update)

The public **Credentials & Practice** section now includes expandable image previews for:

- **mile2 - C)PTE: Certified Penetration Testing Engineer**, dated 06/10/2026 (as printed on the certificate).
- **EDGE-CSE CUET DST Project - Applied Penetration Testing Engineer: Hands-on**, an 87-hour course conducted April 7 - May 18, 2026. This is a training completion certificate, not a second mile2 certification.
- **WINGS Institute - Applied AI Deep Learning with Python for Data Analysis**, an online non-credit training certificate dated August 10, 2026.

The site also lists **TryHackMe Blue** and **TryHackMe Vulnversity** as completed practice rooms, not additional certificates.

Original certificate images and optimized WebP thumbnails are stored in `src/main/resources/static/assets/images/certificates/`. Clicking a thumbnail opens a keyboard-accessible native certificate viewer; the image link still opens the original if JavaScript is disabled. The visual treatment matches the existing Carex-inspired light-blue layout and respects reduced-motion preferences.

No API routes, email settings, MongoDB collections, authentication, or resume download links were changed for this update.

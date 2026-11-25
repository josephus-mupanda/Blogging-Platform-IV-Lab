# Review notes: Blogging Platform IV

A revision guide for the lab review. For every requirement it says **what it means**, **where it is in the code** and **how to show it**. Read sections 2, 4 and 6 twice.

Paths are inside `src/main/java/com/example/Blogging_Platform_IV/`.

---

## 1. Before the review (5 minutes)

1. PostgreSQL (port 5433) and MongoDB (port 27017) must be running.
2. Create the database once (new name, the users table changed):
   ```powershell
   $env:Path += ";C:\Program Files\PostgreSQL\18\bin"
   createdb -h localhost -p 5433 -U postgres blogging_platform_iv
   ```
3. Set the Google values, then start the app (Hibernate creates the tables, the admin is created):
   ```powershell
   $env:GOOGLE_CLIENT_ID = "<client id>"
   $env:GOOGLE_CLIENT_SECRET = "<client secret>"
   .\mvnw.cmd spring-boot:run
   ```
4. Open: Swagger <http://localhost:8080/swagger-ui/index.html>, Postman (import `docs/postman_collection.json`), and keep `logs/application.log` open.
5. Run the **whole Postman collection once** before the review (Collection Runner). Everything should be green. Do not run it again during the review if you want a clean security report.
6. Admin login: `admin` / `Admin@12345`.

---

## 2. The project in one minute (what to say first)

> "Platform IV adds security to Platform III. A user can **register and log in with a username and password**, or **log in with Google**, and gets a **JWT**. The token is sent in the `Authorization` header and checked by **my own filter** on every request. There are three roles, **ADMIN, AUTHOR and READER**, and each endpoint is protected with **`@PreAuthorize`**. Passwords are hashed with **BCrypt**. I configured **CORS** for the browser origins and I **disabled CSRF for the API** because it is stateless, and I show how **CSRF works on a form**. For data structures, I use a **hash map as a blacklist** for logged-out tokens (the key is the **SHA-256** hash of the token) and hash maps to **count security events and detect brute force**. Everything is **logged**, and an admin can read a **security report**. I tested all of it with a **Postman collection**."

---

## 3. Requirement checklist

### Epic 1: Security configuration and access policies

| Requirement | Where | How to show |
|---|---|---|
| ✅ `SecurityFilterChain` with custom rules | `config/SecurityConfig` (3 chains) | Open the file. The `api` chain: public `/auth/login` and `/auth/register`, everything else `authenticated()`. |
| ✅ Public and restricted endpoints (admin, author, reader) | `SecurityConfig` + `@PreAuthorize` in `controller/` | Postman folder 7 "Role checks". |
| ✅ BCrypt for passwords | `SecurityConfig.passwordEncoder`, `service/UserService.create` | `select username, password from users;` in psql: the hash starts with `$2a$10$`. |
| ✅ Global CORS: origins, methods, headers | `config/CorsConfig` | Postman folder 9: allowed origin gives 200, `http://evil.example` gives 403 "Invalid CORS request". |
| ✅ Unauthorized origins blocked | same | same |

### Epic 2: JWT authentication

| Requirement | Where | How to show |
|---|---|---|
| ✅ `/auth/login` returns a signed JWT with username, roles, expiry | `controller/rest/AuthController`, `service/AuthService.login`, `service/JwtService.generate` | Postman "Login as ADMIN". Paste the token on jwt.io: `sub`, `roles`, `iat`, `exp`. |
| ✅ Token validated on every protected request | `filter/JwtAuthenticationFilter`, `JwtService.parse` | Call `GET /api/posts` with and without the token. |
| ✅ Expired or tampered token gives 401 | `JwtAuthenticationFilter` (catches `ExpiredJwtException` and `JwtException`), `handler/JsonSecurityHandlers` | Postman folder 8: tampered and expired token. |
| ✅ HMAC SHA-256, can be decoded in Postman | `JwtService` (`Jwts.SIG.HS256`) | The login test in Postman decodes the header (`alg: HS256`) and the claims. |

### Epic 3: CSRF and sessions

| Requirement | Where | How to show |
|---|---|---|
| ✅ CSRF disabled for the stateless API, with an explanation | `SecurityConfig.api` (`csrf(disable)`), comment above it, README section 6 | Postman: "JWT API: POST needs no CSRF token" gives 201. |
| ✅ CSRF explained for sessions and forms, with a demo | `SecurityConfig.csrfDemo`, `controller/rest/CsrfDemoController` | Browser: <http://localhost:8080/csrf-demo/form>. First form works, second form (no token) gives 403. |
| ✅ CORS vs CSRF documented in the README | README section 6 | Show the comparison table. |
| ✅ Tested with Postman and a browser | Postman folder 9, the form in the browser | |

### Epic 4: OAuth2 and RBAC

| Requirement | Where | How to show |
|---|---|---|
| ✅ Google login with Spring Security OAuth2 Client | `SecurityConfig.googleLogin`, `application.properties` (`spring.security.oauth2.client.registration.google`) | Browser: <http://localhost:8080/oauth2/authorization/google>. |
| ✅ User from Google saved in the database | `handler/OAuth2LoginSuccessHandler`, `UserService.findOrCreateGoogleUser` | After the login: `select username, email, role from users;` shows the new user. |
| ✅ Role assigned automatically | `findOrCreateGoogleUser` creates it with `Role.READER` | The JSON answer says `"role": "READER"`. |
| ✅ Roles ADMIN, AUTHOR, READER | `enums/Role`, column `users.role` | |
| ✅ `@PreAuthorize` on endpoints | all REST and GraphQL controllers | Open `PostController`: `@PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")` on create and update. |
| ✅ Verified with Postman | folder 7 | 12 requests: 200, 201, 403 and the GraphQL `FORBIDDEN` error. |

### Epic 5: DSA and security optimisation

| Requirement | Where | How to show |
|---|---|---|
| ✅ Hashing for passwords and token verification | BCrypt (passwords), HMAC SHA-256 (signature), SHA-256 (blacklist key) | Explain the three uses (section 4.7). |
| ✅ Map for temporary token blacklisting | `service/TokenBlacklist` (`ConcurrentHashMap`) | Postman folder 8: logout, then the same token gives 401 "revoked". |
| ✅ In-memory hash map for revoked tokens and sessions | same + `SecurityEvents` | `GET /api/security/report` shows `revokedTokensTracked`. |
| ✅ Logging of success and failure events | `service/SecurityEvents`, `logs/application.log` | Show `LOGIN_SUCCESS`, `LOGIN_FAILURE`. |
| ✅ Reports with token usage and access frequency | `GET /api/security/report` (`requestsPerUser`, `events`) | Call it as admin. |
| ✅ Logs analysed to detect brute force | `SecurityEvents.recordFailure`, `BRUTE_FORCE_SUSPECTED` | Postman: 6 wrong passwords, then the report lists `suspiciousSources`. |

### Technical requirements and deliverables

| Item | Status |
|---|---|
| Spring Boot 3.x with Security, JWT, OAuth2 Client, Validation, Cache | ✅ `pom.xml` (Cache and Validation from before) |
| Java 21 | ✅ |
| Username/password with JWT and Google | ✅ |
| `User` and `Role` | ⚠️ `User` is an entity. `Role` is an **enum** saved as text in `users.role` (section 7). |
| OpenAPI with secured and public endpoints | ✅ `config/OpenApiConfig`, padlocks in Swagger, none on login and register |
| GraphQL / Postman / frontend testing | ✅ Postman collection (93 checks), GraphiQL with the `Authorization` header |
| README and docs | ✅ README with two pictures, `docs/` |

---

## 4. The concepts in simple words

### 4.1 Authentication and authorization
- **Authentication** = who are you? (login, token). A failure is **401**.
- **Authorization** = what may you do? (the role). A failure is **403**.

### 4.2 BCrypt
A password is never saved. `BCryptPasswordEncoder` makes a hash with a random **salt**, so the same password gives a different hash each time, and it is slow on purpose, so guessing is hard. At login Spring hashes the typed password and compares it with the stored hash. A hash cannot be turned back into the password.

### 4.3 JWT
`header.payload.signature`, three Base64 parts separated by dots.
- Payload (claims): `sub` = username, `roles`, `iat` = issued at, `exp` = expiry (60 minutes), `jti` = unique id.
- Signature: HMAC SHA-256 of `header.payload` with a **secret key** only the server has.
- Anyone can **read** the payload, but if someone **changes** it (for example the role), the signature no longer matches and the token is refused. So the server trusts the token **without** asking the database.
- **Stateless**: the server stores no session. The client sends the token with every request.

### 4.4 The request flow (show the picture in the README)
1. **CORS** check (browsers only).
2. Spring chooses the **filter chain** by URL (three chains).
3. **`JwtAuthenticationFilter`** (mine) reads the `Bearer` token, checks signature and expiry, checks the blacklist, then puts the username and roles in the security context for this request.
4. **Access rules**: public URLs pass, others need a logged-in user. No user: 401.
5. **`@PreAuthorize`**: checks the role. Wrong role: 403.
6. The controller runs.

### 4.5 Why three filter chains?
- `api`: stateless JWT, CSRF off. This is almost everything.
- `csrfDemo`: needs a **session** and **CSRF on**, to demonstrate it.
- `googleLogin`: the OAuth2 login keeps a random `state` in the session for a few seconds, so it cannot be stateless.
Spring uses the first chain whose `securityMatcher` fits (`@Order`).

### 4.6 CORS vs CSRF (the question everybody asks)
- **CORS**: a **browser** rule. A page from site A may not read the answer of site B unless B allows it (`Access-Control-Allow-Origin`). It protects the **data** from being read. Postman and curl ignore it.
- **CSRF**: an **attack**. Site A makes your browser send a request to site B, and the browser adds your **cookie** by itself. It protects the **actions**. The defence is a secret token in the form that site A cannot read.
- They do not replace each other: CORS does not stop a forged form from being sent, and a CSRF token does not let another site read the API.
- **Why CSRF is off for the API**: the token is in the `Authorization` header, which a browser never adds by itself. There is no cookie to abuse.

### 4.7 Hashing and data structures (the DSA part)
| Where | What | Why |
|---|---|---|
| Passwords | BCrypt | one-way, salted, slow |
| JWT signature | HMAC SHA-256 | detects any change |
| Blacklist key | SHA-256 of the token | we never keep a usable token in memory |
| `TokenBlacklist` | `ConcurrentHashMap<hash, expiry>` | checking = one lookup, **O(1)**. Thread-safe because many requests run at once. |
| `SecurityEvents` | `ConcurrentHashMap` counters | events, requests per user, failed logins per username and IP |

**Logout**: a JWT is valid until `exp` and the server has no session to delete, so on logout the **hash of the token goes in the map**. The filter checks the map on every request. Entries have the expiry date and the expired ones are removed when someone logs out.

**Brute force**: a map counts the failed logins **in a row** for each username and each IP. At 5 a warning `BRUTE_FORCE_SUSPECTED` is logged. A successful login resets the count.

### 4.8 Google login (OAuth2), step by step
1. Browser opens `/oauth2/authorization/google`, Spring redirects to Google (with a random `state`).
2. The user signs in at Google. Google redirects to `/login/oauth2/code/google` with a **code**.
3. Spring checks the `state`, trades the code for an access token, and **fetches the profile** from Google.
4. **My handler** (`OAuth2LoginSuccessHandler`) checks the email is verified, finds the user by email or **creates it as READER**, and returns **the same JWT** as a normal login.
- The Google user gets a random password nobody knows, because the column cannot be empty.

### 4.9 `@PreAuthorize`
Needs `@EnableMethodSecurity`. `hasRole('ADMIN')` checks the authority `ROLE_ADMIN`, that is why the filter adds the prefix `ROLE_`. It works the same on REST and GraphQL controllers. If it fails, Spring throws `AccessDeniedException`, and my error handlers (`ApiErrors`, `GraphQlErrors`) turn it into 403 / `FORBIDDEN`.

---

## 5. Demo script (about 10 minutes)

1. **Picture** (README): walk through the six layers, 1 minute.
2. **Register and login** (Postman folder 0, or Swagger): register an author, log in, paste the token on jwt.io, show `sub`, `roles`, `exp`. In psql show the BCrypt hash.
3. **Roles** (folder 7): reader creates a tag: 403. Author creates a tag: 201. Author lists users: 403. Admin lists users: 200. Show `@PreAuthorize` in `PostController`.
4. **Token attacks** (folder 8): no token 401, tampered 401, expired 401. Log out, reuse the token: 401 "revoked".
5. **Brute force**: run the 6 wrong passwords request, call `GET /api/security/report` as admin, show `suspiciousSources`, then show `BRUTE_FORCE_SUSPECTED` in `logs/application.log`.
6. **CORS** (folder 9): allowed origin 200, evil origin 403.
7. **CSRF**: browser, `/csrf-demo/form`, submit both forms (200 then 403). Explain why the API has CSRF off.
8. **Google**: open `/oauth2/authorization/google`, sign in, show the JSON with the token and the new user in the database.
9. **Swagger**: padlocks on secured endpoints, none on `/auth/login` and `/auth/register`.

---

## 6. Questions the coach may ask

**Why JWT and not sessions?** The API is stateless: no session in memory, easy to scale, and it works for Postman, React and JavaFX. The token carries the username and role.

**Can someone change the role inside the token?** No. The payload is only Base64, so anyone can read it, but the signature is made with the server secret. If the role changes, the signature check fails and the answer is 401.

**What happens if the secret is stolen?** Anyone could create valid tokens. That is why the secret is long, comes from an environment variable in production (`JWT_SECRET`), and is never in the repository for prod.

**Why is the token valid after logout?** It would be, because the server has no session. So I put its hash in a blacklist map until it expires, and the filter checks the map.

**Why do you hash the token before putting it in the map?** So we never keep a token that can be used. If someone reads the memory, they only see hashes.

**Why a hash map?** Lookup in O(1) with one call, however many tokens are revoked. `ConcurrentHashMap` because many requests read it while a logout writes.

**What is the weakness of the blacklist?** It is in memory: empty after a restart, and not shared between several servers. A real system would use Redis.

**Difference between 401 and 403?** 401: not logged in (or bad token). 403: logged in, but the role is not allowed.

**Why is CSRF disabled?** CSRF abuses the cookie that the browser sends by itself. The API uses a header token, so there is nothing to abuse. For the form I left it on, and the demo shows it.

**Difference between CORS and CSRF?** See 4.6.

**Does CORS protect the API from Postman?** No. CORS is enforced by browsers. Authentication is what protects the API.

**What does `hasRole('ADMIN')` check?** The authority `ROLE_ADMIN`. The filter builds the authorities from the `roles` claim by adding `ROLE_`.

**Where is the password checked?** In `AuthService.login`: `AuthenticationManager.authenticate(...)`. Spring uses my `AccountUserDetailsService` to load the user and `BCryptPasswordEncoder` to compare.

**What if the user logs in with Google and an account with that email exists?** The existing account is used with its role. A new user becomes READER. An unverified Google email is refused.

**How does the app decide a login is a brute force?** 5 failed logins in a row for the same username (or the same IP). A good login resets the count. It is detected and logged, not blocked.

**Why is `JwtAuthenticationFilter` created with `new` and not with `@Component`?** A `@Component` filter is also registered by Spring Boot for every request, outside the security chain. With `new` it only runs inside the chain.

**Why is `Role` an enum and not an entity?** One role per user is enough here, an enum is simple and safe (`@Enumerated(EnumType.STRING)`). A `Role` table would be needed for several roles per user or roles created at runtime.

**How would you protect a new endpoint?** Add `@PreAuthorize("hasRole('ADMIN')")` above the method. Public URLs are only the ones listed in `SecurityConfig.api`.

**How did you test?** A Postman collection with 93 checks: login and token claims, the role table over REST and GraphQL, tampered, expired and revoked tokens, brute force, CORS, CSRF, and the redirect to Google.

---

## 7. Weak points (know them before the coach finds them)

1. **`Role` is an enum, not a table.** The brief says "User and Role entities". Say why (question above), and that a table is a small change if needed.
2. **No ownership check.** The role decides what you may do, but an AUTHOR can pass any `authorId` when creating a post (as in Lab 3). The lab only asks for role-based rules.
3. **A token stays valid until it expires**, even if the user is deleted or the role changes, unless the user logs out. The lifetime is 60 minutes.
4. **The blacklist and counters are in memory** (lost on restart, one server only). Entries of expired tokens are removed only when somebody logs out.
5. **Brute force is detected, not blocked.** No account lock. The failed-login map is never cleaned for usernames that never log in successfully.
6. **Plain HTTP.** In production it must be HTTPS, otherwise a Bearer token can be read.
7. **Dev secrets are in the repository** (`application-dev.properties`: JWT secret and admin password). They are for local use only. `prod` requires `JWT_SECRET` and `ADMIN_PASSWORD`.
8. **Google login can only be fully tried in a browser.** Postman only checks the redirect to Google. The token appears as JSON on the page (a real frontend would receive it by redirect).
9. **One role per user.**

---

## 8. Code map (read these files before the review, in this order)

| # | File | What to remember |
|---|---|---|
| 1 | `config/SecurityConfig` | 3 chains, BCrypt bean, `@EnableMethodSecurity` |
| 2 | `filter/JwtAuthenticationFilter` | read header, `jwt.parse`, blacklist check, `logIn` |
| 3 | `service/JwtService` | `generate` (claims, HS256) and `parse` |
| 4 | `service/AuthService` | `register`, `login`, `logout` |
| 5 | `service/TokenBlacklist` | map of SHA-256 hash to expiry |
| 6 | `service/SecurityEvents` | log lines, counters, brute force, report |
| 7 | `handler/JsonSecurityHandlers` | 401 and 403 as JSON |
| 8 | `handler/OAuth2LoginSuccessHandler` + `UserService.findOrCreateGoogleUser` | Google user to READER + JWT |
| 9 | `config/CorsConfig` | origins, methods, headers |
| 10 | `controller/rest/CsrfDemoController` | the form with the hidden `_csrf` |
| 11 | `controller/rest/PostController` | `@PreAuthorize` examples |
| 12 | `config/AdminSeeder` | first ADMIN at startup |

**Small changes the coach may ask for:**
- *Token lifetime*: `app.jwt.expiration-minutes` in `application.properties`.
- *Another allowed origin*: `app.cors.allowed-origins` (or `CORS_ALLOWED_ORIGINS`).
- *Change who can delete posts*: the `@PreAuthorize` above `delete` in `PostController`.
- *Brute-force limit*: `FAILURE_LIMIT` in `SecurityEvents`.
- *Make an endpoint public*: add its URL to `permitAll()` in the `api` chain of `SecurityConfig`.

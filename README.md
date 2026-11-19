# Blogging Platform IV

A Java 21 and Spring Boot 3.5 backend for a blogging platform. It continues Blogging Platform III (same REST and GraphQL APIs, same layers, PostgreSQL for users, posts and tags, MongoDB for comments and reviews) and secures it with **Spring Security**: JWT login, Google OAuth2 login, role-based access control, CORS, a CSRF demonstration, and security logging.

## How the requirements are met

| Requirement | Where to look |
|---|---|
| `SecurityFilterChain` with custom access rules | [SecurityConfig](src/main/java/com/example/Blogging_Platform_IV/security/SecurityConfig.java): three chains, public and protected endpoints |
| Public endpoints (login, registration) and restricted ones | `permitAll()` for `/auth/login` and `/auth/register`, everything else `authenticated()`, roles checked by `@PreAuthorize` |
| Passwords stored with `BCryptPasswordEncoder` | `SecurityConfig.passwordEncoder`, used in `UserService.create` |
| Global CORS configuration | `SecurityConfig.corsConfigurationSource`, section [CORS and CSRF](#cors-and-csrf) |
| `/auth/login` returns a signed JWT (username, roles, expiry) | [AuthController](src/main/java/com/example/Blogging_Platform_IV/controller/rest/AuthController.java), [JwtService](src/main/java/com/example/Blogging_Platform_IV/security/JwtService.java) |
| Tokens validated on every request, bad or expired ones get `401` | [JwtAuthenticationFilter](src/main/java/com/example/Blogging_Platform_IV/security/JwtAuthenticationFilter.java), [JsonSecurityHandlers](src/main/java/com/example/Blogging_Platform_IV/security/JsonSecurityHandlers.java) |
| JWT with subject, issue time, expiry, signed with HMAC SHA-256 | `JwtService.generate` (`sub`, `iat`, `exp`, plus `roles` and `jti`) |
| CSRF disabled for the JWT API, explained, and demonstrated on a form | `SecurityConfig.api` and `SecurityConfig.csrfDemo`, [CsrfDemoController](src/main/java/com/example/Blogging_Platform_IV/controller/rest/CsrfDemoController.java) |
| CORS vs CSRF documentation | section [CORS and CSRF](#cors-and-csrf) |
| Google OAuth2 login, user saved, role assigned | `SecurityConfig.googleLogin`, [OAuth2LoginSuccessHandler](src/main/java/com/example/Blogging_Platform_IV/security/OAuth2LoginSuccessHandler.java), `UserService.findOrCreateGoogleUser` |
| Roles `ADMIN`, `AUTHOR`, `READER` with `@PreAuthorize` | `entity/Role`, all controllers in `controller/rest` and `controller/graphql` |
| Hashing, token blacklist in a hash map | [TokenBlacklist](src/main/java/com/example/Blogging_Platform_IV/security/TokenBlacklist.java), section [Data structures and hashing](#data-structures-and-hashing) |
| Logging of authentication events, access reports, brute-force detection | [SecurityEvents](src/main/java/com/example/Blogging_Platform_IV/security/SecurityEvents.java), `GET /api/security/report` |
| Postman tests | [docs/postman_collection.json](docs/postman_collection.json) |
| OpenAPI with secured and public endpoints | Swagger UI, `OpenApiConfig` (bearer scheme), `@SecurityRequirements` on the public endpoints |

## How a request is secured

Spring Security uses the first filter chain whose `securityMatcher` fits the request, so each kind of client gets the rules that suit it:

| Order | Chain | Matches | Session | CSRF | Why |
|---|---|---|---|---|---|
| 1 | `csrfDemo` | `/csrf-demo/**` | yes (cookie) | **on** | A form served to a browser works with a session cookie, which is what CSRF attacks abuse |
| 2 | `googleLogin` | `/oauth2/**`, `/login/oauth2/**` | short-lived | on (GET only) | The OAuth2 login keeps a random `state` in the session between the redirect to Google and the callback |
| 3 | `api` | everything else (REST, GraphQL) | **none** | **off** | Stateless: the JWT is sent on every request |

For the API chain a request goes through these steps:

```
request ─► CORS check ─► JwtAuthenticationFilter ─► authorization rules ─► @PreAuthorize ─► controller
              │                    │                       │                     │
        403 bad origin     401 bad/expired/revoked    401 no token          403 wrong role
                              token (logged)        (logged, JSON body)    (logged, JSON body)
```

`JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, verifies the signature and the expiry, checks the blacklist, and logs the user in **for this one request only**. Errors use the same JSON shape as the rest of the API:

```json
{ "status": "error", "message": "The token has expired. Log in again.", "data": null }
```

## Roles and access

There are three roles (`entity/Role`). Each user has one role, stored in the `users` table and copied into the JWT. `@PreAuthorize("hasRole('ADMIN')")` above a controller method checks it before the method runs. The same rules apply to REST and GraphQL.

| Endpoint | ADMIN | AUTHOR | READER | No token |
|---|:-:|:-:|:-:|:-:|
| `POST /auth/register`, `POST /auth/login`, Swagger UI | yes | yes | yes | **yes** |
| `GET /api/posts`, `/api/posts/{id}`, by author, by tag | yes | yes | yes | 401 |
| `GET /api/tags`, `GET /api/users/{id}` | yes | yes | yes | 401 |
| Comments and reviews: read, add, edit, delete | yes | yes | yes | 401 |
| `POST` and `PUT` on posts and tags | yes | yes | 403 | 401 |
| `DELETE` on posts and tags | yes | 403 | 403 | 401 |
| `GET /api/users`, `POST/PUT/DELETE /api/users` | yes | 403 | 403 | 401 |
| `GET /api/dashboard`, `GET /api/security/report` | yes | 403 | 403 | 401 |
| `POST /auth/logout` | yes | yes | yes | 401 |

- **401 Unauthorized:** nobody is logged in (no token, tampered, expired or revoked token).
- **403 Forbidden:** logged in, but the role is not allowed. In GraphQL the error has `extensions.code = "FORBIDDEN"`.

Registration (`/auth/register`) can create `READER` and `AUTHOR` accounts only (`READER` when no role is sent) and refuses `ADMIN` with 400. The first `ADMIN` is created at startup by [AdminSeeder](src/main/java/com/example/Blogging_Platform_IV/security/AdminSeeder.java) from the `app.admin.*` properties (dev: `admin` / `Admin@12345`). An admin can then create other admins with `POST /api/users`.

## Authentication with JWT

```powershell
# 1. Register (public)
curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" `
  -d '{\"username\":\"jane\",\"email\":\"jane@example.com\",\"displayName\":\"Jane\",\"password\":\"Jane@12345\",\"role\":\"AUTHOR\"}'

# 2. Log in (public): the answer contains the token
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" `
  -d '{\"username\":\"jane\",\"password\":\"Jane@12345\"}'

# 3. Call a protected endpoint with the token
curl http://localhost:8080/api/posts -H "Authorization: Bearer <token>"
```

A JWT has three parts, `header.payload.signature`. Paste a token on [jwt.io](https://jwt.io) or decode it in Postman to read it:

```json
// header
{ "alg": "HS256" }
// payload
{ "jti": "dc278bd9-8d0c-48fe-a909-c33f0c8e356a", "sub": "jane", "roles": ["AUTHOR"], "iat": 1795084325, "exp": 1795087925 }
```

| Claim | Meaning |
|---|---|
| `sub` | the username |
| `roles` | the role of the user |
| `iat` | when the token was issued |
| `exp` | when it stops being valid (60 minutes later, `app.jwt.expiration-minutes`) |
| `jti` | a unique id of the token |

The signature is the **HMAC SHA-256** of `header.payload` made with the secret `app.jwt.secret`. Anyone can read the payload (it is only Base64 encoded), but changing one character makes the signature check fail. The secret must be at least 32 characters, and it comes from `JWT_SECRET` in the `prod` profile.

| Case | Answer |
|---|---|
| Valid token | the request runs |
| No token | 401 "Log in first: send a Bearer token." |
| Payload or signature changed | 401 "The token is not valid." |
| `exp` in the past | 401 "The token has expired. Log in again." |
| Token revoked by `POST /auth/logout` | 401 "The token was revoked by a logout. Log in again." |
| Wrong password or unknown user at login | 401 "Invalid username or password." (the same message for both) |

## Google login (OAuth2)

1. In the [Google Cloud console](https://console.cloud.google.com/apis/credentials) create an **OAuth client ID** of type *Web application*. Add this **authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`.
2. Start the app with the client's values:

```powershell
$env:GOOGLE_CLIENT_ID = "<client id>"
$env:GOOGLE_CLIENT_SECRET = "<client secret>"
.\mvnw.cmd spring-boot:run
```

3. Open `http://localhost:8080/oauth2/authorization/google` in a browser and sign in with Google.

What happens (all of it done by Spring Security's OAuth2 Client, except the last two steps):

1. The browser is redirected to Google with a random `state`.
2. After sign-in, Google sends the browser back to `/login/oauth2/code/google` with a code.
3. Spring Security checks the `state`, trades the code for an access token, and **fetches the profile from Google** (email, name, `email_verified`).
4. [OAuth2LoginSuccessHandler](src/main/java/com/example/Blogging_Platform_IV/security/OAuth2LoginSuccessHandler.java) refuses an unverified email, then `UserService.findOrCreateGoogleUser` looks the user up by email. **First time:** the user is saved in PostgreSQL with the role `READER` (assigned automatically), the Google name as display name, a username made from the start of the email, and a random BCrypt-hashed password nobody knows. **Next times:** the same account is used, with the role it has.
5. The answer is the same JWT as `/auth/login`, so the rest of the API works the same way for both kinds of login.

Without `GOOGLE_CLIENT_ID` the application still starts (the properties have placeholder values) and only the Google login fails.

## CORS and CSRF

They are often confused, but they protect against different things.

| | **CORS** | **CSRF** |
|---|---|---|
| Full name | Cross-Origin Resource Sharing | Cross-Site Request Forgery |
| What it is | A browser rule that **blocks** a web page from reading answers of another origin, unless that server allows it | An **attack** where a malicious site makes the victim's browser send a request to a site where the victim is logged in |
| Who enforces it | The browser, using headers sent by the server (`Access-Control-Allow-Origin`...) | The server, by asking for a secret token the attacker cannot know |
| Protects | The user's data from being read by other sites | The user's actions from being triggered by other sites |
| Needed when | A browser app (React, JavaFX WebView) on another origin calls the API | The user is identified by something the browser sends **by itself**, such as a session cookie |
| Does Postman or curl care? | No, they send no `Origin` header and have no same-origin rule | No, they are not browsers with cookies of other sites |

**How they interact.** CORS does not stop a forged form submission: the browser sends the request anyway, and CORS only hides the answer. That is why CORS cannot replace CSRF protection when cookies are used. And CSRF protection does not let another origin read the API from JavaScript, which is what CORS controls. An API needs the right answer to both questions: *which origins may call me from a browser?* (CORS) and *can a forged request ride on the user's cookie?* (CSRF).

### CORS configuration

`SecurityConfig.corsConfigurationSource` is global (all paths):

| Setting | Value |
|---|---|
| Allowed origins | `app.cors.allowed-origins`, default `http://localhost:3000` and `http://localhost:5173` (`CORS_ALLOWED_ORIGINS` to change) |
| Allowed methods | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Allowed headers | `Authorization`, `Content-Type`, `Accept` |
| Credentials | off (the token is in a header, no cookie is needed) |

An allowed origin gets `Access-Control-Allow-Origin: <that origin>`. **Any other origin gets `403 Invalid CORS request`**, for the preflight (`OPTIONS`) and for the real request.

```powershell
# Allowed origin: 200 and the CORS headers
curl -i -X OPTIONS http://localhost:8080/api/posts -H "Origin: http://localhost:3000" `
  -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: authorization"
# Unknown origin: 403 Invalid CORS request
curl -i -X OPTIONS http://localhost:8080/api/posts -H "Origin: http://evil.example" `
  -H "Access-Control-Request-Method: GET"
```

Tested in Postman (folder *9. CORS, CSRF and Google login*, which sets the `Origin` header by hand). For a real browser, call the API from a page served on `http://localhost:3000` or `http://localhost:5173`; the same page on any other port is blocked.

### CSRF

- **API chain: CSRF is disabled.** The user is not identified by a cookie. The JWT is in the `Authorization` header, and a browser never adds that header to a request made by another site. There is nothing for a forged request to ride on.
- **If sessions or forms were used, CSRF must be enabled.** The browser would send the session cookie with a forged request, so the server must also require a token that only its own pages contain. That is Spring Security's default, and it is what the demo chain does.

**Demonstration** (`/csrf-demo`, its own chain with a session and CSRF on):

1. `GET /csrf-demo/form` creates a session and a random CSRF token stored in it. The page contains the token in the hidden field `_csrf`.
2. `POST /csrf-demo/submit` runs only if the `_csrf` field equals the token in the session.

Open `http://localhost:8080/csrf-demo/form` in a browser: the first form works, the second one (same form without the token, like a forged request) gets **403**. With curl:

```powershell
curl -c jar.txt http://localhost:8080/csrf-demo/form                        # session cookie + page with the token
curl -b jar.txt -d "_csrf=<token>" -d "message=hi" http://localhost:8080/csrf-demo/submit   # 200
curl -b jar.txt -d "message=forged" http://localhost:8080/csrf-demo/submit                  # 403, no token
```

## Data structures and hashing

| Where | Technique | Why |
|---|---|---|
| Passwords | **BCrypt** (`BCryptPasswordEncoder`) | A one-way hash with a random salt and a work factor. The same password gives a different hash each time, and guessing is slow. The database holds `$2a$10$...`, never the password. |
| JWT signature | **HMAC SHA-256** | A keyed hash of `header.payload`. Without the secret nobody can produce a valid signature, so any change to the payload is detected. |
| Revoked tokens | **SHA-256** of the token as the map key | The server never stores a usable token. A dump of the blacklist contains only hashes. |
| Revoked tokens | **`ConcurrentHashMap<String, Instant>`** (`TokenBlacklist`) | Checking a token is one hash-map lookup, O(1) on average, however many tokens are revoked, and safe when many requests read it while a logout writes. |
| Revoked tokens | entries keep the token's expiry and expired ones are removed | A token that has expired is refused anyway, so the map does not grow forever. |
| Failed logins | **`ConcurrentHashMap` of `ConcurrentLinkedDeque<Instant>`** (`SecurityEvents`) | One queue per username and per IP. Old failures are removed from the front, so the queue size is the number of failures in the last 10 minutes. At 5, a brute force is reported. |
| Access frequency | `ConcurrentHashMap<String, LongAdder>` | A counter per user for token uses, safe and fast under many threads. |

**Logout.** A JWT is valid until `exp`, and the server keeps no session, so it cannot "delete" one. `POST /auth/logout` puts the token's hash on the blacklist until its expiry, and the filter refuses it from then on.

**Limits.** The blacklist and the counters live in memory: they are empty after a restart and are not shared by several servers. A real deployment would move them to a shared store such as Redis.

## Security logging and reports

[SecurityEvents](src/main/java/com/example/Blogging_Platform_IV/security/SecurityEvents.java) writes one `key=value` line per event on the logger `security.audit`, to the console and to `logs/application.log`:

```
LOGIN_SUCCESS user=admin ip=127.0.0.1
LOGIN_FAILURE user=admin ip=127.0.0.1
BRUTE_FORCE_SUSPECTED key=user:admin failures=5 windowMinutes=10
TOKEN_REJECTED reason="The token was revoked by a logout. Log in again." ip=127.0.0.1 path=/api/posts
TOKEN_REVOKED user=bob
UNAUTHENTICATED_REQUEST ip=127.0.0.1 path=/api/users
ACCESS_DENIED user=bob path=/api/users
OAUTH_LOGIN user=jane.doe email=jane.doe@example.com
```

Successful logins are `INFO`, failures and refusals are `WARN`, and each request with a valid token (`TOKEN_USED`) is `DEBUG` because it happens all the time. Passwords and tokens are never logged.

**Analysing the logs** (PowerShell):

```powershell
Select-String "BRUTE_FORCE_SUSPECTED" logs\application.log                  # suspected brute force
Select-String "LOGIN_FAILURE" logs\application.log | Measure-Object        # number of failed logins
Select-String "LOGIN_FAILURE" logs\application.log | ForEach-Object { ($_ -split "ip=")[1] } | Group-Object | Sort-Object Count -Descending   # failures per IP
Select-String "ACCESS_DENIED|UNAUTHENTICATED_REQUEST" logs\application.log  # attempts to reach what is not allowed
```

**Report.** `GET /api/security/report` (ADMIN) answers with the counters since the start:

```json
{ "events": { "LOGIN_SUCCESS": 3, "LOGIN_FAILURE": 6, "TOKEN_USED": 5, "TOKEN_REJECTED": 1, "TOKEN_REVOKED": 1,
              "ACCESS_DENIED": 1, "UNAUTHENTICATED_REQUEST": 1, "BRUTE_FORCE_SUSPECTED": 2 },
  "requestsPerUser": { "admin": 2, "bob": 3 },
  "failedLoginsInLast10Minutes": { "ip:127.0.0.1": 6, "user:admin": 6 },
  "suspiciousSources": [ "ip:127.0.0.1", "user:admin" ],
  "bruteForceThreshold": "5 failed logins in 10 minutes",
  "revokedTokensTracked": 1 }
```

`requestsPerUser` is the token usage and access frequency. `suspiciousSources` lists the usernames and IPs with 5 or more failed logins in the last 10 minutes.

## Configuration

| Property | Environment variable | Default | Notes |
|---|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | dev profile only: a fixed value | At least 32 characters. **Required in `prod`.** |
| `app.jwt.expiration-minutes` | | `60` | |
| `app.admin.username`, `app.admin.email` | `ADMIN_USERNAME`, `ADMIN_EMAIL` | `admin`, `admin@blog.local` | The account created at startup |
| `app.admin.password` | `ADMIN_PASSWORD` | dev profile only: `Admin@12345` | **Required in `prod`.** |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma separated |
| Google client | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | placeholders | Needed only for the Google login |

The default dev secret and admin password are in a public repository, so they are for local use only.

## Known limits

- **Ownership is not checked.** The role decides what a user may do, but an AUTHOR can still pass any `authorId` when creating a post, as in Lab 3. The lab asks for role-based rules only.
- **A token is valid until it expires**, even if the user is deleted or changes role meanwhile, unless the user logs out. Short expiry and the blacklist limit the risk.
- One role per user.
- The server speaks plain HTTP. In production, put it behind HTTPS, since a Bearer token sent over HTTP can be read.

## Code structure (Lab 3 layers plus `security`)

`Controller → Service → Repository`, as in Platform II.

| Package | What it contains |
|---|---|
| `controller.rest`, `controller.graphql` | REST and GraphQL endpoints. They call the services. |
| `service` | Business rules, `@Transactional` and the cache annotations. |
| `repository` | Spring Data interfaces. Spring writes their implementation at startup. |
| `entity` | The classes stored in the databases. The API returns them directly. |
| `dto` | The request bodies, with Bean Validation (`@NotBlank`, `@Email`...). |
| `config` | OpenAPI, GraphQL and the AOP logging aspect (`ServiceMonitoring`). |
| `security` | Lab 4: filter chains, JWT, blacklist, Google login, security events, admin seeding. |

The data layer below is unchanged since Lab 3.

Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) writes the getters, setters and empty constructors of the entities.

## Entities and relationships

| Entity | Stored in | Relationships |
|---|---|---|
| `User` | table `users` | has a BCrypt `password` and a `role` (`ADMIN`, `AUTHOR`, `READER`) |
| `Post` | table `posts` | `@ManyToOne author` (column `author_id`), `@ManyToMany tags` (join table `post_tags`) |
| `Tag` | table `tags` | — |
| `Comment` | MongoDB `comments` | keeps `postId` and `authorId` |
| `Review` | MongoDB `reviews` | keeps `postId` and `reviewerId` |

With `ddl-auto=update`, Hibernate reads these classes at startup and creates the tables, the foreign keys, the unique constraints and the indexes declared with `@Index`. Nothing has to be created by hand.

## Repositories and queries

The repositories are interfaces. `JpaRepository` already gives `save`, `findById`, `findAll`, `findAll(Pageable)`, `count` and `delete`. Each extra method is one of three kinds:

| Kind | Example | How it works |
|---|---|---|
| **Derived query** | `Page<Post> findByAuthorUsername(String username, Pageable pageable)` | Spring Data reads the method name and writes the SQL: posts whose `author.username` equals the value. |
| **JPQL** | `@Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE ...")` `searchByKeyword` | Written with the entity and field names (`Post`, `p.title`), not the table names. |
| **Native SQL** | `@Query(value = "DELETE FROM post_tags WHERE tag_id = :tagId", nativeQuery = true)` | Plain PostgreSQL SQL. Needed here because `post_tags` is a join table with no entity class, so JPQL cannot reach it. `@Modifying` marks it as a DELETE. |

The other derived queries are `findByTagsName`, `existsByAuthorId`, `existsByUsername`, `existsByEmail`, and for MongoDB `findByPostId`, `existsByPostIdAndReviewerId` and `deleteByPostId`.

## Pagination and sorting

The list endpoints receive a Spring `Pageable`, filled from the URL:

```
GET /api/posts?page=0&size=10&sort=createdAt,desc
GET /api/posts?keyword=spring&page=0&size=5&sort=title,asc
```

- Without parameters, `@PageableDefault` gives the first 10 posts, newest first.
- The page size has no upper limit: `spring.data.web.pageable.max-page-size` is commented out in `application.properties`.
- The repository adds `ORDER BY`, `LIMIT` and `OFFSET` to the SQL, so only one page leaves the database.
- The response contains the posts and the page information:

```json
{ "content": [ ... ], "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 } }
```

## Transactions

| Where | What it does |
|---|---|
| `@Transactional(rollbackFor = Exception.class)` on every service method that writes | All the SQL of the method is one unit: it is committed at the end, or rolled back if an exception is thrown. By default Spring only rolls back for a `RuntimeException`; `rollbackFor = Exception.class` makes **any** exception roll back. |
| `PostService.create`: `propagation = Propagation.REQUIRED` | Joins the caller's transaction if there is one, otherwise starts a new one (this is the default). |
| `PostService.update`: `isolation = Isolation.READ_COMMITTED` | The transaction only reads data that other transactions have already committed (no "dirty reads"). |
| `@Version` in `Post` | If two people save the same post at the same moment, Hibernate refuses the second save, so one change cannot silently erase the other. The API answers **409**. |

### When a transaction fails

| What went wrong | Exception | Answer | Where |
|---|---|---|---|
| A business rule failed (for example, a tag does not exist) | `BlogException` | 404, 409 or 400 | `ApiErrors.businessError` |
| A unique value is already used (title, tag name, username, email) | `DataIntegrityViolationException` | 409, "The transaction was rolled back, nothing was saved." | `ApiErrors.duplicateValue` |
| Two people saved the same post at once (`@Version`) | `ConcurrencyFailureException` | 409, "Your change was rolled back." | `ApiErrors.concurrentChange` |
| The transaction could not start or commit (for example, the database is down) | `TransactionException` | 500, "The transaction failed and was rolled back." | `ApiErrors.transactionFailed` |
| Any other database error | `DataAccessException` | 500, "The transaction was rolled back." | `ApiErrors.databaseError` |

In every case Spring has already rolled the transaction back before the error handler answers. GraphQL answers with the same messages (`GraphQlErrors`).

**See the rollback in the console.** The `dev` profile logs Spring's transactions (`logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG`). A failed write prints:

```
Creating new transaction with name [...PostService.create]
Initiating transaction rollback
Rolling back JPA transaction on EntityManager [...]
```

**Rollback demo (Postman: "Rollback demo").** `PostService.create` first inserts the post, then adds its tags. Create a post with a tag ID that does not exist, for example `"tagIds": [1, 999999]`:

1. The INSERT of the post runs.
2. Tag 999999 is not found, so a `BlogException` is thrown.
3. Spring rolls the transaction back, and the post that was just inserted disappears.

The API answers 404, the console shows "Rolling back JPA transaction", and `GET /api/posts` shows that no post was added. Other rollbacks to try: give a post a title that is already used (409), or give a user an email that is already used (409).

**PostgreSQL first, then MongoDB.** Comments and reviews (MongoDB) are not part of the PostgreSQL transaction. So when a method writes to both, it does the PostgreSQL part first and forces it to run with `flush()` / `saveAndFlush()`. Only if that works does it touch MongoDB:

- `PostService.delete`: delete the post, `flush()`, then delete its comments and reviews.
- `UserService.delete`: delete the user, `flush()`, then delete their comments and reviews.
- `UserService.update`: `saveAndFlush` the user, then update the name copied into their comments and reviews.

If the PostgreSQL part fails, the transaction is rolled back and MongoDB is never touched. Each MongoDB save changes one document, which MongoDB saves atomically.

## Caching

`@EnableCaching` on `BloggingPlatformIVApplication` turns caching on. `spring.cache.type=simple` keeps the values in memory.

| Cache | Filled by `@Cacheable` | Replaced by `@CachePut` | Emptied by `@CacheEvict` |
|---|---|---|---|
| `posts` | `PostService.get(id)` (frequently read posts) | `PostService.update` | deleting the post; renaming or deleting a tag; **updating a user** |
| `users` | `UserService.get(id)` | `UserService.update` | deleting the user |
| `tags` | `TagService.list()` | — | creating, renaming or deleting a tag |

- `@Cacheable`: if the value is in the cache, the method is skipped.
- `@CachePut`: the method always runs, and its result replaces the cached value.
- `@CacheEvict`: removes the cached value.

**Keeping the cache consistent.** A cached post contains its author and its tags. So every change to a user or a tag must also clear the posts cache, otherwise a post would still show the old name:

| Change | What is refreshed |
|---|---|
| Update a user | `users` entry replaced (`@CachePut`), whole `posts` cache cleared (`@CacheEvict`), and the name copied into the user's comments and reviews (MongoDB) updated. All three in [UserService.update](src/main/java/com/example/Blogging_Platform_IV/service/UserService.java). |
| Rename or delete a tag | `tags` and `posts` caches cleared |
| Update a post | `posts` entry replaced |
| Delete a post | `posts` entry removed, plus its comments and reviews |
| Delete a user | `users` entry removed, plus their comments and reviews |

**To see it work:** call `GET /api/posts/1` twice. The console (dev profile) shows `PostService.get(..) success in` about 5 ms the first time and under 1 ms the second time. Start the app with `--spring.cache.type=none` to compare without the cache.

## Run locally

Install JDK 21, PostgreSQL and MongoDB. Create an empty database once, then start the app; Hibernate creates the tables and the `admin` account is created.

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
createdb -h localhost -p 5433 -U postgres blogging_platform_iv
.\mvnw.cmd spring-boot:run
```

| Profile | PostgreSQL | MongoDB | GraphiQL | Secrets |
|---|---|---|---|---|
| `dev` (default) | `localhost:5433/blogging_platform_iv` | `localhost:27017/blogging_platform_iv` | on | fixed dev values |
| `test` | `localhost:5433/blogging_platform_iv_test` | `localhost:27017/blogging_platform_iv_test` | on | `TEST_JWT_SECRET`, `TEST_ADMIN_PASSWORD` or dev values |
| `prod` | from `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD`, `DB_MONGO_URI` | | off | `JWT_SECRET` and `ADMIN_PASSWORD` required |

The users table now has `password` and `role` columns. With `ddl-auto=update` Hibernate cannot add a `NOT NULL` column to a table that already has users, so use a new database (as above) or empty the old one.

## Testing the API

- **Postman:** import [docs/postman_collection.json](docs/postman_collection.json) and run the whole collection with the Collection Runner. Every request has a test. It registers an author and a reader, logs in with three roles and runs, in order: the Lab 3 requests, the role matrix (REST and GraphQL), the token checks (no, tampered, expired and revoked tokens), brute-force detection with the security report, the CORS and CSRF checks, and the cleanup. The expired-token request signs a token with the dev secret, so run it on the `dev` profile.
- **Swagger UI:** [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html). Log in with `POST /auth/login`, copy the token, click **Authorize** and paste it. The padlock marks the secured endpoints, and `/auth/register` and `/auth/login` have none because they are public.
- **GraphiQL:** [localhost:8080/graphiql](http://localhost:8080/graphiql), with the `Authorization` header explained in [docs/examples.graphql](docs/examples.graphql).
- **Browser:** the CSRF demo at [localhost:8080/csrf-demo/form](http://localhost:8080/csrf-demo/form) and the Google login at `/oauth2/authorization/google`.

| Routes | Operations | Roles |
|---|---|---|
| `/auth/register`, `/auth/login` | create an account, log in | public |
| `/auth/logout` | revoke the current token | any logged-in user |
| `/api/users`, `/api/users/{id}` | create, list, read, update, delete users | ADMIN (read one: any role) |
| `/api/posts`, `/api/posts/{id}` | list (paginated, optional `keyword`), read, create, update, delete posts | read: any, write: ADMIN and AUTHOR, delete: ADMIN |
| `/api/posts/author/{username}`, `/api/posts/tag/{name}` | posts of one author or tag, paginated | any |
| `/api/tags`, `/api/tags/{id}` | create, list, read, rename, delete tags | read: any, write: ADMIN and AUTHOR, delete: ADMIN |
| `/api/posts/{postId}/comments`, `/reviews` | list, add, edit, delete comments, list and add reviews | any |
| `/api/dashboard` | number of users, posts, tags, comments and reviews | ADMIN |
| `/api/security/report` | security counters and suspicious sources | ADMIN |
| `/csrf-demo/form`, `/csrf-demo/submit` | the CSRF demonstration | public (CSRF token needed to POST) |

Every REST response has `status`, `message` and `data`.

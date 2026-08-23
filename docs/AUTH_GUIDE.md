# Auth Guide — JWT, OAuth2, and RBAC for Workly

Written for a first pass at all three. Skim once end-to-end before writing any Security config, then
come back to the relevant section while you implement each ROADMAP.md Phase 2 item.

## 1. The three concepts, kept separate

People blur these together constantly. They solve different problems:

- **Authentication** — "who are you?" Proving identity. Logging in with a password, or via Google.
- **Authorization** — "what are you allowed to do?" Once we know who you are, can you `POST /jobs`?
- **JWT** — a *format* for carrying "who you are" (and maybe "what you're allowed to do") between
  requests without the server having to remember you. It's not authentication or authorization itself
  — it's the token that *proves* authentication already happened, sent with every request after login.

RBAC (role-based access control) is one way to do authorization: attach a `Role` to each user
(`APPLICANT`, `COMPANY`, `ADMIN`), attach required roles to each endpoint, check one against the other.

## 2. JWT, concretely

A JWT is three base64url-encoded parts joined by dots: `header.payload.signature`.

```text
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGl5YUBleGFtcGxlLmNvbSIsInJvbGUiOiJDT01QQU5ZIiwiZXhwIjoxNzYxMTk4Nzg2fQ.4f9c...
└──────── header ────────┘└──────────────── payload ─────────────────┘└─ signature ─┘
```

- **Header**: algorithm used (e.g. `HS256`).
- **Payload (claims)**: data about the user — `sub` (subject, usually user id or email), `role`,
  `exp` (expiry, unix seconds), `iat` (issued at). **Anyone can read this without the secret** — it's
  base64, not encrypted. Never put a password or anything sensitive in the payload.
- **Signature**: `HMAC-SHA256(header + "." + payload, secret)`. This is the part that actually matters
  — it proves the token wasn't tampered with, *if and only if* the verifier holds the same secret (or,
  for RS256, the matching public key). Anyone can *decode* a JWT; only someone with the secret can
  *forge or validate* one.

**What "verifying a JWT" means in code:** recompute the signature from the header+payload using your
secret, check it matches the signature on the token, then check `exp` hasn't passed. That's it. No
database call needed — that's the "stateless" part people mean when they say JWT auth is stateless.

**Access token vs refresh token** — two JWTs, different lifetimes:
- **Access token**: short-lived (~15 min in this project). Sent on every API request. If stolen, the
  damage window is small.
- **Refresh token**: long-lived (~7 days). Sent only to `POST /auth/refresh` to get a new access token
  without making the user log in again. Store it more carefully (it's more valuable if stolen).

**Where should the frontend store the token?**
- `localStorage`: simple, but readable by any JS on the page — vulnerable if you ever have an XSS bug.
- httpOnly cookie: JS can't read it, so XSS can't steal it directly — but now you need CSRF protection,
  and it's more setup. Standard for browser-based frontends in production.
- For this project (Postman/JMeter as primary clients right now, per [DECISIONS.md ADR-003](DECISIONS.md#adr-003-jwt-storage-on-the-client)):
  `Authorization: Bearer <token>` header is simplest and matches how you'll test it. Revisit if you add
  a browser frontend later.

## 3. How JWT plugs into Spring Security

Two pieces you'll write:

1. **A filter** (`JwtAuthenticationFilter extends OncePerRequestFilter`) that runs before Spring
   Security's normal checks: read the `Authorization` header, verify the JWT, and if valid, build an
   `Authentication` object and put it in `SecurityContextHolder`. If the header is missing or invalid,
   just let the request continue unauthenticated — the `SecurityFilterChain` rules decide whether that's
   allowed for this endpoint, the filter's only job is "if there's a valid token, trust it."
2. **A `SecurityFilterChain` bean** that says which paths need what: public GETs, `hasRole(...)` on
   writes, and registers your filter to run before Spring's built-in
   `UsernamePasswordAuthenticationFilter`.

Rough shape (fill in as you build Phase 2):

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // stateless API, no cookies/forms to protect — safe here specifically because we don't use cookie-based auth
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/jobs/**", "/companies/**").permitAll()
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/jobs").hasAnyRole("COMPANY", "ADMIN")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

Then on individual controller methods, once method security is enabled
(`@EnableMethodSecurity`), you can be more precise than the filter chain allows — e.g. "only the
review's own author or an ADMIN":

```java
@PreAuthorize("hasRole('ADMIN') or #review.reviewedBy == authentication.principal.id")
```

## 4. OAuth2 login (Google/GitHub), and how it's different from "OAuth2" in general

OAuth2 was designed for *authorization* ("let app X post to my Twitter on my behalf"), not login. When
you "Sign in with Google," you're actually using **OIDC (OpenID Connect)**, a thin identity layer on
top of OAuth2 — but Spring Security's `spring-boot-starter-oauth2-client` and the term "OAuth2 login"
cover this case fine; you don't need to build OIDC yourself.

The flow, concretely:

1. User hits `GET /oauth2/authorization/google` on your backend.
2. Spring Security redirects them to Google's login/consent screen.
3. User approves. Google redirects back to your backend's callback URL
   (`/login/oauth2/code/google`) with an authorization code.
4. Spring Security exchanges that code for Google's ID token + profile info (email, name) —
   automatically, you don't write this exchange yourself.
5. **Your code runs here**: an `OAuth2UserService`/success handler where you upsert a `User` row
   (create if new, find if returning) with `authProvider = GOOGLE`, no password.
6. Issue the *same* JWT format your local login issues, so the rest of the app never needs to know or
   care whether someone logged in with a password or with Google.

What you need at each OAuth provider (Google Cloud Console / GitHub OAuth Apps):
- A registered app with a **redirect/callback URI** set to `http://localhost:8080/login/oauth2/code/google`
  (and the GitHub equivalent) for local dev.
- Client ID + secret, injected as env vars (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`) — never commit
  these.

## 5. RBAC specifics for this project

Suggested roles (adjust in ROADMAP.md Phase 2 if the domain calls for something different):

| Role | Can do |
|---|---|
| `APPLICANT` | Browse jobs/companies/reviews (public anyway), write reviews |
| `COMPANY` | Everything APPLICANT can, plus create/update/delete jobs for their own company |
| `ADMIN` | Everything, any company, any review, any job |

Two layers of enforcement, use both:
- **Coarse, at the filter chain**: `hasRole("COMPANY")` for `POST /jobs` — cheap, no DB hit, fails fast.
- **Fine, at the method with `@PreAuthorize`**: "is this the *same* company that owns the job being
  edited?" — needs to load the entity, so it's more expensive but catches "right role, wrong resource."

## 6. Testing auth without a frontend

`spring-security-test` is already a test dependency in `pom.xml`. Two useful tools:

```java
@Test
@WithMockUser(roles = "COMPANY")
void companyCanCreateJob() { ... } // skips real JWT parsing, just asserts the role check

@Test
void loginThenUseToken() {
    // real integration test: POST /auth/login, extract token from response,
    // use it as Authorization header on a POST /jobs call, assert 201
}
```

Do at least one real end-to-end test (`loginThenUseToken`-style) per role — `@WithMockUser` alone
never proves your actual `JwtAuthenticationFilter` works.

## 7. Multi-service auth (read this once you reach ROADMAP.md Phase 3)

Once `job`/`company`/`review` are split out and `auth-service` exists:
- `auth-service` is the only thing that *creates* tokens.
- Every other service gets the same `JwtAuthenticationFilter` + `SecurityFilterChain` you built in the
  monolith, pointed at the same shared secret (via Config Server) — largely copy-paste, this is the
  payoff of building it in the monolith first.
- When `job-service` calls `company-service` via Feign, forward the caller's original token with a
  `RequestInterceptor`:

```java
@Bean
RequestInterceptor forwardAuthHeader() {
    return template -> {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null) template.header("Authorization", auth);
        }
    };
}
```

That's what makes "every service validates independently" ([DECISIONS.md ADR-001](DECISIONS.md#adr-001-auth-service-topology--dedicated-auth-service-validated-at-every-hop)) actually work end to end.

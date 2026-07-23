<!-- AUTO-SYNCED from agents KB: concepts/AUTH.md @ e5e5376.
     Do NOT edit here — edit the source in ~/projects/agents and re-run scripts/sync-conventions.sh. -->

# Authentication & Authorization

The standard across projects is **stateless, self-issued JWT** on a Spring Security
backend, consumed by a React frontend. The backend is its own identity provider — it
mints and validates its own tokens (no external IdP in the common case).

**Reference implementation:** the Cosy Domain Provider backend
(`security/jwtfilter/`, `configuration/security/`) + its frontend. It has the fullest
version (access/refresh tokens, TOTP 2FA, OAuth social login); simpler projects use a
subset. See also the Security section of [`../technologies/SPRING_BOOT.md`](../technologies/SPRING_BOOT.md).

## Backend model (Spring Security + jjwt)

- **Stateless.** `SessionCreationPolicy.STATELESS`, CSRF disabled (documented reason:
  stateless token API). No server-side session.
- **A `JwtFilter extends OncePerRequestFilter`**, registered
  `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`. It:
  1. resolves `Authorization: Bearer <token>`,
  2. validates the signature and that it is an **identity** token,
  3. loads the `UserEntity` by the token subject (user UUID),
  4. sets a custom `AuthenticationToken(subject, user)` on the `SecurityContextHolder`.
- **Route rules in one `SecurityFilterChain` bean.** Public prefixes (`/api/v1/auth/**`,
  `/actuator/**`, api-docs, webhooks) are `permitAll()`; everything else
  `.authenticated()`. Admin/staging paths are gated by their own filters.
- **Signing:** HMAC (symmetric) via the `jjwt` library. The secret comes from
  `@ConfigurationProperties(prefix = "jwt")` (`JwtProperties`) — injected in prod via
  env (`JWT_SECRET_KEY`), never the committed default (see the Robert Space Tracker
  note for why the default in `application.properties` is only a fallback).

### Token types

Tokens carry a `tokenType` claim; the filter only accepts the identity token for
authentication. Expirations are separate config values (`jwt.*-expiration-time`).

| Token | Purpose | Contents |
|-------|---------|----------|
| **Identity (access)** | short-lived, authenticates each request | rich user claims: `userId`, `username`, `email`, `isVerified`, `isMfaEnabled`, `tier`, … |
| **Refresh** | long-lived, mints new identity tokens | minimal (`userId`, `username`) |
| **MFA challenge** | interim token between password step and 2FA code | `userId` only |

The **identity token is deliberately claim-rich** so the frontend can read auth state
(verified? MFA on? plan tier?) without an extra round-trip.

### Delivery: header vs cookie (`TokenMode`)

- The identity token is sent by the SPA as `Authorization: Bearer …`.
- The refresh token is issued as an **httpOnly cookie** (`RefreshCookieFactory`,
  `auth.cookie.secure` toggled per environment). A `TokenMode` of `COOKIE` (browser)
  vs `DIRECT` (token in body) lets non-browser clients opt out of cookies.

### Extras in the reference project

- **2FA:** TOTP (`MfaService`) — when MFA is enabled, the password step returns an MFA
  challenge token, exchanged for identity+refresh after a valid 6-digit code.
- **Social login:** OAuth2 (Google/GitHub/Discord) links an external identity to a
  local user, then issues the **same** JWT pair — OAuth is only the login step, the
  app's own JWT is still the session.
- **Abuse controls:** a Bucket4j `RateLimitFilter` and a Cloudflare Turnstile
  `CaptchaService` on auth endpoints.

## Authorization

- Coarse-grained: route rules in the filter chain + `@PreAuthorize` on service methods.
- Fine-grained (Strata): a dedicated `security/authorization/` package with policy
  classes + an `AuthorizationAspect` / `@NeedsValidation` annotations enforcing
  namespace- and resource-level grants. Reach for this when access depends on the
  specific resource, not just a role.

## Frontend side (React)

- The Orval `customInstance` (`src/api/axios-instance.ts`) attaches the bearer token
  and handles a **silent refresh on 401** using the refresh cookie.
- Route access control lives in TanStack Router `beforeLoad` guards
  (e.g. `requireFullAuth()`), **never** inside page components — see
  [`../technologies/REACT.md`](../technologies/REACT.md) (Routing).
- A `useAuthInformation`-style hook decodes the identity token claims for UI state.

## DO / DON'T

**DO:**
- Keep the API stateless; validate the JWT in a dedicated filter, not in business logic.
- Split access vs refresh tokens; keep the refresh token in an httpOnly cookie.
- Source the signing secret from env/`@ConfigurationProperties`; fail closed.
- Put auth endpoints behind rate limiting (+ captcha on public sign-up/login).

**DON'T:**
- Hardcode the JWT secret, roles, or issuers in source (a committed default is a smell,
  even when overridden in prod).
- Do access control in controllers/components — use the filter chain, `@PreAuthorize`,
  and router `beforeLoad`.
- Store the access token where XSS can read it if it can be avoided; prefer the
  in-memory + httpOnly-refresh pattern.

## Federated SSO login with Authentik (the house template)

Apps can offer **"Login with Authentik"** (central IdP at `sso.jannekeipert.de`)
*alongside* their password login. Authentik only replaces the first step — after the
OIDC dance the backend resolves a local `UserEntity` and issues **its own JWT pair**,
exactly like a password login. Reference implementation: **Strata** (pilot, 2026-07;
backend `services/auth/oauth/` + `configuration/oauth/`).

The template, per app:

- **Authentik side** (in `cluster-deployment`): one blueprint ConfigMap
  (`authentik-<app>-blueprint.yaml`) declaring the groups (`<app>-admins`,
  `<app>-users`), an `oauth2provider` (confidential, **explicit
  `grant_types: [authorization_code, refresh_token]`** — the blueprint default is
  empty and breaks the code flow) and an `application`; client id/secret via `!Env`
  from the `authentik-secrets` SealedSecret; register the ConfigMap in
  `authentik.yaml` under `blueprints.configMaps`. Also add a `localhost` redirect
  URI for local dev.
- **Backend**: hand-rolled authorization-code flow (no spring-security-oauth2-client;
  `RestClient` needs an explicit `@Bean` — Boot doesn't auto-configure a builder).
  Pieces: `OAuthProperties` (provider map + group names), DB-backed single-use
  `state` store (10-min TTL), an `oauth_identity` table
  (unique `(provider, provider_subject)`), and a **separate transactional
  `OAuthUserResolver`** (never self-invoke `@Transactional` from the service).
  `password_hash` becomes nullable — OAuth-only users have none; the password login
  must null-guard before `passwordEncoder.matches`.
- **Policy (Strata's choices, default for internal tools):** unknown identity ⇒
  **always create a new user** (username collision → random suffix); **strict group
  gate** — no `<app>-*` group ⇒ login rejected (`/login?oauthError=noAccess`); **role
  synced from groups on every login** (admins-group→ADMIN else USER, OWNER never
  downgraded). Fine-grained grants stay app-managed.
- **Handoff — no token ever in a URL:** the callback sets the httpOnly refresh
  cookie (`SameSite=Lax` — required for the cross-site top-level redirect) and 302s
  to `{frontend}/`; the SPA's normal cookie bootstrap picks up the session. Failures
  redirect to `/login?oauthError=true|noAccess`.
- **Frontend**: an outline "Login with Authentik" button doing a **full navigation**
  to `/api/v1/auth/oauth/authentik/authorize` (never axios), plus an `oauthError`
  search param on the login route.
- **Ops gotchas:** force blueprint discovery after first apply (mounted-configmap
  race); the consumer app reads the client secret from its own Secret — the values
  must match the ones sealed into `authentik-secrets`; pods read secrets at startup
  (restart after patching).

## Exceptions (not the standard — know them)

- **Bitfrost** runs its *own* OAuth2 **Authorization Server** (Spring Authorization
  Server): RSA-signed JWTs with a JWKS + `/.well-known/openid-configuration` endpoint.
  A full IdP, unlike the self-validated HMAC tokens elsewhere.
- **SAU-Portal** uses **Keycloak** (OIDC) with `oidc-client-ts` on the frontend — an
  external IdP, because it's a multi-team university platform.
- **Mail Service** uses a shared **API key** filter (`ROLE_API_USER`), not JWT — it's a
  machine-to-machine send API.
- **Robert Space Tracker** is passwordless: login emails a JWT link that's exchanged
  for a cookie; single token, no refresh/MFA.

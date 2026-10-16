# ADR-012: Google and GitHub sign-in (scope change)

Status: Accepted  
Date: Sprint 6, P80  

## Context

The original scope ruled out third-party identity providers: accounts were email plus password only (ADR-001). Users now expect to continue with an account they already have, and password reset (US-92) shows that account recovery is the costliest part of owning passwords. US-93 adds Google and GitHub sign-in as a deliberate scope change.

## Decision

1. Spring Security's OAuth2 client runs the provider redirect flow in its own filter chain, matched only to `/oauth2/**` and `/login/oauth2/**`. The API chain stays stateless and unchanged.
2. The application still issues its own tokens. On a successful provider login the success handler issues the same short-lived access JWT and single-use refresh token as password login (ADR-001) and hands them to the single-page app in a URL fragment. Provider tokens are discarded immediately and never stored.
3. Linking is conservative. A sign-in is resolved in this order: a known `(provider, subject)` identity signs in directly; otherwise an existing account with the same email is linked only if the provider marks that email verified, and is refused when it does not; otherwise a new account is created with an unusable random password. GitHub does not return a verified flag on the profile, so the primary verified address is read from its email endpoint, and anything else counts as unverified.
4. Providers are opt-in by configuration. `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are read from the environment. A provider with a missing id or secret is not registered, its authorization URL returns 404, and the sign-in pages do not show its button (the pages ask `GET /api/v1/auth/oauth/providers`).
5. Storage: `oauth_identities` (V24) keeps the stable `(provider, subject)` pair per user, cascading on account deletion. Emails are never used as the lookup key because providers let them change.

## Consequences

- Refusing an unverified email prevents account takeover by anyone able to register a victim's address at a provider.
- A user who signed up with a password can add a provider later, and the reverse, without a second account, but only through a verified address.
- Deployments without credentials behave exactly as before; no existing test or flow depends on a provider being present.
- Provider secrets live only in the environment, never in the repository.

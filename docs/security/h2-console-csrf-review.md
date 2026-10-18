# H2 console CSRF exception — security review

## SonarCloud finding

- Rule: `java:S4502` — verify that disabling Spring Security CSRF protection is safe.
- Reported location: `src/main/java/com/demo/config/SecurityConfig.java`, in the conditional H2 console configuration.
- SonarCloud issue key: `AaEnh-RqvdWhUKH3fb8W`.
- This is an open security finding until it is reviewed and classified in SonarCloud; this document is not a waiver or a closure.

## Scope of the exception

The application leaves CSRF protection enabled by default. The only configured exclusion is the request matcher `/h2-console/**`, and it is registered **only** when `SecurityConfig.isLocalH2ConsoleAllowed(environment)` returns true.

The helper requires **all** of the following:

1. Exactly one active Spring profile, `dev`.
2. `spring.h2.console.enabled=true`.
3. The application-configured `server.address` equals `127.0.0.1`.

When any condition is absent or differs, the exclusion is not registered and the default frame protection is retained. With the exception enabled, frame options are relaxed to `SAMEORIGIN` so that the local console can render.

The exclusion applies to H2 console paths only. It does not exempt application endpoints such as movie administration, checkout, or user management from CSRF checks. This design is for a trusted local development environment, not a deployed application.

## Evidence

- `SecurityConfigTest.h2ConsoleEndpointDoesNotBypassCsrfInTestOrDefaultProfile` verifies a POST to the H2 console path is forbidden under the default/test setup.
- `SecurityConfigTest.defaultTestProfileKeepsDenyFrameProtection` verifies that the default/test setup retains `X-Frame-Options: DENY`.
- `SecurityConfigH2GateTest` (P2.175) verifies that only the explicitly enabled loopback-only `dev` environment passes the gate, including rejection of mixed profiles and public addresses.

These tests establish the gate and default/test behavior. They do **not** constitute a penetration test of a running dev console or prove that every deployment overrides these properties correctly.

## Operational requirements and residual risk

- Never enable the H2 console in production or any publicly accessible environment.
- Do not combine `dev` with other profiles to attempt to enable the exception.
- Verify that runtime configuration really binds the HTTP listener to `127.0.0.1`; ingress, proxies, container port publishing, and tunnels can change effective exposure outside this Java predicate.
- Do not treat loopback as an authentication control. A local process or compromised development host can still reach an enabled console.
- Keep normal application CSRF protection enabled; do not broaden the H2 request matcher.
- If the console becomes unnecessary, disable it and remove the exception rather than expanding its scope.

## SonarCloud disposition

Review the live `java:S4502` issue against the deployed configuration and the evidence above. If the development-only exception is an accepted requirement and actual exposure has been verified, record a project-specific justification and use the appropriate SonarCloud classification (for example, **Accepted** where supported). Do not mark the issue as fixed merely because tests or this document were merged. If a deployment can expose this console to other hosts, keep the finding open and remediate the deployment or code configuration first.

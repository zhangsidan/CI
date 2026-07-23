# Day 4 Status — fx-app-spring

## Tested at which altitude

**Unit** (`com.fx.api.ConversionServiceTest`, `com.fx.core.*`):
- EUR→USD 123.45 → converted 133.55, fee 1.34, net 132.21 (rounding)
- Unknown currency pair throws `UnknownPairException`

**Slice** (`com.fx.api.web.RateControllerTest`):
- `GET /api/rates` → 200 + JSON list
- `GET /api/rates/XX/YY` (unknown pair) → 404 + `{ "error": "..." }`
- `POST /api/conversions` (valid) → 201 + conversion result
- `POST /api/conversions` (negative amount) → 400, validated in web layer before service runs

**Integration** (`com.fx.api.repo.RateRepositoryIT`):
- Real MySQL via Testcontainers; schema built by Liquibase on startup (no seed file)
- `findLatest()` returns exactly 10 rows
- EUR/USD = 1.0818
- Unknown pair returns `Optional.empty()`, not an error

## What CI guards

- `mvn -B verify` on every push and PR — all three altitudes including the Testcontainers IT
- Branch protection on `main` requires `build` to pass; red PR cannot be merged
- OWASP `dependency-check` job surfaces CVE reports (`continue-on-error: true` — informational,
  does not block PRs)

## What CI does NOT guard

- No full end-to-end test: CI does not start the full stack and curl `/api/rates` as a user would
- No front-end coverage (fx-dashboard is Week 3)
- `dependency-check` is tolerated-failure; a flagged CVE surfaces but does not block merging

## Dependabot rule

Dependabot alerts and security updates are enabled on this repo.  
**Team rule:** Dependabot PRs reviewed within one working day; security PRs merged same day
if CI (`build`) is green.

## Honesty checks (observed 2026-07-23)

| Scenario | Command | Result |
|---|---|---|
| MySQL off, Docker on | `./mvnw test` | 29 run, 0 failures, 1 skip — fast tier does not reach MySQL |
| Docker off | `./mvnw verify` | BUILD SUCCESS — but RateRepositoryIT Skipped: 3 — green build hiding a skipped test |
| Docker on (normal) | `./mvnw verify` | 29 + 3 IT, Skipped: 0 — full coverage confirmed |

The Docker-off scenario is the key lesson: `disabledWithoutDocker = true` silently skips the IT
and the build still says SUCCESS. Defence: always read the summary line.

---

## Definition of Done

A change is **done** only when **all** of these hold:

1. From `fx-app-spring/`, **`./mvnw verify` is green** — unit, slice **and** integration, with
   **nothing wrongly skipped** (read the summary line) — **and** `docker compose up` on a clean
   machine serves `/api/rates` returning **10** rates.
2. **The new behaviour has tests at the right altitude**: a new endpoint → a slice test, happy
   *and* failure path; a new calculation → a unit test with boundaries; new or changed SQL → an
   `*IT`.
3. **A teammate reviewed the PR** — checked out the branch, ran `./mvnw verify` themselves, and
   approved.
4. **Merged to `main` only through that PR** — never a direct push.
5. **`main` is still green after the merge**, and a **fresh clone** builds and verifies.

**Not done:** a failing or wrongly-skipped test · "works on my laptop" but not on a fresh clone ·
code merged without review.

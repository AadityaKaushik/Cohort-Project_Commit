# Testing

How Cohort is tested, how to run the suites, and which test proves each sprint check in [`sprints.md`](sprints.md).

## Strategy

Most tests are unit tests over pure logic: mastery, scoring, the status state machine, analytics metrics and the federated learning arithmetic. Above them sit API tests over the real HTTP contract, a few integration tests over the main journeys, and Playwright screenshots for the responsive review. Integration tests run against a real Postgres through Testcontainers, never an in-memory database, because the constraints under test only exist in Postgres.

| Category | What it covers |
|---|---|
| Backend unit | Password hashing; token creation, expiry and rotation; every status transition pair, valid and invalid; dwell time and days in stage; funnel and conversion rates against hand-computed fixtures; mastery with decay and the insufficient-evidence gate; prerequisite routing and its depth cap; recommendation scoring; template rendering and the causal-language linter |
| API | Every endpoint for the happy path, a validation failure, no token (401) and another user's id (404); pagination edges; combined filters; upload rejection; illegal status changes (409) |
| Frontend | React Testing Library and Vitest: forms for login, register, application, interview and debrief; tables with empty, loading and error states; status badges showing text as well as colour; protected routes; confirmation dialogs; recommendation cards and the vector inspector; token contrast in both themes |
| Integration | Register, log in, create, change status, read history. Application, resume, interview, questions, debrief. Practice attempts, mastery, recommendation, accept, task. A full FL round from extraction to ledger entries. Account deletion with an orphan check across every table |
| Security | The endpoint suite built from the OpenAPI schema (401 and 404 everywhere), the client cannot set `user_id`, malicious upload, injection-shaped input, auth rate limit, no email enumeration, fl-service has no credentials for user tables |
| Federated learning | Exactly 24 named floats with no strings; opted-out clients send nothing; a client with fewer than five examples abstains; clipped norms stay within the bound; noised vectors differ from raw ones; a two-client round is refused; an over-norm update is dropped; a failed round keeps the previous model; results are fixed under a seed |

Standing rules:

- Every bug fix comes with a regression test that fails before the fix.
- Every pull request that touches a service adds or updates tests.
- Coverage never goes down; backend line coverage stays at 70% or more.
- The suites run offline and give the same result every time; all randomness is seeded.
- The backend suite stays fast enough that people keep running it.

## Running the tests

```
cd api && mvn verify          # JUnit, MockMvc, Testcontainers, JaCoCo gate (needs Docker or Podman)
cd frontend && npm test       # Vitest and React Testing Library
cd frontend && npx tsc -b && npm run lint
cd fl-service && pytest       # federated learning service
```

CI runs the same suites on every push, plus the `security-gates` job (gitleaks and `scripts/check-sql-interpolation.sh`) and the fl-service database isolation job (`scripts/verify-fl-db-isolation.sh`).

## Evidence by sprint

Java paths are relative to `api/src/test/java/com/cohort/api/`.

### Sprint 1

| Check | Test |
|---|---|
| Status changes follow the transition table | `service/application/state/ApplicationStatusStateRegistryTest` |
| Every finder takes the owner id | `repository/UserScopedRepositoryIntegrationTest` |
| Register, log in, create, change status, read history | `SprintOneJourneyIntegrationTest` |
| Migrations apply in order and roll back | `migration/FlywayMigrationTest` |
| Backend line coverage at least 70% | JaCoCo `check` in `api/pom.xml`, run by `mvn verify` |

### Sprint 2

| Check | Test |
|---|---|
| Filters combine correctly | `controller/ApplicationFilterIntegrationTest` |
| A renamed executable is rejected on upload | `service/resume/storage/ValidatingFileStorageTest`, `service/resume/storage/validation/MagicByteValidationHandlerTest` |
| Another user's resume file returns 404 | `controller/ResumeIntegrationTest` |
| Interview rules and calendar export | `controller/InterviewIntegrationTest`, `service/calendar/IcsCalendarTest` |
| Debrief chips and one-tap save | `frontend/src/pages/interviews/InterviewDebriefPage.test.tsx`, `InterviewDebriefGuardrails.test.tsx` |
| Seed data loads the same way twice | `migration/SeedDataIntegrationTest` |

### Sprint 3

| Check | Test |
|---|---|
| The topic graph has no cycles | `migration/TopicPrerequisiteCycleTest` |
| Every topic has real questions | `migration/QuestionBankContentTest` |
| A metric cannot skip the sample-size gate | `service/preparation/analytics/AnalyticsMetricTest` |
| Analytics formulas match fixed fixtures | `service/analytics/*GoldenFileTest` |
| Mastery decay matches fixed fixtures | `service/preparation/MasteryEngineGoldenFileTest` |
| Stall detection threshold | `service/application/StallDetectionServiceTest`, `controller/StallDetectionIntegrationTest` |

### Sprint 4

| Check | Test |
|---|---|
| Recommendation text never claims a cause | `service/recommendation/template/CausalLanguageLinterTest`, `RecommendationTemplateRegistryTest` |
| The feature vector is exactly 24 named floats | `fl-service/app/common/test_schema.py` |
| No record after the cutoff leaks into a feature | `fl-service/app/client/test_feature_extractor.py` |
| The sent vector is clipped and noised | `fl-service/app/client/test_privacy.py` |
| A round below the minimum cohort is refused | `fl-service/app/server/test_strategy.py`, `test_round_orchestrator.py` |
| An over-norm update is dropped | `fl-service/app/server/test_poisoning_filter.py` |
| A failed round keeps the previous model | `fl-service/app/server/test_model_registry.py` |
| fl-service has no access to user tables | `FlSchemaIsolationIntegrationTest`, CI job running `scripts/verify-fl-db-isolation.sh` |
| The export matches the allow-list | `service/fl/FlFeatureExportJobTest` |
| The Java adapter matches the fl-service API | `service/fl/FlowerServiceAdapterContractTest` |
| fl-service down: recommendations still return 200 | `controller/RecommendationFederatedFaultInjectionIntegrationTest` |
| The ledger stores the real sent vector | `service/fl/PrivacyLedgerServiceTest`, `controller/PrivacyLedgerIntegrationTest` |
| Federated Lift stays within 0.75 to 1.25 | `service/fl/FederatedLiftCalculatorTest` |
| Demo round with synthetic clients fills the ledger | `fl-service/app/demo/test_synthetic.py`, `test_simulate.py`, `controller/DevFlRoundIntegrationTest` |

### Sprint 5

| Check | Test |
|---|---|
| Events fire on the right condition | `service/detection/InterviewEventDetectorTest`, `service/detection/StalledApplicationDetectorTest` |
| Swapping the channel does not touch the detector | `service/notification/NotificationStrategySwapTest`, `NotificationListenerTest` |
| Notifications end to end | `controller/NotificationIntegrationTest` |
| Report output matches a fixture | `service/report/ReportBuilderTest` |
| Argon2id parameters and token lifetimes | `security/PasswordEncoderConfigTest`, `service/TokenServiceTest` |
| Account deletion leaves no orphan rows | `controller/AccountIntegrationTest` |
| Login does not reveal whether an email exists | `service/AuthServiceTest` |
| Reset and verification tokens are single use | `controller/AccountRecoveryIntegrationTest` |

### Sprint 6

| Check | Test |
|---|---|
| Colour tokens pass WCAG AA in both themes | `frontend/src/lib/contrast.test.ts`, `frontend/src/test/theme-contrast.test.ts` |
| Illegal board moves are refused | `controller/ApplicationBoardIntegrationTest` |
| Logging a practice attempt moves mastery | `controller/PracticeMasteryIntegrationTest`, `frontend/src/pages/prep/QuestionBankPage.test.tsx` |
| CSV import, including LinkedIn's export | `controller/ApplicationImportIntegrationTest`, `frontend/src/components/applications/CsvImport.test.tsx` |
| Social sign-in providers and linking rule | `security/oauth/OAuthProviderConfigurationTest`, `frontend/src/pages/auth/OAuthCallbackPage.test.tsx` |
| Pipeline staircase and readiness steps | `frontend/src/components/board/Ascent.test.tsx`, `frontend/src/pages/prep/PrepPage.test.tsx` |
| Security headers on every response | `security/SecurityHeadersIntegrationTest` |
| No secrets in history, no SQL built from strings | CI job `security-gates` |

### Sprint 7

| Check | Test |
|---|---|
| Contacts, companies, offers and goals are isolated | `controller/ContactIntegrationTest`, `CompanyIntegrationTest`, `OfferIntegrationTest`, `GoalIntegrationTest` |
| Directory figures hidden below five contributors | `controller/CompanyDirectoryIntegrationTest`, `ContributionIntegrationTest` |
| Search never returns another user's data | `controller/SearchIntegrationTest` |
| A cloned resume carries its sections | `controller/ResumeSectionIntegrationTest` |

### Sprint 8

| Check | Test |
|---|---|
| Swapping the AI provider changes no feature code | `controller/AiProviderIntegrationTest` |
| Saving an interview question into the bank | `controller/SaveToQuestionBankIntegrationTest`, `frontend/src/components/interviews/SaveToBankControl.test.tsx` |
| Readiness score matches fixed fixtures | `service/preparation/ReadinessMetricGoldenFileTest`, `controller/ReadinessIntegrationTest` |
| Resume and job description match | `controller/ResumeMatchIntegrationTest` |
| Mock interview answers become practice attempts | `service/preparation/mock/MockInterviewServiceTest`, `controller/MockInterviewIntegrationTest`, `frontend/src/pages/prep/MockInterviewPage.test.tsx` |
| Story bank is isolated | `controller/StoryIntegrationTest`, `frontend/src/pages/stories/*.test.tsx` |
| Prep pack page | `frontend/src/pages/interviews/InterviewPrepPage.test.tsx` |
| Email drafts | `controller/EmailDraftIntegrationTest`, `frontend/src/components/ai/EmailDraftPanel.test.tsx` |
| Review queue scheduling | `service/preparation/review/ReviewQueueServiceTest`, `controller/ReviewQueueIntegrationTest` |

### Sprint 9

| Check | Test |
|---|---|
| Every endpoint: 401 without a token, 404 on another user's id | `security/EndpointSecuritySuiteIntegrationTest` |
| Sample data and onboarding | `controller/OnboardingIntegrationTest`, `frontend/src/pages/onboarding/OnboardingPage.test.tsx` |
| Product tour | `frontend/src/components/onboarding/ProductTour.test.tsx` |
| No critical accessibility issues on the main pages | `frontend/src/test/pages-a11y.test.tsx` |

## Design pattern tests

| Pattern | Code | Test |
|---|---|---|
| State | `service/application/state/` | `ApplicationStatusStateRegistryTest` |
| Repository with owner scoping | `repository/UserScopedRepository` | `UserScopedRepositoryIntegrationTest` |
| Specification | `repository/specification/` | `ApplicationFilterIntegrationTest` |
| Strategy | file storage, notification channels, evidence tiers, mail sender, AI provider | `NotificationStrategySwapTest`, `EvidenceTierClassifierTest`, `AiProviderIntegrationTest` |
| Decorator and Chain of Responsibility | `ValidatingFileStorage` and its validation handlers | `ValidatingFileStorageTest` |
| Template Method | `AnalyticsMetric` | `AnalyticsMetricTest` |
| Chain of Responsibility | recommendation generators | `DecayGeneratorTest`, `StalledApplicationGeneratorTest`, `RootCauseRouterTest` |
| Observer | domain events and `NotificationListener` | `InterviewEventDetectorTest`, `NotificationListenerTest` |
| Facade and Builder | `RecommendationFacade`, `ReportBuilder` | `RecommendationIntegrationTest`, `ReportBuilderTest` |
| Adapter and Circuit Breaker | `FlowerServiceAdapter` with Resilience4j | `FlowerServiceAdapterContractTest`, `RecommendationFederatedFaultInjectionIntegrationTest` |

## Manual checks before a demo

- [ ] A debrief from the dashboard prompt takes under three minutes.
- [ ] axe-core reports no critical issues on the main pages in light and dark themes.
- [ ] Every page works at 375 px with no horizontal scroll, and looks right at 1440 px.
- [ ] With the FL profile running, a demo round adds a Privacy Ledger entry (see "Demo round with synthetic clients" in `fl-service/README.md`).
- [ ] Each sign-in provider that is configured in `.env` completes a sign-in.
- [ ] The full demo runs end to end from a fresh clone with seed data.

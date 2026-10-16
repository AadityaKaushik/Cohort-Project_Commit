# Evidence

Where each check from the plan is tested. Paths under `api/` are relative to `api/src/test/java/com/cohort/api/`. Run everything with `mvn verify` in `api/`, `npm test` in `frontend/` and `pytest` in `fl-service/`. CI runs the same on every push to main.

## Sprint checks

| Check | Sprint | Test |
|---|---|---|
| Status changes follow the transition table; illegal moves are refused | 1, 6 | `service/application/state/ApplicationStatusStateRegistryTest`, `controller/ApplicationBoardIntegrationTest` |
| Every finder takes the owner id | 1 | `repository/UserScopedRepositoryIntegrationTest` |
| Register, log in, create, change status, read history | 1 | `SprintOneJourneyIntegrationTest` |
| Backend line coverage at least 70% | 1 | JaCoCo `check` in `api/pom.xml`, run by `mvn verify` |
| A renamed executable is rejected on upload | 2 | `service/resume/storage/ValidatingFileStorageTest`, `service/resume/storage/validation/MagicByteValidationHandlerTest` |
| Another user's resume file returns 404 | 2 | `controller/ResumeIntegrationTest` |
| Seed data loads the same way twice | 2 | `migration/SeedDataIntegrationTest` |
| The topic graph has no cycles | 3 | `migration/TopicPrerequisiteCycleTest` |
| Every topic has real questions | 3 | `migration/QuestionBankContentTest` |
| Analytics formulas match fixed fixtures | 3 | `service/analytics/*GoldenFileTest` |
| Mastery decay matches fixed fixtures | 3 | `service/preparation/MasteryEngineGoldenFileTest` |
| A metric cannot skip the sample-size gate | 3 | `service/preparation/analytics/AnalyticsMetricTest` |
| Recommendation text never claims a cause | 4 | `service/recommendation/template/CausalLanguageLinterTest`, `RecommendationTemplateRegistryTest` |
| The feature vector is exactly 24 named floats | 4 | `fl-service/app/common/test_schema.py` |
| No record after the cutoff leaks into a feature | 4 | `fl-service/app/client/test_feature_extractor.py` |
| The sent vector is clipped and noised | 4 | `fl-service/app/client/test_privacy.py` |
| A round below the minimum cohort is refused | 4 | `fl-service/app/server/test_strategy.py`, `test_round_orchestrator.py` |
| An over-norm update is filtered | 4 | `fl-service/app/server/test_poisoning_filter.py` |
| A failed round keeps the previous model | 4 | `fl-service/app/server/test_model_registry.py` |
| fl-service has no access to user tables | 4 | `FlSchemaIsolationIntegrationTest`, CI job running `scripts/verify-fl-db-isolation.sh` |
| fl-service down: recommendations still return 200 | 4 | `controller/RecommendationFederatedFaultInjectionIntegrationTest` |
| The Java adapter matches the fl-service API | 4 | `service/fl/FlowerServiceAdapterContractTest` |
| Argon2id parameters and token lifetimes | 5 | `security/PasswordEncoderConfigTest`, `TokenServiceTest` |
| Account deletion leaves no orphan rows | 5 | `controller/AccountIntegrationTest` |
| Login does not reveal whether an email exists | 5 | `service/AuthServiceTest` |
| Reset and verification tokens are single use | 5 | `controller/AccountRecoveryIntegrationTest` |
| Colour tokens pass WCAG AA in both themes | 6 | `frontend/src/lib/contrast.test.ts` |
| Security headers on every response | 6 | `security/SecurityHeadersIntegrationTest` |
| No secrets in history, no SQL built from variables | 6 | CI job `security-gates` (gitleaks and `scripts/check-sql-interpolation.sh`) |
| Search never returns another user's data | 7 | `controller/SearchIntegrationTest` |
| Every endpoint: 401 without a token, 404 on another user's id | 9 | `security/EndpointSecuritySuiteIntegrationTest` |
| No critical accessibility issues on the main pages | 9 | `frontend/src/test/pages-a11y.test.tsx` |

## Design patterns

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

## Added after the plan

| Feature | Test |
|---|---|
| Save a question from an interview into the question bank | `controller/SaveToQuestionBankIntegrationTest`, `frontend/src/components/interviews/SaveToBankControl.test.tsx` |
| Self-graded mock interview | `service/preparation/mock/MockInterviewServiceTest`, `controller/MockInterviewIntegrationTest`, `frontend/src/pages/prep/MockInterviewPage.test.tsx` |
| STAR story bank | `controller/StoryIntegrationTest`, `frontend/src/pages/stories/*.test.tsx` |
| FL demo round with synthetic clients | `fl-service/app/demo/test_synthetic.py`, `test_simulate.py`, `controller/DevFlRoundIntegrationTest` |

## Checked by hand, not by a test

- The debrief takes under 3 minutes.
- Screenshots at 390 px and 1440 px in both themes.
- The app works at 375 px without sideways scrolling.
- A Privacy Ledger entry end to end with the FL profile running (see "Demo round with synthetic clients" in `fl-service/README.md`).
- Two full demo rehearsals.

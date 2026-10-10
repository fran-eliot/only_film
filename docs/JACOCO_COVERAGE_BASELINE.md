# JaCoCo coverage baseline — H2 regression job

## Scope and provenance

The existing `h2-regression` job in `.github/workflows/postgres-migration.yml` runs a targeted Maven Surefire test selection with `-DrerunFailingTestsCount=0`. The `jacoco-maven-plugin` configured in `pom.xml` attaches its agent and generates HTML/XML coverage reports during `mvn test`. The job uploads `target/site/jacoco/` and `target/surefire-reports/` in the **jacoco-h2-regression** artifact (14-day retention). The upload step uses `if: always()`, so test failure does not prevent collecting any reports that were produced.

Find it under **Actions → PostgreSQL schema and regression validation → PR run → Artifacts → jacoco-h2-regression**. Open `target/site/jacoco/index.html` locally, or inspect `target/site/jacoco/jacoco.xml` for machine-readable counters. The artifact is evidence of the selected H2 regression tests, **not** a coverage percentage for every test in the repository.

## Reproduce locally

With Java 25 and Maven available at the repository root:

```bash
mvn -B -ntp clean test -Dtest='*RepositoryTest,*ServiceTest,*ControllerTest,*SecurityConfigTest,TicketSecurityRegressionTest,TicketCheckoutRegressionTest,TicketCheckoutConcurrencyTest,MutationCsrfSecurityTest,EnvironmentHardeningTest,SessionManagementRollbackTest,SessionEditInvariantsTest,MvcErrorContractTest' -DrerunFailingTestsCount=0
```

The resulting HTML and XML reports are generated under `target/site/jacoco/`. Use `clean` locally to avoid stale execution data. This job runs on a fresh GitHub-hosted runner.

## Interpretation and next steps

- Record actual line/branch coverage **only after inspecting a generated report**; no baseline percentage has been verified as of this change.
- This job excludes Selenium browser scenarios and other classes not matched by its Surefire selector; separate workflows run PostgreSQL tests and other validations.
- Coverage across independent jobs/JVMs cannot be computed by averaging their percentages. A whole-suite figure would require a deliberately unified execution and properly aggregated JaCoCo execution data.
- A passing test suite does not prove every business invariant is covered. Prioritize checkout concurrency, purchase authorization, session updates, seat uniqueness and administrative ticket edits when reviewing uncovered paths.
- No minimum threshold is enforced by this change; agree on new-code expectations after measuring an actual baseline.

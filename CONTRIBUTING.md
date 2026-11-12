# Contributing to FlowEdu

Thanks for taking the time to improve FlowEdu.

## Getting set up

Prerequisites: Java 17+, Maven 3.8+, Node.js 20+ and npm. See the README for the full quick start.

```bash
cp .env.example .env
cd backend && ./run.sh          # API on http://localhost:8080
cd frontend && npm ci && npm run dev   # UI on http://localhost:5173
```

## Before you open a pull request

```bash
cd backend && mvn verify
cd frontend && npm run lint && npm test && npm run build
```

CI runs the same checks. Please add or update tests for behaviour you change.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/) in the imperative mood, for example:

```
feat(workflow): add a weekly digest template
fix(import): reject attendance rows above 100 percent
docs: explain the cron format
```

Keep each commit to one coherent change that builds and passes the checks on its own.

## Adding a workflow action

Implement the `Action` interface and annotate the class with `@Component`; the engine discovers it automatically. Add unit tests, and never log credentials.

## Reporting security issues

Follow [SECURITY.md](SECURITY.md) instead of opening a public issue.

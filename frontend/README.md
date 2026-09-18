# Cohort — frontend

React + TypeScript + Tailwind CSS v4 + shadcn/ui (radix-nova preset), built with Vite.

See the repository root [`README.md`](../README.md) for the full project. This package is the
`frontend/` piece described there.

## Structure

```
src/
  components/
    layout/    AppShell, AppSidebar — the persistent app frame
    shared/    Cross-feature building blocks (EmptyState, ...)
    ui/        shadcn/ui primitives — generated, not hand-edited
  lib/         Navigation config, utilities
  pages/       Route-level components
```

Feature code (once features start landing from Sprint 2 onward) lives under
`src/features/<feature>/`, keeping each feature's pages, components, hooks, and API client
together rather than splitting by file type.

## Local development

```
npm install
npm run dev
```

Expects the API at `VITE_API_BASE_URL` (see the root `.env.example`), default
`http://localhost:8080/api/v1`.

## Scripts

- `npm run dev` — Vite dev server with HMR
- `npm run build` — type-check (`tsc -b`) then production build
- `npm run lint` — Oxlint
- `npm run preview` — preview a production build locally

# Cohort frontend

React + TypeScript + Tailwind CSS v4 + shadcn/ui, built with Vite. See the root [`README.md`](../README.md) for the whole project.

## Development

```
npm install
npm run dev
```

The API is expected at `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`, see `.env.example`).

## Scripts

- `npm run dev` Vite dev server
- `npm run build` type-check (`tsc -b`) and production build
- `npm run lint` Oxlint
- `npm test` Vitest
- `npm run preview` preview a production build

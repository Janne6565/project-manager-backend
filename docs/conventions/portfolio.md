<!-- AUTO-SYNCED from agents KB: projects/portfolio.md @ e5e5376.
     Do NOT edit here — edit the source in ~/projects/agents and re-run scripts/sync-conventions.sh. -->

# Portfolio

The user's personal portfolio site plus the "project manager" system that catalogs every other project (and exposes them to agents via MCP).

- **Live:** https://jannekeipert.de (portfolio) · project-manager.jannekeipert.de · project-fetcher.jannekeipert.de
- **Repos:** github.com/Janne6565/{`project-manager-frontend`, `project-manager-backend`, `project-contribution-fetcher`, `project-manager-mcp` (the MCP server, TS/Bun, single `src/index.ts`), `projektejwkk-new` (current portfolio site, Next.js 15 + React 19), `projektejwkk` (previous Vite + React build)}
- **Local:** clone the repo(s) listed above into `~/projects/portfolio/` — single-repo → directly into `~/projects/portfolio/`, multi-repo → one subfolder per repo (`~/projects/portfolio/<repo-name>/`). Always `git pull` before reading. See [repo conventions](README.md#local-repos--clone-on-demand-pull-before-reading).
- **Cluster:** `portfolio`, `project-manager`, `project-fetcher`, `projektejwkk`

## Idea
A meta-project: the portfolio front-end renders projects pulled from a "project manager" backend, a "contribution fetcher" aggregates GitHub contributions, and an MCP server exposes the project catalog to AI assistants. Hidden in the project manager itself (`isVisible=false`).

## Stack
- **Portfolio site:** Next.js 15, React 19, TypeScript, Tailwind CSS, shadcn/ui (migrated from an earlier Vite + React build)
- **Project manager:** separate frontend + backend services
- **MCP server:** TypeScript on Bun/Node, wraps the project-manager REST API
- **Contribution fetcher:** aggregates GitHub activity
- **Infra:** Docker, Kubernetes (per-service namespaces)

## Notable (stands out vs other projects)
- The meta-project that catalogs all the others — the source of this very project list.
- Ships an MCP server (`project-manager-mcp`) exposing 8 tools to agents: `list_projects`, `get_project`, `create_project`, `update_project`, `delete_project`, `toggle_project_visibility`, `update_project_index`, `list_unassigned_repositories`.
- MCP needs `PROJECTMANAGER_API_KEY` + `PROJECTMANAGER_BASE_URL` env vars and talks to the backend via an `X-API-Key` header.
- Split across four cluster namespaces for four cooperating services.

## Notes for agents
- The MCP server is a thin wrapper over the project-manager REST API — behavior changes usually belong in `project-manager-backend`, not the MCP.
- Two portfolio front-ends exist: `projektejwkk-new` (Next.js, current) vs `projektejwkk` (Vite, older). Edit the Next.js one unless told otherwise.
- `MIGRATION_PLAN.md` in `projektejwkk-new` documents the ongoing move.

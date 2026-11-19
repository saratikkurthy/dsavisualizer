# DSA Visualizer — Design (Visualgo-style, Java + Spring Boot + React)

A web app for learning data structures visually, in the style of
https://visualgo.net/en. The user picks a data structure or algorithm,
enters data, and watches it run step by step — with the array / nodes /
tree drawn on screen, the current comparison highlighted, and the code
line explained. Free to host, CI/CD on every push.

## How Visualgo works (the model to copy)

Visualgo's core idea is a **step trace**: the algorithm is not run to
completion and shown as a result. Instead it is run once, slowly, and
every meaningful moment is recorded as a **step** (a snapshot of the
data + what just happened + a one-line explanation). The UI then just
plays those steps back — play, pause, step forward/back, change speed.
That separation is the whole design:

- The **backend** owns the algorithm and produces the trace.
- The **frontend** owns the drawing and the playback controls.
- The trace is plain JSON, so any frontend can render it.

## Architecture

```
React (Vite) ── fetch ──> Spring Boot (REST)
  draws the steps            computes the trace
  play / pause / speed       one endpoint per topic
  input data, code panel     returns List<Step> as JSON

Step (the contract between backend and frontend):
  {
    "data":        [5, 2, 8, 1],     // the array / nodes after this step
    "highlight":   [0, 1],           // indexes being compared
    "swapped":     [0, 1],           // indexes just swapped (or [])
    "sorted":      [3],              // indexes already in final place
    "message":     "Compare 5 and 2 — 2 is smaller, swap them.",
    "line":        3                 // line number in the shown code
  }
```

Why the backend computes the trace (instead of the frontend):
- The Java code IS the algorithm. Babu learns DSA in Java; the trace
  comes from real Java, not a JS re-implementation.
- One source of truth: add a new algorithm in Java, the frontend
  renders it with no new logic.
- The frontend stays a pure player (draw + controls), which is easier
  to test and extend.

## Features (phased)

Phase 1 — scaffold (this repo, built now):
- Sorting: Bubble Sort, Selection Sort, Insertion Sort (trace API +
  bar-chart visualizer, play/pause/step/speed, custom input, random).
- Array: create, insert, delete, search (linear + binary) — the trace
  shows the probe index moving.
- Navigation + topic pages, code panel with the current line
  highlighted, Big-O shown per algorithm.

Phase 2 — data structures (add one endpoint + one visualizer each):
- Stack (push/pop), Queue (enqueue/dequeue), Singly Linked List
  (insert/delete/traverse), Binary Search Tree (insert/search/delete),
  Min-Heap (insert/extract-min), Hash Table (put/get, chaining),
  Graph BFS/DFS (node/edge highlighting).

Phase 3 — polish:
- User accounts + saved progress (optional, needs a free DB — see below).
- Quiz mode ("what happens next?"), speedrun / step-count display.
- Dark mode, shareable links (encode the input in the URL).

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 17, Spring Boot 3, Maven | Babu's stack; real Java algorithms |
| Frontend | React 18 + Vite | Fast dev, small build, free hosting |
| Styling | Plain CSS (no framework) | Keeps the scaffold readable; add Tailwind later if wanted |
| Drawing | SVG / divs | No canvas library needed for bars, nodes, edges |
| Tests | JUnit 5 (backend), Vitest (frontend, optional) | CI runs both |
| CI/CD | GitHub Actions | Free for public repos |
| Frontend host | Vercel (free) | Zero-config for Vite; global CDN |
| Backend host | Render (free tier) | Runs the Spring Boot JAR; free tier spins down after ~15 min idle (first request is slow — acceptable for a learning app) |
| (Alternative) | Single JAR on Render | Spring Boot serves the React build too; one URL, no CORS. Simpler; use this if two services feel like overhead. |

## API (Phase 1)

- `GET /api/health` — `{ "status": "ok" }` (Render health check).
- `POST /api/sort/bubble` — body `{ "values": [5,2,8,1] }` →
  `{ "algorithm": "Bubble Sort", "steps": [ Step, ... ] }`.
- `POST /api/sort/selection`, `POST /api/sort/insertion` — same shape.
- `POST /api/search/linear` — body `{ "values": [...], "target": 8 }`.
- `POST /api/search/binary` — same; values must be sorted.
- CORS: `*` in dev; restrict to the Vercel domain in prod
  (`app.cors.allowed-origins` in application.properties).

## CI/CD (GitHub Actions, .github/workflows/ci.yml)

On every push to `main` and on pull requests:
1. Build backend: `mvn -B -q verify` (compiles + runs JUnit).
2. Build frontend: `npm ci && npm run build` (fails the run if the
   React build breaks).
3. Deploy frontend to Vercel (on push to main only) via the Vercel
   GitHub integration or `vercel --prod --token`.
4. Deploy backend to Render (on push to main only) via Render's
   GitHub integration (auto-deploy on push) or a deploy hook.

## Free deployment (step by step)

1. Push this repo to GitHub (public, free).
2. Frontend — Vercel:
   - Import the repo at vercel.com, set Root Directory to `frontend`,
     Framework Preset to Vite. Deploy. You get `https://<name>.vercel.app`.
   - Set env var `VITE_API_URL` to the Render backend URL.
3. Backend — Render:
   - New Web Service, connect the same repo, Root Directory `backend`,
     **Language: Docker** (Render has no native Java runtime; the
     backend deploys from `backend/Dockerfile`, which builds the JAR
     with Maven and runs it on a JRE image — no Build/Start command).
     (Or use the `render.yaml` in this repo — Blueprint deploy.)
   - Free tier: 512 MB RAM, spins down after inactivity; first hit
     takes ~30–60 s to wake. Fine for learning. The Dockerfile caps
     the JVM heap at 384 MB so it fits the 512 MB limit.
   - Set env var `CORS_ALLOWED_ORIGINS` to the Vercel URL.
4. Open the Vercel URL from anywhere — the app is public.

Cost: $0. Limits to know: Render free sleeps; Vercel free is generous
for a learning app; GitHub Actions free minutes are plenty here.

## Local run

```bash
# backend (needs Java 17 + Maven)
cd backend && mvn spring-boot:run        # http://localhost:8080

# frontend (needs Node 18+)
cd frontend && npm install && npm run dev  # http://localhost:5173
```

Set `VITE_API_URL=http://localhost:8080` in `frontend/.env.local`
for local dev (a sample `.env.example` is included).

## File map

```
dsa-visualizer/
  DESIGN.md                  # this file
  README.md                  # quickstart + deploy
  backend/
    pom.xml
    src/main/java/com/sarat/dsavisualizer/
      DsavisualizerApplication.java
      controller/VisualizationController.java
      service/SortingService.java
      service/SearchService.java
      model/Step.java
      model/TraceRequest.java
      model/TraceResponse.java
    src/main/resources/application.properties
    src/test/java/com/sarat/dsavisualizer/SortingServiceTest.java
  frontend/
    package.json, vite.config.js, index.html
    src/main.jsx, src/App.jsx, src/App.css
    src/services/api.js
    src/components/SortingVisualizer.jsx
    src/components/ArrayVisualizer.jsx
  .github/workflows/ci.yml
  render.yaml
  vercel.json
```

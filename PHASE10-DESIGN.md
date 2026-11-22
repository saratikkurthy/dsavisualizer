# Phase 10 Design — Finish the Accounts, More Algorithms, Faster First Load

Babu picked "All of these" (2026-11-19) for the three remaining items:
Google sign-in + password reset, more algorithms (Bellman-Ford,
topological sort, 0/1 Knapsack), faster first load (code splitting).

## 0. What exists now (read before changing)

- Backend: `~/workspace/dsa-visualizer/backend/` — Spring Boot 3.2.5,
  Java 17. 35 POST endpoints. `Step` model: `{data, highlight,
  swapped, sorted, message, line, nodes, edges, aux, buckets,
  mstEdges}`. `TraceRequest`: `{values, value, target, index,
  capacity, graphEdges, start, words, word}`. `EdgeDto`: `{from, to,
  weight}` (weight nullable). `NodeDto`: `{id, value, x, y, label}`.
  Graph edges are `[from,to]` or `[from,to,weight]`.
- Frontend: `~/workspace/dsa-visualizer/frontend/src/` — React 18 +
  Vite, plain CSS, dark theme (Phase 6, CSS variables in `App.css`).
  14 nav modules (Sorting, Stack, Queue, Linked List, BST, Heap, Hash
  Table, Graph, AVL, Trie, Search, Quiz, Lectures, My Progress).
  Accounts (Phase 8): `src/lib/firebase.js` (exports `firebaseReady`,
  `auth`, `db`; null/false when the 6 `VITE_FIREBASE_*` env vars are
  missing), `src/lib/sync.js` (`signUp`, `signIn`, `signOutUser`,
  `onAuthChange`, `loadCloudProgress`, `saveCloudProgress` debounced,
  `mergeProgress`, `getCurrentUser`, `onSyncStatusChange`,
  `friendlyAuthError`), `src/lib/progress.js` (localStorage progress
  with `updatedAt`, cloud save on sign-in, module-level
  `initCloudSync()` subscribing to auth changes),
  `src/components/ProgressVisualizer.jsx` (account section: sign up /
  sign in form, or the signed-in email + sign-out + sync status).
  MFA (Phase 9): TOTP enrollment / sign-in / unenroll in `sync.js`
  and `ProgressVisualizer.jsx`; `qrcode` package renders the QR code
  locally. Firebase SDK `firebase: ^13.4.0`, `qrcode: ^1.5.4`.
- Firebase project (live since 2026-11-18): `dsa-visualizer`,
  project ID `dsa-visualizer-a881b`, Spark plan. Email/Password auth
  enabled, Firestore in Production mode, the `progress/{userId}` rule
  published, web app registered, the 6 `VITE_FIREBASE_*` vars in
  Vercel, Phase 9 code deployed and working (Babu confirmed).
- JS bundle is ~1,015 kB (273 kB gzip); Vite warns about the >500 kB
  chunk (Firebase SDK + `qrcode` are in the initial chunk).
- Line convention (keep it): compare/visit = 2, swap/mutation = 3,
  final = 0.
- Verified limits (same as Phases 1–9): frontend `npm run build` is
  verified on Muse's machine; backend is hand-checked only (no JDK /
  Docker here) — `mvn verify` on Babu's machine / CI is the real
  confirmation.

## 1. Firebase console steps (Babu does these — for the delivery message)

Google sign-in:
1. Firebase console → Authentication → Sign-in method → Google →
   Enable → choose a support email → Save.
2. The Google provider uses the project's OAuth consent screen. If
   the consent screen is in **Testing** mode, only listed test users
   can sign in with Google. To let anyone sign in, publish it:
   Google Cloud console → APIs & Services → OAuth consent screen →
   fill in the app name + support email → **Publish app**
   (Production mode). No Google verification review is needed for
   the basic scopes this uses (openid / email / profile).
Password reset needs no console step — Firebase sends the email with
its default template (from noreply@<project>.firebaseapp.com); the
link opens Firebase's default password-reset page.
Identity Platform + TOTP (Phase 9) is still the separate MFA switch
and is NOT part of this phase.

## 2. Backend — Bellman-Ford

New `bellmanFord` method on `GraphService` + endpoint
`POST /api/graph/bellmanford`.

- Request `{graphEdges, start}` (weighted edges; **weights MAY be
  negative** — that is the point of Bellman-Ford, unlike Dijkstra /
  Prim's / Kruskal's, which reject weight <= 0). Controller
  validation, 400 when: `graphEdges` null/empty; `start` null.
  (No weight validation at all — negative and zero weights are
  legal here.)
- Trace: `data` = distances array (index = node id, JSON null =
  infinity), `highlight` = [u, v] (the edge being relaxed),
  `nodes`/`edges` = full graph. Steps: V-1 iterations over all edges
  in input order. Per edge relaxation: a Step (highlight=[u,v],
  message `Relax edge 1-2 (weight -3): dist[2] 2 -> 1.`, line 2 if
  it improves, line 3 if it does not improve — no, keep it simple:
  line 2 for every relaxation step, and the message says whether it
  improved). Per iteration: after the edges, a Step summarising it
  (`Iteration 1 complete. Distances: [0, 4, 1, 9, 11, 14].`, line 2).
  **Early exit:** if an iteration relaxes nothing, stop and emit the
  final step (do not run the remaining iterations). After the
  iterations, run one detection pass over all edges: if any edge can
  still be relaxed, emit the final step
  `Negative cycle detected — no shortest path exists.` (line 0) and
  stop; otherwise emit the final step
  `Shortest distances from 0: [0, 4, 1, 9, 11, 14].` (line 0).
- Test: edges `[[0,1,4],[0,2,2],[1,2,-3],[1,3,5],[2,3,8],[2,4,10],
  [3,4,2],[3,5,6],[4,5,3]]` start 0 → final distances
  `[0, 4, 1, 9, 11, 14]`. This example is chosen because Dijkstra
  gets node 2 WRONG here (it finalises 2 at distance 2 before
  discovering the shorter path 0→1→2 = 1 via the negative edge) —
  the lecture slide must say so.
  Verify by hand: after iteration 1, d = [0,4,1,9,11,14] (1-2
  improves 2 from 2 to 1); iteration 2 relaxes nothing → early exit.
- Negative-cycle test: edges `[[0,1,1],[1,2,-2],[2,0,-1]]` start 0
  (cycle weight 1-2-1 = -2) → last message contains
  "Negative cycle".

## 3. Backend — Topological sort

New `topoSort` method on `GraphService` + endpoint
`POST /api/graph/toposort`.

- Request `{graphEdges}` (directed edges; weights are ignored —
  topological sort does not use them). Controller validation, 400
  when: `graphEdges` null/empty.
- Trace: Kahn's algorithm (in-degree based). `data` = in-degree
  array (index = node id), `highlight` = [the node being output],
  `sorted` = nodes in the output order so far, `nodes`/`edges` =
  full graph. Steps: per node output (highlight=[node], message
  `Output node 0 (in-degree 0).`, line 2); when a neighbour's
  in-degree drops to 0, a Step (message `Node 2 in-degree 1 -> 0 —
  ready.`, line 3). **Tie-breaking (specify it exactly):** the ready
  queue is FIFO, and neighbours are processed in ascending node-id
  order. If the output has fewer nodes than V (a cycle exists),
  final: `Cycle detected — no topological order exists.` (line 0);
  otherwise final: `Topological order: [0, 1, 2, 3, 4, 5].`
  (line 0).
- Test: edges `[[0,1],[0,2],[1,3],[2,3],[3,4],[3,5],[4,5]]` →
  order `[0, 1, 2, 3, 4, 5]` (FIFO queue, neighbours ascending).
  Verify: in-degrees 0:0, 1:1, 2:1, 3:2, 4:1, 5:2; output 0 (1,2
  ready), 1, 2 (3 ready), 3 (4 ready), 4 (5 ready), 5.
- Cycle test: edges `[[0,1],[1,2],[2,0]]` → last message contains
  "Cycle".

## 4. Backend — 0/1 Knapsack (DP)

New service `KnapsackService` (separate service file, matching the
Phase 4/8 pattern) + endpoint `POST /api/dp/knapsack`.

- Request `{weights, values, capacity}`. Controller validation, 400
  when: `weights`/`values` null/empty; lengths differ; `capacity` <
  0; `capacity` > 30 (the table must stay drawable); more than 10
  items; any weight <= 0; any value < 0.
- `TraceRequest` gains `weights` (`List<Integer>`), `values`
  (`List<Integer>`), `capacity` (`Integer`).
- Trace: the DP table. `Step` gains an optional nullable `table`
  (`List<List<Integer>>`, section 5) — the full table snapshot at
  each step (rows 0..n, columns 0..W; uncomputed cells are JSON
  null). `highlight` = [row, col] (the cell being computed — the
  frontend interprets it as a cell coordinate when `table` is
  present). Steps: one Step per cell computed (row i, col w),
  message `dp[2][5] = max(dp[1][5], 4 + dp[1][2]) = max(3, 7) = 7.`
  (line 2); when the item is taken (the value improves over the
  cell above), line 3 instead. Final Step:
  `Maximum value: 7. Items taken: 0, 1.` (line 0), with the table
  complete and highlight = the final cell [n, W]. The backend
  computes the taken items by tracing back from (n, W) and reports
  them as 0-based indices in the final message.
- Test: weights `[2,3,4,5]`, values `[3,4,5,6]`, capacity 5 → final
  max value 7, items taken `[0, 1]`. Verify by hand: row 1
  (item 0: w2 v3): [0,0,3,3,3,3]; row 2 (item 1: w3 v4):
  [0,0,3,4,4,7]; row 3 (item 2: w4 v5): [0,0,3,4,5,7]; row 4
  (item 3: w5 v6): [0,0,3,4,5,7]. Traceback from (4,5): item 3
  not taken (7 = dp[3][5]), item 2 not taken (7 = dp[2][5]),
  item 1 taken (7 > dp[1][5] = 3), remaining capacity 2, item 0
  taken (3 > dp[0][2] = 0). Items [0,1], value 7.
- Small tests: weights `[1,2]`, values `[10,20]`, capacity 2 →
  max 20, items `[1]`; capacity 0 → max 0, items `[]`.

## 5. Step / TraceRequest extensions (backend)

- `Step.java`: add optional nullable `table`
  (`List<List<Integer>>`) — the DP table snapshot, used by Knapsack
  only. Getters/setters, default null. Do not break the existing
  constructor.
- `TraceRequest.java`: add `weights` (`List<Integer>`), `values`
  (`List<Integer>`), `capacity` (`Integer`).

## 6. Frontend — new algorithm UI

- **GraphVisualizer.jsx**: add **Bellman-Ford** and **Topo Sort**
  buttons (alongside BFS/DFS/Dijkstra/Prim's/Kruskal's). Bellman-Ford
  calls `POST /api/graph/bellmanford` `{graphEdges, start}` (uses
  the start input; the edge input must accept negative weights —
  the existing `0-1:-3` format already parses a leading minus, so
  check it works and fix it if it does not). Topo Sort calls
  `POST /api/graph/toposort` `{graphEdges}` (no start). The
  distances/key table shows for Dijkstra (distances), Prim's (keys)
  and Bellman-Ford (distances), and for Topo Sort it shows the
  in-degree array (labelled "in-degree"); hidden for BFS/DFS/
  Kruskal's. Pseudocode for Bellman-Ford and Topo Sort (compare/
  visit = 2, mutation = 3).
- **New DPVisualizer.jsx + new nav module "DP"** (15th module, after
  Trie — it belongs with the algorithm modules). Inputs: weights
  (comma-separated, default `2,3,4,5`), values (default `3,4,5,6`),
  capacity (default `5`). Button: Solve
  (`POST /api/dp/knapsack` `{weights, values, capacity}`). Draw the
  table from `step.table` as a grid (rows = items 0..n with each
  row labelled by the item's weight/value, columns = capacity
  0..W); uncomputed cells (null) render empty/grey; the current cell
  (`highlight` = [row, col]) is amber; the final cell is green.
  Frontend validation: capacity 0–30, at most 10 items, weights >
  0, values >= 0 (show an inline error otherwise). Pseudocode for
  knapsack. Dark theme applies — use the CSS variables, do not
  introduce light colours. (The table cells themselves are data, not
  theme — keep them readable on the dark background.)
- **App.jsx**: add "DP" to the nav after Trie. Keep the dark theme
  and Phase 5 motion exactly.

## 7. Frontend — quiz & lecture content for the new algorithms

- **quizData.js**: extend the banks (keep every question factually
  correct and unambiguous):
  - Graph +2: 1 on Bellman-Ford (negative weights, why Dijkstra
    fails on them, O(V·E)), 1 on topological sort (what it computes,
    Kahn's algorithm, cycle detection).
  - New **DP** module bank: 5 questions (what dynamic programming
    is, the knapsack table, what dp[i][w] means, complexity O(n·W),
    when to use DP vs greedy).
  - Total new questions: 7 (65 → 72). Rounds still sample 5 per
    module (Phase 8 rule).
- **lectureData.js**: Graph +2 slides (Bellman-Ford, Topological
  Sort — the Bellman-Ford slide MUST state that Dijkstra gets node 2
  wrong on the default example because of the negative edge);
  new **DP** lecture (4–5 slides: what DP is, the knapsack table,
  how the recurrence works, complexity O(n·W), when to use it).
  Total new slides: 6–7 (65 → 71–72). Complexities must match the
  design doc exactly: Bellman-Ford O(V·E), topological sort O(V+E),
  knapsack O(n·W).
- **traceQuiz.js**: none of the three new algorithms is added (none
  is array-based).
- **progress.js / ProgressVisualizer.jsx**: add the DP module to
  `PROGRESS_MODULES` (quizKey `DP`, lectureKey `dp`) so its scores
  and slides track like the other modules. Do NOT touch the account
  or MFA sections of ProgressVisualizer (that belongs to the accounts
  agent).

## 8. Frontend — Google sign-in + password reset (accounts agent)

- **sync.js**: add `signInWithGoogle()` — `signInWithPopup(auth,
  new GoogleAuthProvider())`. If it throws
  `auth/multi-factor-auth-required`, return
  `{ mfaRequired: true, resolver }` exactly like the email `signIn`
  does (the component reuses the same second-factor prompt —
  `resolveMfaSignIn` already handles any resolver). On success,
  return the user credential. If the popup is blocked/closed
  (`auth/popup-blocked`, `auth/popup-closed-by-user`), throw a plain
  message ("The sign-in window was blocked or closed — allow popups
  for this site and try again."). After a Google sign-in, if the
  local progress name is empty, set it from the Google user's
  `displayName` (do not overwrite an existing name).
- **sync.js**: add `sendPasswordReset(email)` —
  `sendPasswordResetEmail(auth, email)`. Show the same confirmation
  regardless of whether the account exists (do not leak account
  existence) — the component shows "Reset link sent — check your
  inbox." on success, and a plain error only for an invalid email
  address.
- **ProgressVisualizer.jsx**: add a "Sign in with Google" button
  above the email sign-in form (only when Firebase is ready and the
  user is signed out). Add a "Forgot password?" link under the
  sign-in form → an email input + "Send reset link" button →
  confirmation text. Keep the existing account section (sign up /
  sign in, sync status, MFA section) unchanged apart from these
  additions. Dark theme applies — use the CSS variables.
- No console change is needed in code for the consent screen — it is
  Babu's console step (section 1).

## 9. Frontend — faster first load (code splitting, same agent as section 8)

The initial JS bundle is ~1,015 kB (273 kB gzip) because the Firebase
SDK and the `qrcode` library are in the initial chunk. Split them
out with dynamic `import()` so they load on demand:

- **firebase.js**: stop statically importing `firebase/app`,
  `firebase/auth`, `firebase/firestore`. Export async getters —
  e.g. `getAuthAsync()` and `getDbAsync()` — that dynamically import
  the Firebase modules on first call, initialise the app once, cache
  the result, and return it. `firebaseReady` stays a synchronous
  boolean (based on the env vars, as now). The `auth` / `db`
  synchronous exports go away — every consumer must use the async
  getters.
- **sync.js**: every function that uses `auth` / `db` awaits the
  async getters. The functions are already async, so this is
  contained — but check EVERY call site (including `onAuthChange`,
  `loadCloudProgress`, `saveCloudProgress`, and the new Google
  sign-in). Keep every existing function signature the same.
- **progress.js**: `initCloudSync()` runs at module level and
  subscribes to auth changes — it must handle the async Firebase
  load (call the async getter, subscribe when ready; if Firebase is
  not configured, it is a no-op as now).
- **ProgressVisualizer.jsx**: the `qrcode` import becomes a dynamic
  import inside the MFA enrollment handler
  (`const QRCode = (await import('qrcode')).default;`) — it loads
  only when the user starts MFA enrollment.
- **Goal (measure it, do not guess):** the initial JS chunk under
  500 kB (no Vite chunk-size warning for the initial load) and the
  build output showing separate chunks for Firebase and for
  `qrcode`. Report the before/after bundle sizes from the build
  output.
- **Risk (flag it):** this refactor touches the auth wiring. Verify
  by reading that every call site awaits the async getters and that
  no static Firebase import remains in the initial chunk path.

## 10. Endpoints summary (new)

- `POST /api/graph/bellmanford` `{graphEdges, start}`
- `POST /api/graph/toposort` `{graphEdges}`
- `POST /api/dp/knapsack` `{weights, values, capacity}`
- Google sign-in + password reset + code splitting are client-side;
  no new endpoints for them.

## 11. Tests & verification

- `Phase10ServiceTest` (JUnit 5): Bellman-Ford example → final
  distances `[0, 4, 1, 9, 11, 14]`; Bellman-Ford negative-cycle
  example → last message contains "Negative cycle"; toposort DAG →
  order `[0, 1, 2, 3, 4, 5]`; toposort cycle → last message
  contains "Cycle"; knapsack `[2,3,4,5]` / `[3,4,5,6]` / capacity 5
  → max 7, items `[0, 1]`; knapsack `[1,2]` / `[10,20]` / capacity 2
  → max 20, items `[1]`; knapsack capacity 0 → max 0, items `[]`.
- Controller validation (400s) — verify by reading the controller
  code, same as Phases 3–4, 7 and 8.
- Frontend: `npm run build` must pass. Report the module count and
  the bundle sizes (initial chunk vs the Firebase and `qrcode`
  chunks). Validate `quizData.js` and `lectureData.js` with a script
  (all modules present, every question has 4 options + a valid
  answer index + an explanation, every slide has a non-empty title +
  body). Verify the code-splitting by reading the build output (the
  initial chunk under 500 kB, separate Firebase / `qrcode` chunks)
  and by grepping that no static `firebase/*` or `qrcode` import
  remains in the initial chunk path.
- Backend: brace balance on all Java files; simulate the
  Bellman-Ford / toposort / knapsack test cases in Python (same loops
  as the Java code) and confirm the final states — especially that
  Bellman-Ford gives `[0, 4, 1, 9, 11, 14]` (node 2 = 1, not 2),
  that toposort gives `[0, 1, 2, 3, 4, 5]` with the specified
  tie-breaking, and that knapsack gives max 7 with items `[0, 1]`.
- No JDK/Docker on this machine — `mvn verify` on Babu's machine /
  CI is the real confirmation.
- **NOT verified live — state plainly:** Google sign-in cannot be
  tested without Babu enabling the Google provider in the console
  (and publishing the consent screen); password reset cannot be
  tested without a real Firebase project email send; the lazy-loaded
  chunks cannot be click-tested in a browser here. Build + bundle
  output + code reading are the verification.

## 12. Out of scope for Phase 10

- SMS as a second factor (needs Blaze + per-SMS charges; Babu did
  not pick it).
- Forcing MFA for every user (no simple console toggle).
- Any other new algorithm (A* search, segment tree, Red-Black tree)
  — candidates for a later phase.
- Moving the backend off Render or the frontend off Vercel.

## 13. Corrections found during the build (2026-11-19)

1. **TraceRequest.capacity already existed** (the hash-table size
   field, with getter/setter). Section 5's instruction to add it was
   wrong; the knapsack endpoint reuses the same `capacity` field.
   Only `weights` and `values` were added.
2. **Negative-weight parsing did NOT work — the section 6
   assumption was wrong.** The doc said the existing `0-1:-3`
   format "already parses a leading minus". It did not: the old
   `split('-')` parser split `0-1:-3` into three parts and silently
   dropped the entry, so Bellman-Ford could never receive a negative
   edge. Replaced with a regex parser
   (`/^(\d+)-(\d+)(?::(-?\d+))?$/`) and verified headlessly on
   negative, plain, spaced, malformed and zero-weight inputs.
3. **Latent Phase 7 bug found while editing GraphVisualizer:**
   `tableTitle` read `startNode` inside a template literal *before*
   `const startNode` was declared — a temporal-dead-zone
   `ReferenceError`. On first render the action is BFS so the else
   branch runs, but the moment a user clicks **Dijkstra** the
   component throws and the app unmounts. This was in the shipped
   Phase 7/8/9 code. Fixed by moving the `startNode` declaration
   above `tableTitle`. This repairs Dijkstra on the live site, not
   just the new buttons.
4. **Code splitting side effects (accounts agent):**
   `listTotpFactors()` is now async (it was synchronous) — the
   component awaits it at all three call sites. The Google
   display-name adoption lives in the component, not in `sync.js`
   (putting it in `sync.js` would have required `sync.js` to import
   `progress.js`, and `progress.js` already imports `sync.js` — a
   circular import). Total bundle size grew to ~1,265 kB across all
   chunks (+25%) because namespace dynamic imports tree-shake less
   than the previous static named imports — the cost is only paid on
   demand, never on first load. The Vite >500 kB warning still
   appears, but now for the lazy Firestore chunk (695.91 kB), not
   the initial load.

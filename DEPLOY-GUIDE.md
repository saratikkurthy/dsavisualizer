# DSA Visualizer — Step-by-Step Guide
## GitHub → VS Code → Free Deployment (Vercel + Render)

This guide takes you from the zip file to a live website anyone can open.
There are 5 parts. Do them in order.

> **Two-repo setup (what you have):** this guide was written for one
> repo with `backend/` and `frontend/` folders, but you created **two
> repos** — one for the backend, one for the frontend. That works fine
> and is a clean setup. The only difference is **Root Directory**:
> - **Backend repo:** the Spring Boot files (`pom.xml`, `src/`,
>   `Dockerfile`) sit at the **repo root**, so leave **Root Directory
>   blank** on Render. Do NOT set it to `backend`.
> - **Frontend repo:** the React files (`package.json`, `src/`,
>   `vite.config.js`) sit at the **repo root**, so leave **Root
>   Directory blank** on Vercel. Do NOT set it to `frontend`.
> - The `render.yaml` Blueprint in the zip is written for the
>   one-repo layout; with two repos, use the dashboard flow in
>   Part C (it does not need render.yaml).
> Everywhere below that says Root Directory `backend` or `frontend`,
> read it as "blank" for your two-repo setup.

- **Part A** — Put the code on GitHub
- **Part B** — Open it in VS Code and run it on your machine
- **Part C** — Deploy the backend (Spring Boot) on Render — free
- **Part D** — Deploy the frontend (React) on Vercel — free
- **Part E** — Connect them and test the live site

**What you need before you start**
- A GitHub account (you have one: github.com/saratikkurthy)
- Git installed on Windows — check by opening PowerShell and running
  `git --version`. If it says "not recognized", install it from
  https://git-scm.com/download/win (accept all defaults).
- VS Code — https://code.visualstudio.com (the free editor; this guide
  means VS Code when it says "Visual Studio").
- Java 17 and Maven (for the backend). You already run Spring Boot, so
  you have these. Check with `java -version` and `mvn -version`.
- Node.js 18+ (for the frontend). Check with `node -version`.
  If missing, install from https://nodejs.org (LTS version).

---

## Part A — Put the code on GitHub

### A1. Unzip the project
Unzip `dsa-visualizer.zip` to a folder, for example:
`C:\Users\ishar\MyAIProject\MyAIProject\dsa-visualizer`

Inside you should see: `backend/`, `frontend/`, `DESIGN.md`,
`README.md`, `render.yaml`, `vercel.json`, `.github/`.

### A2. Create an empty repo on GitHub
1. Go to https://github.com and sign in.
2. Click the **+** (top right) → **New repository**.
3. Repository name: `dsa-visualizer`
4. Description: `Visualgo-style data structures visualizer — Spring Boot + React`
5. Choose **Public**.
6. **Do NOT** tick "Add a README file" (we already have one).
7. Click **Create repository**.
8. GitHub shows you a page with a URL. Copy it. It looks like:
   `https://github.com/saratikkurthy/dsa-visualizer.git`

### A3. Push the code from PowerShell
Open PowerShell, go to the project folder, and run these commands
one by one. (Replace the repo URL with yours if your username differs.)

```powershell
cd C:\Users\ishar\MyAIProject\MyAIProject\dsa-visualizer
git init
git add .
git commit -m "Initial commit: DSA Visualizer Phase 1 (sorting + search)"
git branch -M main
git remote add origin https://github.com/saratikkurthy/dsa-visualizer.git
git push -u origin main
```

**If GitHub asks for a password and rejects it:** GitHub no longer
accepts your account password for pushing. It wants a Personal Access
Token (PAT). Do this once:
1. GitHub → your profile photo → **Settings** → **Developer settings**
   → **Personal access tokens** → **Tokens (classic)** → **Generate new
   token (classic)**.
2. Give it a name like `dsa-visualizer-push`, tick the `repo` scope,
   click **Generate token**, and copy the token (you only see it once).
3. When `git push` asks for a password, paste the **token**, not your
   GitHub password. Windows will remember it.

**If `git push` fails with "git-receive-pack not permitted":** that is
the same push-auth issue you hit with the DSA repo. The token above
fixes it. If it still fails, push from GitHub Desktop instead
(https://desktop.github.com — sign in, File → Add Local Repository,
then Publish).

### A4. Confirm it worked
Refresh your repo page on GitHub. You should see `backend/`,
`frontend/`, `README.md` and the rest. If you see them, Part A is done.

---

## Part B — Open it in VS Code and run it locally

### B1. Open the project
1. Open VS Code.
2. **File → Open Folder** → select the `dsa-visualizer` folder.
3. VS Code shows the whole project in the left sidebar.

### B2. Install the extensions (one time)
Click the Extensions icon (left sidebar, four squares) and install:
- **Extension Pack for Java** (by Microsoft) — Java language support.
- **Spring Boot Extension Pack** (by VMware) — run/debug Spring Boot.
- **ESLint** (by Microsoft) — JavaScript/React linting.
- **Prettier — Code formatter** (by Prettier) — optional, formats code.

### B3. Run the backend (Spring Boot)
1. In VS Code, open the integrated terminal:
   **Terminal → New Terminal** (or Ctrl + `).
2. Run:
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
3. Wait for `Started DsavisualizerApplication` in the output.
4. Open http://localhost:8080/api/health in your browser.
   You should see: `{"status":"ok"}`.
5. Leave this terminal running. (Stop it later with Ctrl + C.)

### B4. Run the frontend (React)
1. Open a **second** terminal in VS Code (click the **+** in the
   terminal panel) — keep the backend terminal running.
2. Run:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
   The first `npm install` downloads React and Vite (one time, ~1 min).
3. VS Code prints a local address, usually http://localhost:5173.
   Open it in your browser.
4. You should see "DSA Visualizer" with Sorting and Search tabs.
   Enter numbers, click **Visualize**, and the bars should animate.
   If the bars do not appear, check the backend terminal is still
   running and the browser console (F12) for errors.

### B5. How the two parts talk to each other
The React app calls the backend at the address in
`frontend/.env.example` (`VITE_API_URL=http://localhost:8080`).
For local runs you do not need to change anything.

**You now have the app working on your machine. Next: the internet.**

---

## Part C — Deploy the backend on Render (free)

Do the backend first, because the frontend needs the backend's URL.

### C1. Create a Render account
1. Go to https://render.com and click **Get Started**.
2. Sign up with **GitHub** (click "GitHub", authorize Render).
   This lets Render read your repo.

### C2. Create the Web Service
1. Render dashboard → **New +** → **Web Service**.
2. Choose **Build and deploy from a Git repository** → **Next**.
3. Find `saratikkurthy/dsa-visualizer` in the list and click **Connect**.
   (If it is not listed, click "Configure account" and grant access.)
4. Fill in the settings:
   - **Name:** `dsa-visualizer-backend`
   - **Region:** pick the closest (e.g. Oregon / Ohio — any is fine).
   - **Branch:** `main`
   - **Root Directory:** `backend`
   - **Language:** `Docker` — see the note below; this is the
     important one.
   - **Plan:** **Free**
   - Leave **Dockerfile Path** as the default (`Dockerfile`).
     Render finds `backend/Dockerfile` because Root Directory is
     `backend`.
   - There is **no Build Command / Start Command** to fill in when
     Language is Docker — the Dockerfile does the build (Maven) and
     the start (`java -jar app.jar`) itself.
5. Click **Create Web Service**.
6. Render starts building. It takes 3–5 minutes the first time
   (it downloads the Maven image and your dependencies). Watch the logs.
7. When it says "Your service is live", Render gives you a URL like:
   `https://dsa-visualizer-backend.onrender.com`
   **Copy this URL — you need it in Part D.**

> **Why Docker, not Java?** Render has **no native Java runtime**.
> Its Language dropdown is Docker, Elixir, Go, Node, Python 3, Ruby,
> Rust — Java is not in it. Render's own docs say to use Docker for
> JVM languages (Java, Kotlin, Scala). The repo already has
> `backend/Dockerfile`: it builds the JAR with Maven (JDK 17) and runs
> it on a small JRE 17 image, with the heap capped at 384 MB so it
> fits Render's free-tier 512 MB RAM. You do not need Docker installed
> on your machine — Render builds the image on its own servers.

### C3. Test the live backend
Open `https://dsa-visualizer-backend.onrender.com/api/health` in your
browser. You should see `{"status":"ok"}`.

**Free-tier note:** Render's free plan puts the service to sleep after
~15 minutes with no traffic. The first request after that takes
30–60 seconds to wake up. That is normal and free. It does not affect
the frontend (the page loads; only the first "Visualize" click waits).

### C4. (Optional) Use the Blueprint instead
If you prefer, the repo has a `render.yaml`. In Render:
**New + → Blueprint**, pick the repo, and Render creates the service
from that file. Same result as C2.

---

## Part D — Deploy the frontend on Vercel (free)

### D1. Create a Vercel account
1. Go to https://vercel.com and click **Sign Up**.
2. Choose **Continue with GitHub** and authorize Vercel.

### D2. Import the project
1. Vercel dashboard → **Add New… → Project**.
2. Find `saratikkurthy/dsa-visualizer` and click **Import**.
3. Configure the project:
   - **Framework Preset:** Vite (Vercel usually detects this itself).
   - **Root Directory:** click **Edit** and select `frontend`.
     (This is the important step — without it Vercel looks at the
     repo root and fails.)
   - **Build Command:** `npm run build` (default).
   - **Output Directory:** `dist` (default).
4. **Before you click Deploy**, add the backend URL:
   - Expand **Environment Variables**.
   - Name: `VITE_API_URL`
   - Value: your Render URL from Part C, e.g.
     `https://dsa-visualizer-backend.onrender.com`
     (no trailing slash).
5. Click **Deploy**. It takes 1–2 minutes.
6. Vercel gives you a URL like:
   `https://dsa-visualizer.vercel.app`
   **Copy this URL — you need it in Part E.**

### D3. If Vercel asks for a team / payment
Choose the **Hobby** (free) plan. Vercel's free plan covers this app
fully. You do not need to add a card.

---

## Part E — Connect them and test the live site

### E1. Tell the backend to accept the frontend
The backend currently allows any origin (`@CrossOrigin(origins="*")`),
so it already works. For a cleaner setup, restrict it to your Vercel
domain:
1. In Render, open your `dsa-visualizer-backend` service →
   **Environment** (left sidebar).
2. Add a variable:
   - Key: `CORS_ALLOWED_ORIGINS`
   - Value: your Vercel URL, e.g. `https://dsa-visualizer.vercel.app`
3. Click **Save Changes**. Render redeploys automatically (~1 min).

(Note: the current code reads CORS from the controller annotation, not
from this variable, so this step is a placeholder for the next
version. The site works with or without it — do not block on it.)

### E2. Test the live site
1. Open your Vercel URL (e.g. `https://dsa-visualizer.vercel.app`).
2. On the **Sorting** tab, leave the default numbers and click
   **Visualize**. The bars should animate.
   - If nothing happens for ~30–60 seconds on the first try, that is
     the Render free tier waking up. Wait and click Visualize again.
3. Switch to the **Search** tab, run a Linear Search and a Binary
   Search. The boxes should highlight as it probes.
4. Open the site on your phone (same URL). It works anywhere on the
   internet — that is the point.

### E3. If something does not work
| Symptom | Likely cause | Fix |
|---|---|---|
| Bars never appear, browser console shows a failed fetch to `localhost:8080` | `VITE_API_URL` was not set on Vercel (it built with the localhost default) | Vercel → project → Settings → Environment Variables → add `VITE_API_URL` = Render URL → redeploy (Deployments → ⋯ → Redeploy) |
| Fetch fails with a CORS error | Backend is not accepting the Vercel origin | The backend allows `*` by default, so this should not happen. Check the Render URL is correct in `VITE_API_URL` (no trailing slash) |
| `https://...onrender.com/api/health` shows an error or never loads | Render build failed | Render → service → Logs. Look for the error. Common cause: a wrong Build/Start command — compare with Part C2 |
| Vercel build fails | Root Directory is not `frontend` | Vercel → project → Settings → General → Root Directory → `frontend`, then redeploy |

### E4. How updates work from now on (CI/CD)
From now on, the flow is:
1. Change code in VS Code (backend or frontend).
2. Commit and push to GitHub:
   ```powershell
   git add .
   git commit -m "Describe your change"
   git push
   ```
3. GitHub Actions runs the checks (backend `mvn verify`, frontend
   `npm run build`). You can watch it under the repo's **Actions** tab.
4. If the checks pass, Render rebuilds the backend and Vercel
   rebuilds the frontend automatically. No manual deploy step.
   (If a check fails, fix the error and push again — the platforms
   keep serving the last good version.)

---

## Quick reference (all the URLs and commands)

| What | Value |
|---|---|
| GitHub repo | https://github.com/saratikkurthy/dsa-visualizer |
| Local backend | http://localhost:8080 (health: /api/health) |
| Local frontend | http://localhost:5173 |
| Render backend | https://dsa-visualizer-backend.onrender.com (yours may differ) |
| Vercel frontend | https://dsa-visualizer.vercel.app (yours may differ) |
| Backend run | `cd backend` then `mvn spring-boot:run` |
| Frontend run | `cd frontend` then `npm install` then `npm run dev` |
| Push a change | `git add .` → `git commit -m "..."` → `git push` |

**Cost: $0.** GitHub (public repo), Vercel Hobby, Render Free.
The only limitation is Render sleeping after 15 minutes idle.

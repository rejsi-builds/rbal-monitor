# RBAL DevOps Control Tower

A DevOps monitoring dashboard for a bank environment. It shows the health of nine simulated banking microservices across DEV, UAT and PROD, and is delivered with Docker and a GitHub Actions CI/CD pipeline.

Live dashboard (GitHub Pages): https://rejsi-builds.github.io/rbal-monitor/

## Features

- Live health monitoring of 9 services (ATM Network API, Mobile Banking Backend, Card Processor) in 3 environments
- Cluster state: STABLE, DEGRADED or CRITICAL
- Filter by environment (ALL, DEV, UAT, PROD)
- Service details: CPU, RAM, uptime, response time and logs
- Simulated crashes and recoveries, so the dashboard reacts like a real system
- Release History table that reads releases from the GitHub API

## Architecture

| Component | Description |
|-----------|-------------|
| `MockServices.java` | Starts 9 mock services (ports 8082-8090) with a `/health` endpoint. They crash and recover at random. |
| `MonitorServer.java` | Checks every service and serves the dashboard and `/api/status` on port 8081. |
| `web/` | Dashboard frontend (HTML, CSS, JavaScript). |
| `Dockerfile` | Packages all of the above into one image. |
| `.github/workflows/ci-cd.yml` | GitHub Actions pipeline. |

## Run locally with Docker

```
docker build -t rbal-monitor .
docker run -p 8081:8081 rbal-monitor
```

Open http://localhost:8081

## CI/CD pipeline

The pipeline runs on every push to `main` (or manually), on a self-hosted runner on a local PC.

1. Creates a unique version tag: `1.0.<run number>-<commit hash>`
2. Builds the Docker image
3. Starts a test container and checks that the dashboard and `/api/status` answer
4. Saves the image as a `.tar` file and attaches it to a GitHub release
5. Replaces the running container on the local PC with the new image
6. Publishes the `web/` folder to GitHub Pages

## Project Status

- Docker image: built and tested locally.
- CI/CD: working on a self-hosted runner (GitHub Actions).
- Start the runner with Git's bash first in the PATH: `$env:PATH = "C:\Program Files\Git\bin;" + $env:PATH` then `.\run.cmd`
- GitHub Pages shows the dashboard. Live data needs the local container running.

## Technologies Used

Java, HTML, CSS, JavaScript, Docker, Git, GitHub Actions, GitHub Pages.

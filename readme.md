# Real-Time Chat Application

A real-time group chat platform where users register, create rooms, and exchange messages instantly. All messages are automatically scanned for toxic or inappropriate content by an asynchronous ML moderation pipeline — flagged messages are removed after delivery without interrupting the chat experience.

## Architecture

The system is composed of six services:

| Service | Technology | Responsibility |
|---|---|---|
| API Gateway | Java / Spring Cloud Gateway | JWT validation, request routing |
| User Service | Java / Spring Boot | Registration, login, JWT issuance |
| Room Service | Java / Spring Boot | Room creation, listing, membership |
| Chat Service | Java / Spring Boot | WebSocket connections, message history |
| Moderation Service | Python / FastAPI | ML content moderation via Kafka |
| Frontend | React / Nginx | Browser UI |

Messages flow through Kafka: the Chat Service publishes every message, the Moderation Service consumes and scores it, then publishes a verdict back. Rejected messages are removed from all connected clients automatically.

## Docker Compose

### Environment Setup

Create a `.env` file at the monorepo root:

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-long-random-secret-here
```

### Run

```bash
docker compose up --build
```

The `--build` flag rebuilds all images before starting. Once everything is up:

| URL | Description |
|---|---|
| `http://localhost:80` | Frontend UI |
| `http://localhost:8080` | API Gateway |
| `http://localhost:3000` | Grafana |

### Stop

Shut down containers but keep all data:

```bash
docker compose down
```

Shut down and delete all data (fresh start next time):

```bash
docker compose down -v
```

## Kubernetes (Minikube)

### Environment Setup

Generate the secrets file by running the helper script from the monorepo root:

```bash
./k8s/secrets/generate-secrets.sh
```

It will prompt for `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and write a ready-to-apply `k8s/secrets/app-secrets.yaml`.

### Run

Start Minikube if it is not already running:

```bash
minikube start
```

Then deploy the full stack:

```bash
./k8s/deploy.sh
```

### Teardown

Delete all Kubernetes resources but keep Minikube running:

```bash
./k8s/teardown.sh
```

Stop and delete Minikube along with all data:

```bash
minikube delete
```

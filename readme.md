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

## Environment Setup

Create a `.env` file at the monorepo root:

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-long-random-secret-here
```

## Run

Start the entire system with a single command from the monorepo root:

```bash
docker compose up --build
```

The `--build` flag rebuilds all images before starting. Once everything is up:

| URL | Description |
|---|---|
| `http://localhost:80` | Frontend UI |
| `http://localhost:8080` | API Gateway |

## Stop

Shut down and **delete all data** (fresh start next time):

```bash
docker compose down -v
```

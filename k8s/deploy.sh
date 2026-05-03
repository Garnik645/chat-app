#!/bin/bash
# Deploy the full chat-app stack to Minikube.
# Run from the monorepo root: bash k8s/deploy.sh

set -e

echo "==> Checking minikube is running..."
minikube status | grep -q "Running" || { echo "ERROR: minikube is not running. Start it with: minikube start"; exit 1; }

echo "==> Pointing Docker to Minikube's daemon (so it can see local images)..."
eval $(minikube docker-env)

echo "==> Building all Docker images inside Minikube..."
docker build -t chat-app/api-gateway:latest        ./api-gateway
docker build -t chat-app/user-service:latest       ./user-service
docker build -t chat-app/room-service:latest       ./room-service
docker build -t chat-app/chat-service:latest       ./chat-service
docker build -t chat-app/moderation-service:latest ./moderation-service
docker build -t chat-app/frontend:latest           ./frontend

echo "==> Applying namespace..."
kubectl apply -f k8s/namespace.yaml

echo "==> Applying secrets (make sure you have filled in k8s/secrets/app-secrets.yaml)..."
kubectl apply -f k8s/secrets/app-secrets.yaml

echo "==> Applying configmaps..."
kubectl apply -f k8s/configmaps/

echo "==> Applying infrastructure (databases, kafka, prometheus, grafana)..."
kubectl apply -f k8s/infrastructure/zookeeper/
kubectl apply -f k8s/infrastructure/kafka/
kubectl apply -f k8s/infrastructure/user-db/
kubectl apply -f k8s/infrastructure/rooms-db/
kubectl apply -f k8s/infrastructure/chat-db/
kubectl apply -f k8s/infrastructure/prometheus/
kubectl apply -f k8s/infrastructure/grafana/

echo "==> Waiting for databases to be ready..."
kubectl rollout status deployment/user-db  -n chat-app --timeout=120s
kubectl rollout status deployment/rooms-db -n chat-app --timeout=120s
kubectl rollout status deployment/chat-db  -n chat-app --timeout=120s

echo "==> Waiting for Kafka to be ready..."
kubectl rollout status deployment/zookeeper -n chat-app --timeout=120s
kubectl rollout status deployment/kafka     -n chat-app --timeout=120s

echo "==> Applying application services..."
kubectl apply -f k8s/services/user-service/
kubectl apply -f k8s/services/room-service/
kubectl apply -f k8s/services/chat-service/
kubectl apply -f k8s/services/moderation-service/
kubectl apply -f k8s/services/api-gateway/
kubectl apply -f k8s/services/frontend/

echo "==> Waiting for application services to be ready..."
kubectl rollout status deployment/user-service       -n chat-app --timeout=180s
kubectl rollout status deployment/room-service       -n chat-app --timeout=180s
kubectl rollout status deployment/chat-service       -n chat-app --timeout=180s
kubectl rollout status deployment/moderation-service -n chat-app --timeout=600s
kubectl rollout status deployment/api-gateway        -n chat-app --timeout=180s
kubectl rollout status deployment/frontend           -n chat-app --timeout=120s

echo ""
echo "==> All services are up! Getting URLs..."
echo ""
echo "Frontend:            $(minikube service frontend         -n chat-app --url)"
echo "API Gateway:         $(minikube service api-gateway      -n chat-app --url)"
echo "Grafana:             $(minikube service grafana          -n chat-app --url)"
echo "Moderation Service:  $(minikube service moderation-service -n chat-app --url)"

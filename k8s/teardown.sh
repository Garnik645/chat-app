#!/bin/bash
# Tear down the full stack from Minikube.
# Run from the monorepo root: bash k8s/teardown.sh

set -e

echo "==> Deleting all resources in namespace chat-app..."
kubectl delete namespace chat-app

echo "==> Done. All resources deleted."

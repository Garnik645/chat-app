#!/bin/bash
# Prompts for real values and writes a ready-to-apply secrets file.
# Run from the repo root: bash k8s/secrets/generate-secrets.sh

set -e

read -rsp "DB_USERNAME: " DB_USERNAME; echo
read -rsp "DB_PASSWORD: " DB_PASSWORD; echo
read -rsp "JWT_SECRET: "  JWT_SECRET;  echo

cat > k8s/secrets/app-secrets.yaml <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: chat-app
type: Opaque
data:
  DB_USERNAME:         $(echo -n "$DB_USERNAME"         | base64)
  DB_PASSWORD:         $(echo -n "$DB_PASSWORD"         | base64)
  JWT_SECRET:          $(echo -n "$JWT_SECRET"          | base64)
EOF

echo "Written to k8s/secrets/app-secrets.yaml"

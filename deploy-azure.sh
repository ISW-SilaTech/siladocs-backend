#!/bin/bash

RESOURCE_GROUP="siladocs-rg"
REGISTRY_NAME="siladocsregistry"

echo "📦 Desplegando PostgreSQL..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-postgres \
  --image postgres:15 \
  --cpu 1 --memory 1 \
  --os-type Linux \
  --environment-variables POSTGRES_DB=siladocs POSTGRES_USER=siladocs POSTGRES_PASSWORD=siladocs \
  --ports 5432 \
  --ip-address public

echo "📦 Desplegando MinIO..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-minio \
  --image minio/minio:latest \
  --cpu 1 --memory 1 \
  --os-type Linux \
  --environment-variables MINIO_ROOT_USER=minioadmin MINIO_ROOT_PASSWORD=minioadmin \
  --ports 9000 9001 \
  --ip-address public \
  --command-line "minio server /data --console-address :9001"

echo "📦 Desplegando Fabric Middleware..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-fabric-middleware \
  --image ${REGISTRY_NAME}.azurecr.io/siladocs-fabric-middleware:latest \
  --registry-login-server ${REGISTRY_NAME}.azurecr.io \
  --cpu 1 --memory 1 \
  --os-type Linux \
  --ports 8000 \
  --ip-address public

echo "📦 Desplegando Backend..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend \
  --image ${REGISTRY_NAME}.azurecr.io/siladocs-backend:latest \
  --registry-login-server ${REGISTRY_NAME}.azurecr.io \
  --cpu 2 --memory 2 \
  --os-type Linux \
  --environment-variables SPRING_PROFILES_ACTIVE=fabric FABRIC_API_URL=http://siladocs-fabric-middleware:8000 \
  --ports 8080 \
  --ip-address public

echo "✅ Desplegados"

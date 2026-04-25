#!/bin/bash

# Variables
RESOURCE_GROUP="siladocs-rg"
REGISTRY_NAME="siladocsregistry"
LOCATION="eastus"

echo "🚀 Obteniendo credenciales de ACR..."
ACR_PASSWORD=$(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query "passwords[0].value" -o tsv)
ACR_USERNAME=$(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query "username" -o tsv)
REGISTRY_URL="${REGISTRY_NAME}.azurecr.io"

echo "📦 Desplegando PostgreSQL..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-postgres \
  --image postgres:15 \
  --cpu 1 --memory 1 \
  --environment-variables POSTGRES_DB=siladocs POSTGRES_USER=siladocs POSTGRES_PASSWORD=siladocs \
  --ports 5432 \
  --ip-address public \
  --protocol TCP

echo "📦 Desplegando MinIO..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-minio \
  --image minio/minio:latest \
  --cpu 1 --memory 1 \
  --environment-variables MINIO_ROOT_USER=minioadmin MINIO_ROOT_PASSWORD=minioadmin \
  --ports 9000 9001 \
  --ip-address public \
  --command-line "minio server /data --console-address :9001" \
  --protocol TCP

echo "📦 Desplegando Fabric Middleware..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-fabric-middleware \
  --image $REGISTRY_URL/siladocs-fabric-middleware:latest \
  --registry-login-server $REGISTRY_URL \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --cpu 1 --memory 1 \
  --ports 8000 \
  --ip-address public \
  --protocol TCP

echo "📦 Desplegando Backend Spring Boot..."
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend \
  --image $REGISTRY_URL/siladocs-backend:latest \
  --registry-login-server $REGISTRY_URL \
  --registry-username $ACR_USERNAME \
  --registry-password $ACR_PASSWORD \
  --cpu 2 --memory 2 \
  --environment-variables SPRING_PROFILES_ACTIVE=fabric FABRIC_API_URL=http://siladocs-fabric-middleware:8000 \
  --ports 8080 \
  --ip-address public \
  --protocol TCP

echo "✅ DESPLIEGUE COMPLETADO"
echo ""
echo "📍 IPs Públicas:"
echo "Backend: $(az container show --resource-group $RESOURCE_GROUP --name siladocs-backend --query ipAddress.ip -o tsv):8080"
echo "Fabric Middleware: $(az container show --resource-group $RESOURCE_GROUP --name siladocs-fabric-middleware --query ipAddress.ip -o tsv):8000"
echo "MinIO: $(az container show --resource-group $RESOURCE_GROUP --name siladocs-minio --query ipAddress.ip -o tsv):9001"
echo "PostgreSQL: $(az container show --resource-group $RESOURCE_GROUP --name siladocs-postgres --query ipAddress.ip -o tsv):5432"

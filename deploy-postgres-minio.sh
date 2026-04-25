#!/bin/bash

# ================================================================
# Deploy PostgreSQL and MinIO to Azure ACI
# Usage: bash deploy-postgres-minio.sh
# ================================================================

set -e

# Configuration
RESOURCE_GROUP="siladocs-rg"
REGISTRY_NAME="siladocsregistry"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Deploying PostgreSQL and MinIO to Azure ACI...${NC}"

# ================================================================
# 1. Deploy PostgreSQL
# ================================================================
echo -e "${BLUE}1️⃣ Deploying PostgreSQL...${NC}"

az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-postgres \
  --image postgres:15 \
  --cpu 1 \
  --memory 1.5 \
  --os-type Linux \
  --environment-variables \
    POSTGRES_DB=siladocs \
    POSTGRES_USER=siladocs \
    POSTGRES_PASSWORD=siladocs \
  --ports 5432 \
  --ip-address public \
  --restart-policy OnFailure

echo -e "${GREEN}✅ PostgreSQL container created${NC}"

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 30

# Get PostgreSQL IP
POSTGRES_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-postgres \
  --query ipAddress.ip \
  --output tsv)

echo -e "${GREEN}PostgreSQL IP: $POSTGRES_IP:5432${NC}"

# ================================================================
# 2. Deploy MinIO
# ================================================================
echo -e "${BLUE}2️⃣ Deploying MinIO...${NC}"

az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-minio \
  --image minio/minio:latest \
  --cpu 1 \
  --memory 1.5 \
  --os-type Linux \
  --environment-variables \
    MINIO_ROOT_USER=minioadmin \
    MINIO_ROOT_PASSWORD=minioadmin \
  --ports 9000 9001 \
  --ip-address public \
  --restart-policy OnFailure \
  --command-line "minio server /data --console-address :9001"

echo -e "${GREEN}✅ MinIO container created${NC}"

# Wait for MinIO to be ready
echo "⏳ Waiting for MinIO to be ready..."
sleep 20

# Get MinIO IP
MINIO_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-minio \
  --query ipAddress.ip \
  --output tsv)

echo -e "${GREEN}MinIO API: $MINIO_IP:9000${NC}"
echo -e "${GREEN}MinIO Console: http://$MINIO_IP:9001${NC}"

# ================================================================
# 3. Create a new Backend container with correct environment variables
# ================================================================
echo -e "${BLUE}3️⃣ Updating Backend with PostgreSQL and MinIO configuration...${NC}"

az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend-v2 \
  --image $REGISTRY_NAME.azurecr.io/siladocs-backend:latest \
  --cpu 1 \
  --memory 2 \
  --os-type Linux \
  --environment-variables \
    SPRING_PROFILES_ACTIVE=fabric \
    SPRING_DATASOURCE_URL="jdbc:postgresql://$POSTGRES_IP:5432/siladocs" \
    SPRING_DATASOURCE_USERNAME=siladocs \
    SPRING_DATASOURCE_PASSWORD=siladocs \
    SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
    SILADOCS_STORAGE_ENDPOINT="http://$MINIO_IP:9000" \
    SILADOCS_STORAGE_ACCESS_KEY=minioadmin \
    SILADOCS_STORAGE_SECRET_KEY=minioadmin \
    SILADOCS_STORAGE_BUCKET=siladocs \
    MINIO_ENDPOINT="http://$MINIO_IP:9000" \
    MINIO_ACCESS_KEY=minioadmin \
    MINIO_SECRET_KEY=minioadmin \
    MINIO_BUCKET_NAME=syllabi \
    FABRIC_API_URL=http://135.234.224.116:8080 \
  --ports 8080 \
  --ip-address public \
  --restart-policy OnFailure \
  --registry-login-server $REGISTRY_NAME.azurecr.io \
  --registry-username $(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query username --output tsv) \
  --registry-password $(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query passwords[0].value --output tsv)

echo -e "${GREEN}✅ Backend container updated${NC}"

# ================================================================
# 4. Summary
# ================================================================
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo "📊 Service IPs:"
echo "   PostgreSQL:      $POSTGRES_IP:5432"
echo "   MinIO API:       $MINIO_IP:9000"
echo "   MinIO Console:   http://$MINIO_IP:9001"
echo "   Fabric Middleware: 135.234.224.116:8080"
echo ""
echo "🔐 Credentials:"
echo "   PostgreSQL User:   siladocs"
echo "   PostgreSQL Pass:   siladocs"
echo "   MinIO User:        minioadmin"
echo "   MinIO Pass:        minioadmin"
echo ""
echo "⏳ Waiting for Backend to start (check status with 'az container show --resource-group $RESOURCE_GROUP --name siladocs-backend-v2 --query instanceView.state')"
echo ""

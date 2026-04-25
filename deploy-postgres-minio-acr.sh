#!/bin/bash

# ================================================================
# Deploy PostgreSQL and MinIO to Azure ACI (Using ACR)
# Usage: bash deploy-postgres-minio-acr.sh
# ================================================================

set -e

# Configuration
RESOURCE_GROUP="siladocs-rg"
REGISTRY_NAME="siladocsregistry"
REGISTRY_URL="siladocsregistry.azurecr.io"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Deploying PostgreSQL and MinIO to Azure ACI...${NC}"

# Get registry credentials
echo -e "${BLUE}📝 Getting ACR credentials...${NC}"
REGISTRY_USERNAME=$(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query username --output tsv)
REGISTRY_PASSWORD=$(az acr credential show --resource-group $RESOURCE_GROUP --name $REGISTRY_NAME --query passwords[0].value --output tsv)

if [ -z "$REGISTRY_USERNAME" ] || [ -z "$REGISTRY_PASSWORD" ]; then
    echo -e "${YELLOW}⚠️ Could not get ACR credentials. Using direct Docker images...${NC}"
    USE_DOCKER_HUB=true
else
    echo -e "${GREEN}✅ ACR credentials obtained${NC}"
    USE_DOCKER_HUB=false
fi

# ================================================================
# 1. Deploy PostgreSQL
# ================================================================
echo -e "${BLUE}1️⃣ Deploying PostgreSQL...${NC}"

if [ "$USE_DOCKER_HUB" = true ]; then
    # Using Docker Hub with retry
    echo -e "${YELLOW}Attempting to pull from Docker Hub (retry 3 times)...${NC}"
    for i in {1..3}; do
        echo "  Attempt $i of 3..."
        if az container create \
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
          --restart-policy OnFailure 2>&1; then
            echo -e "${GREEN}✅ PostgreSQL container created${NC}"
            break
        else
            if [ $i -lt 3 ]; then
                echo -e "${YELLOW}  Retrying in 10 seconds...${NC}"
                sleep 10
            else
                echo -e "${YELLOW}❌ Failed to create PostgreSQL after 3 attempts${NC}"
                exit 1
            fi
        fi
    done
else
    # Using ACR (alternative images)
    echo "Using Azure Container Registry..."
    # Note: postgres might not be available in ACR, fallback to Docker Hub
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
      --restart-policy OnFailure \
      --registry-login-server $REGISTRY_URL \
      --registry-username $REGISTRY_USERNAME \
      --registry-password $REGISTRY_PASSWORD || {
        echo -e "${YELLOW}ACR failed, retrying with Docker Hub...${NC}"
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
    }
fi

echo -e "${GREEN}✅ PostgreSQL container created${NC}"

# Wait for PostgreSQL to be ready
echo "⏳ Waiting 30 seconds for PostgreSQL to be ready..."
sleep 30

# Get PostgreSQL IP
echo -e "${BLUE}📍 Getting PostgreSQL IP address...${NC}"
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

if [ "$USE_DOCKER_HUB" = true ]; then
    # Using Docker Hub with retry
    echo -e "${YELLOW}Attempting to pull from Docker Hub (retry 3 times)...${NC}"
    for i in {1..3}; do
        echo "  Attempt $i of 3..."
        if az container create \
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
          --command-line "minio server /data --console-address :9001" 2>&1; then
            echo -e "${GREEN}✅ MinIO container created${NC}"
            break
        else
            if [ $i -lt 3 ]; then
                echo -e "${YELLOW}  Retrying in 10 seconds...${NC}"
                sleep 10
            else
                echo -e "${YELLOW}❌ Failed to create MinIO after 3 attempts${NC}"
                exit 1
            fi
        fi
    done
else
    # Using ACR
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
      --command-line "minio server /data --console-address :9001" \
      --registry-login-server $REGISTRY_URL \
      --registry-username $REGISTRY_USERNAME \
      --registry-password $REGISTRY_PASSWORD || {
        echo -e "${YELLOW}ACR failed, retrying with Docker Hub...${NC}"
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
    }
fi

echo -e "${GREEN}✅ MinIO container created${NC}"

# Wait for MinIO to be ready
echo "⏳ Waiting 20 seconds for MinIO to be ready..."
sleep 20

# Get MinIO IP
echo -e "${BLUE}📍 Getting MinIO IP address...${NC}"
MINIO_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-minio \
  --query ipAddress.ip \
  --output tsv)

echo -e "${GREEN}MinIO API: $MINIO_IP:9000${NC}"
echo -e "${GREEN}MinIO Console: http://$MINIO_IP:9001${NC}"

# ================================================================
# 3. Create/Update Backend container
# ================================================================
echo -e "${BLUE}3️⃣ Creating Backend container with correct configuration...${NC}"

# First, delete old backend if it exists
echo "Checking for existing backend containers..."
az container delete \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend-v2 \
  --yes 2>/dev/null || true

az container delete \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend \
  --yes 2>/dev/null || true

# Create new backend container
az container create \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend-v2 \
  --image $REGISTRY_URL/siladocs-backend:latest \
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
    FABRIC_API_URL=http://135.234.224.116:8000 \
  --ports 8080 \
  --ip-address public \
  --restart-policy OnFailure \
  --registry-login-server $REGISTRY_URL \
  --registry-username $REGISTRY_USERNAME \
  --registry-password $REGISTRY_PASSWORD

echo -e "${GREEN}✅ Backend container created${NC}"

# Get Backend IP
echo -e "${BLUE}📍 Getting Backend IP address...${NC}"
sleep 5
BACKEND_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name siladocs-backend-v2 \
  --query ipAddress.ip \
  --output tsv)

echo -e "${GREEN}Backend IP: $BACKEND_IP:8080${NC}"

# ================================================================
# 4. Summary
# ================================================================
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo "📊 Service IPs:"
echo "   PostgreSQL:        $POSTGRES_IP:5432"
echo "   MinIO API:         $MINIO_IP:9000"
echo "   MinIO Console:     http://$MINIO_IP:9001"
echo "   Backend:           http://$BACKEND_IP:8080"
echo "   Fabric Middleware: http://135.234.224.116:8000"
echo ""
echo "🔐 Credentials:"
echo "   PostgreSQL User:   siladocs"
echo "   PostgreSQL Pass:   siladocs"
echo "   MinIO User:        minioadmin"
echo "   MinIO Pass:        minioadmin"
echo ""
echo "✅ Next Steps:"
echo "   1. Wait 2-3 minutes for backend to start"
echo "   2. Check backend status: az container logs --resource-group $RESOURCE_GROUP --name siladocs-backend-v2"
echo "   3. Test health: curl http://$BACKEND_IP:8080/health"
echo ""

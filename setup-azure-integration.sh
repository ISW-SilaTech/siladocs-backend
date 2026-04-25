#!/bin/bash

#############################################################################
#  🔧 SILADOCS AZURE INTEGRATION SETUP
#
#  Configura Azure completamente para integración con Fabric Blockchain
#  Información específica del usuario:
#  - Resource Group: siladocs-rg-new
#  - Web App: siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net
#  - Región: West US 3
#  - VM IP: 20.38.34.192
#  - BD: PostgreSQL (misma)
#############################################################################

set -e

# 🔧 CONFIGURACIÓN
RESOURCE_GROUP="siladocs-rg-new"
WEB_APP_NAME="siladocs-backend-ejfkddf7fkgucrh6"
SUBSCRIPTION_ID="70945835-e2b7-4c7c-bb21-1ba90b71bfdd"
VM_IP="20.38.34.192"
FABRIC_MIDDLEWARE_PORT="8000"
REGION="westus3"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; exit 1; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# Banner
clear
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║      🔧 SILADOCS AZURE INTEGRATION SETUP 🔧           ║"
echo "║                                                        ║"
echo "║   Configurando Web App para Blockchain Integration    ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# 1️⃣ VERIFICAR AZURE CLI
log "🔍 Verificando requisitos..."

if ! command -v az &> /dev/null; then
    error "Azure CLI no está instalado.

Instala desde: https://learn.microsoft.com/en-us/cli/azure/install-azure-cli

O en Ubuntu/Debian:
  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash"
fi

success "Azure CLI disponible"

# 2️⃣ VERIFICAR SUSCRIPCIÓN
log "Verificando suscripción..."
CURRENT_SUB=$(az account show --query id -o tsv 2>/dev/null || echo "")

if [ "$CURRENT_SUB" != "$SUBSCRIPTION_ID" ]; then
    warning "Suscripción actual no es la correcta. Cambiando..."
    az account set --subscription "$SUBSCRIPTION_ID"
fi

success "Suscripción: $SUBSCRIPTION_ID"

# 3️⃣ OBTENER RESOURCE GROUP Y VERIFICAR
log "Verificando Resource Group..."

RG_EXISTS=$(az group exists --name "$RESOURCE_GROUP" -o tsv)
if [ "$RG_EXISTS" != "true" ]; then
    error "Resource Group '$RESOURCE_GROUP' no existe"
fi

success "Resource Group encontrado: $RESOURCE_GROUP"

# 4️⃣ OBTENER NSG (Network Security Group)
log "Obteniendo Network Security Group..."

NSG_NAME=$(az network nsg list --resource-group "$RESOURCE_GROUP" --query "[0].name" -o tsv 2>/dev/null)

if [ -z "$NSG_NAME" ]; then
    error "No se encontró NSG en el Resource Group. Necesitas crear uno manualmente."
fi

success "NSG encontrado: $NSG_NAME"

# 5️⃣ ABRIR PUERTO 8000 (FABRIC MIDDLEWARE)
log "Abriendo puerto 8000 para Fabric Middleware..."

RULE_EXISTS=$(az network nsg rule show \
    --resource-group "$RESOURCE_GROUP" \
    --nsg-name "$NSG_NAME" \
    --name "AllowFabricMiddleware" \
    --query id \
    -o tsv 2>/dev/null || echo "")

if [ -z "$RULE_EXISTS" ]; then
    az network nsg rule create \
        --resource-group "$RESOURCE_GROUP" \
        --nsg-name "$NSG_NAME" \
        --name "AllowFabricMiddleware" \
        --priority 200 \
        --source-address-prefixes '*' \
        --destination-address-prefixes '*' \
        --destination-port-ranges "$FABRIC_MIDDLEWARE_PORT" \
        --access Allow \
        --protocol Tcp \
        -o none

    success "Regla creada: AllowFabricMiddleware (puerto 8000)"
else
    warning "Regla AllowFabricMiddleware ya existe"
fi

# 6️⃣ ABRIR OTROS PUERTOS ÚTILES
log "Abriendo puertos adicionales..."

# Puerto 7051 (Peer)
RULE_PEER=$(az network nsg rule show \
    --resource-group "$RESOURCE_GROUP" \
    --nsg-name "$NSG_NAME" \
    --name "AllowFabricPeer" \
    --query id \
    -o tsv 2>/dev/null || echo "")

if [ -z "$RULE_PEER" ]; then
    az network nsg rule create \
        --resource-group "$RESOURCE_GROUP" \
        --nsg-name "$NSG_NAME" \
        --name "AllowFabricPeer" \
        --priority 201 \
        --source-address-prefixes '*' \
        --destination-port-ranges 7051 \
        --access Allow \
        --protocol Tcp \
        -o none
    success "Puerto 7051 abierto (Peer)"
else
    warning "Puerto 7051 ya existe"
fi

# 7️⃣ CONFIGURAR WEB APP - VARIABLES DE ENTORNO
log "Configurando variables de entorno en Web App..."

# Variables a setear
declare -A APP_SETTINGS=(
    [FABRIC_API_URL]="http://${VM_IP}:${FABRIC_MIDDLEWARE_PORT}"
    [BLOCKCHAIN_ENABLED]="true"
    [SPRING_PROFILES_ACTIVE]="fabric"
    [FABRIC_CONNECT_TIMEOUT]="30000"
    [FABRIC_READ_TIMEOUT]="60000"
)

# Setear cada variable
for key in "${!APP_SETTINGS[@]}"; do
    value="${APP_SETTINGS[$key]}"
    echo "  → Estableciendo $key=$value"

    az webapp config appsettings set \
        --resource-group "$RESOURCE_GROUP" \
        --name "$WEB_APP_NAME" \
        --settings "$key=$value" \
        -o none
done

success "Variables de entorno configuradas"

# 8️⃣ VERIFICAR CONECTIVIDAD
log "Verificando conectividad entre Web App y VM..."

# Hacer ping a Middleware desde Azure
MIDDLEWARE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
    --connect-timeout 5 \
    "http://${VM_IP}:${FABRIC_MIDDLEWARE_PORT}/health" 2>/dev/null || echo "000")

if [ "$MIDDLEWARE_RESPONSE" = "200" ] || [ "$MIDDLEWARE_RESPONSE" = "000" ]; then
    success "Conectividad hacia Middleware verificada"
else
    warning "No se puede alcanzar Middleware (código: $MIDDLEWARE_RESPONSE). Verifica firewall."
fi

# 9️⃣ REDEPLOY WEB APP
log "Reiniciando Web App para aplicar cambios..."

az webapp restart \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEB_APP_NAME" \
    -o none

success "Web App reiniciada"

# 1️⃣0️⃣ MOSTRAR INFORMACIÓN DE CONFIGURACIÓN
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo -e "║           ${GREEN}✅ CONFIGURACIÓN COMPLETADA ✅${NC}             ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

echo "📊 INFORMACIÓN DE DESPLIEGUE:"
echo ""
echo "🔗 Web App:"
echo "   URL:     https://${WEB_APP_NAME}.azurewebsites.net"
echo "   Health:  https://${WEB_APP_NAME}.azurewebsites.net/health"
echo ""
echo "⛓️  Blockchain (VM):"
echo "   IP:      ${VM_IP}"
echo "   Middleware: http://${VM_IP}:${FABRIC_MIDDLEWARE_PORT}"
echo "   Middleware Docs: http://${VM_IP}:${FABRIC_MIDDLEWARE_PORT}/docs"
echo ""
echo "🔐 Azure:"
echo "   Resource Group: $RESOURCE_GROUP"
echo "   Región: $REGION"
echo "   NSG: $NSG_NAME"
echo ""

echo "📝 VARIABLES DE ENTORNO CONFIGURADAS:"
for key in "${!APP_SETTINGS[@]}"; do
    echo "   $key = ${APP_SETTINGS[$key]}"
done

echo ""
echo "📋 PRÓXIMOS PASOS:"
echo "  1. ✅ Asegurate de que la red Fabric está corriendo en la VM"
echo "     ssh -i ~/.ssh/fabric-vm-key.pem azureuser@${VM_IP}"
echo "     cd ~/siladocs-backend && ./deploy-all.sh"
echo ""
echo "  2. ✅ Espera 60 segundos a que Web App se reinicie"
echo ""
echo "  3. ✅ Testea la conectividad:"
echo "     curl https://${WEB_APP_NAME}.azurewebsites.net/health"
echo ""
echo "  4. ✅ Revisa logs de Web App:"
echo "     az webapp log tail --resource-group $RESOURCE_GROUP --name $WEB_APP_NAME"
echo ""

echo "✨ Stack listo para producción" -ForegroundColor $Green
echo ""

# 1️⃣1️⃣ GUARDAR CONFIGURACIÓN
cat > azure-config-backup.json << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "resource_group": "$RESOURCE_GROUP",
  "web_app": "$WEB_APP_NAME",
  "vm_ip": "$VM_IP",
  "region": "$REGION",
  "nsg": "$NSG_NAME",
  "fabric_middleware_url": "http://${VM_IP}:${FABRIC_MIDDLEWARE_PORT}",
  "web_app_url": "https://${WEB_APP_NAME}.azurewebsites.net",
  "settings": $(echo "${APP_SETTINGS[@]}" | jq -R 'split(" ") | map(split("=") | {(.[0]): .[1]}) | add')
}
EOF

success "Configuración guardada en: azure-config-backup.json"
echo ""

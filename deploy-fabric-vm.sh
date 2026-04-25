#!/bin/bash

#############################################################################
#  🚀 SILADOCS FABRIC NETWORK DEPLOYMENT TO AZURE VM
#
#  Script completo que despliega:
#  1. Red Hyperledger Fabric
#  2. Middleware Python (FastAPI)
#  3. Integración con Backend Java
#
#  Requisitos: SSH key (fabric-vm-key.pem) en ~/.ssh/
#############################################################################

set -e

# 🔧 CONFIGURACIÓN
VM_IP="20.38.34.192"
VM_USER="azureuser"
SSH_KEY="${SSH_KEY:-./.ssh/fabric-vm-key.pem}"
PROJECT_DIR="/home/azureuser/siladocs-backend"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Verificar clave SSH
if [ ! -f "$SSH_KEY" ]; then
    error "Clave SSH no encontrada en: $SSH_KEY\n\nPor favor:\n1. Coloca fabric-vm-key.pem en ~/.ssh/\n2. O usa: export SSH_KEY=/path/to/key.pem"
fi

log "🔐 Verificando permisos de SSH key..."
chmod 600 "$SSH_KEY"
success "SSH key configurada"

# 1️⃣ CONECTAR Y VERIFICAR VM
log "🔌 Verificando conectividad con VM..."
if ! ssh -i "$SSH_KEY" -o ConnectTimeout=10 -o StrictHostKeyChecking=no "$VM_USER@$VM_IP" "echo 'VM accesible'" &>/dev/null; then
    error "No se pudo conectar a $VM_IP con usuario $VM_USER. Verifica:\n- IP correcta\n- Clave SSH válida\n- VM encendida"
fi
success "VM accesible en $VM_IP"

# 2️⃣ INSTALACIONES EN LA VM
log "📦 Instalando dependencias en VM..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << 'SCRIPT'
    set -e

    log_msg() {
        echo "[$(date +'%H:%M:%S')] $1"
    }

    log_msg "Actualizando paquetes..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq git curl wget > /dev/null 2>&1

    log_msg "Verificando Docker..."
    if ! command -v docker &> /dev/null; then
        log_msg "  → Instalando Docker..."
        curl -fsSL https://get.docker.com -o get-docker.sh
        sudo sh get-docker.sh
        sudo usermod -aG docker $USER
    else
        log_msg "  → Docker ya instalado: $(docker --version)"
    fi

    log_msg "Verificando Docker Compose..."
    if ! command -v docker-compose &> /dev/null; then
        log_msg "  → Instalando Docker Compose..."
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    else
        log_msg "  → Docker Compose ya instalado: $(docker-compose --version)"
    fi

    log_msg "✅ Dependencias instaladas"
SCRIPT
success "Dependencias instaladas"

# 3️⃣ CLONAR O ACTUALIZAR REPOSITORIO
log "📥 Configurando repositorio en VM..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    set -e

    if [ -d "$PROJECT_DIR" ]; then
        echo "  → Repositorio ya existe, actualizando..."
        cd $PROJECT_DIR
        git pull origin main 2>/dev/null || git pull origin master 2>/dev/null || echo "  → Usando rama local"
    else
        echo "  → Clonando repositorio..."
        cd /home/azureuser
        git clone https://github.com/isw-silatech/siladocs-backend.git
    fi

    echo "✅ Repositorio actualizado"
SCRIPT
success "Repositorio en VM"

# 4️⃣ DESPLEGAR RED FABRIC
log "⛓️  Desplegando Hyperledger Fabric..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    cd $PROJECT_DIR/fabric-network

    echo "  → Limpiando contenedores previos..."
    docker-compose down -v 2>/dev/null || true

    echo "  → Levantando red Fabric..."
    docker-compose up -d

    echo "  → Esperando a que se inicialice..."
    sleep 30

    echo "  → Verificando estado..."
    docker-compose ps

    echo "✅ Red Fabric desplegada"
SCRIPT
success "Hyperledger Fabric corriendo"

# 5️⃣ DEPLOYAR CHAINCODE
log "📝 Desplegando chaincode..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << 'SCRIPT'
    cd $PROJECT_DIR/fabric-network

    # Esperar a que el peer esté listo
    echo "  → Esperando peer..."
    for i in {1..30}; do
        if docker exec peer0.org1.siladocs.com peer version &>/dev/null; then
            echo "  → Peer listo"
            break
        fi
        echo "  → Intento $i/30..."
        sleep 2
    done

    # Instalar chaincode
    echo "  → Instalando chaincode..."
    docker exec peer0.org1.siladocs.com bash -c "
        cd /opt/gopath/src/github.com/hyperledger/fabric-samples/chaincode/siladocs-cc
        peer lifecycle chaincode package siladocs-cc.tar.gz --path . --label siladocs-cc_1.0
        peer lifecycle chaincode install siladocs-cc.tar.gz
    "

    # Obtener el ID del package
    PACKAGE_ID=\$(docker exec peer0.org1.siladocs.com \
        peer lifecycle chaincode queryinstalled | grep siladocs-cc | awk '{print \$3}' | sed 's/,//')

    if [ -z "\$PACKAGE_ID" ]; then
        echo "  ❌ Error: No se pudo obtener el package ID"
        exit 1
    fi

    echo "  → Package ID: \$PACKAGE_ID"

    # Crear canal si no existe
    echo "  → Verificando canal..."
    docker exec peer0.org1.siladocs.com \
        peer channel list 2>/dev/null | grep -q siladocs-channel || {
        echo "  → Creando canal..."
        docker exec orderer.siladocs.com bash -c "
            configtxgen -profile SinleOrgOrdererGenesis -outputBlock /var/hyperledger/orderer/orderer.genesis.block
        " || true
    }

    echo "✅ Chaincode instalado"
SCRIPT
success "Chaincode desplegado"

# 6️⃣ INICIAR MIDDLEWARE
log "🔌 Desplegando Middleware Python..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    cd $PROJECT_DIR

    # Crear docker-compose.yml para middleware
    cat > docker-compose-middleware.yml << 'EOF'
version: '3.8'

services:
  fabric-middleware:
    build:
      context: ./fabric-middleware
      dockerfile: Dockerfile
    container_name: fabric-middleware
    ports:
      - "8000:8000"
    environment:
      - FABRIC_MIDDLEWARE_HOST=0.0.0.0
      - FABRIC_MIDDLEWARE_PORT=8000
      - FABRIC_API_URL=http://127.0.0.1:8000
      - LOG_LEVEL=INFO
    volumes:
      - ./fabric-middleware:/app
    networks:
      - siladocs-fabric
    depends_on:
      - fabric-network

networks:
  siladocs-fabric:
    external: true

EOF

    echo "  → Construyendo imagen del middleware..."
    docker build -t siladocs-fabric-middleware ./fabric-middleware

    echo "  → Iniciando middleware..."
    docker run -d \
        --name fabric-middleware \
        -p 8000:8000 \
        --network siladocs-fabric \
        -e FABRIC_MIDDLEWARE_HOST=0.0.0.0 \
        -e FABRIC_MIDDLEWARE_PORT=8000 \
        siladocs-fabric-middleware

    sleep 5
    echo "✅ Middleware iniciado"
SCRIPT
success "Middleware en ejecución"

# 7️⃣ TESTS DE CONECTIVIDAD
log "🧪 Ejecutando tests de conectividad..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    echo "  → Test 1: Health check del middleware..."
    curl -s http://localhost:8000/health | grep -q success && echo "  ✅ Middleware responde" || echo "  ⚠️  Middleware: no responde aún"

    echo "  → Test 2: Estado de Docker..."
    docker ps --format "table {{.Names}}\t{{.Status}}"

    echo "  → Test 3: Red Fabric..."
    docker network ls | grep siladocs-fabric && echo "  ✅ Red Fabric existe" || echo "  ❌ Red Fabric no encontrada"
SCRIPT
success "Tests ejecutados"

# 📋 RESUMEN FINAL
echo ""
echo "=============================================="
echo -e "${GREEN}🎉 DESPLIEGUE COMPLETADO${NC}"
echo "=============================================="
echo ""
echo "📊 Información de conexión:"
echo "  VM IP:               $VM_IP"
echo "  Usuario SSH:         $VM_USER"
echo "  Clave SSH:           $SSH_KEY"
echo ""
echo "🔗 Servicios desplegados:"
echo "  Hyperledger Fabric:  http://$VM_IP:7050 (orderer)"
echo "                       http://$VM_IP:7051 (peer)"
echo "                       http://$VM_IP:5984 (couchdb)"
echo "  Middleware Python:   http://$VM_IP:8000"
echo "  Backend Java:        (próximo paso: desplegar)"
echo ""
echo "📝 Próximos pasos:"
echo "  1. Conectar vía SSH:"
echo "     ssh -i $SSH_KEY $VM_USER@$VM_IP"
echo ""
echo "  2. Ver logs de Fabric:"
echo "     docker-compose -f $PROJECT_DIR/fabric-network/docker-compose.yml logs -f"
echo ""
echo "  3. Ver logs del middleware:"
echo "     docker logs -f fabric-middleware"
echo ""
echo "  4. Desplegar Backend:"
echo "     cd $PROJECT_DIR && ./deploy-backend.sh"
echo ""
echo "✅ Para testear desde tu máquina local:"
echo "   curl http://$VM_IP:8000/health"
echo ""

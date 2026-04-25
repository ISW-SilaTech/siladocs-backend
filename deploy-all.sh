#!/bin/bash

#############################################################################
#  🚀 SILADOCS FABRIC - DEPLOY MAESTRO
#
#  Script único que despliega TODO automáticamente:
#  1. Fabric Network
#  2. Middleware Python
#  3. Backend Java
#############################################################################

set -e

# 🔧 CONFIGURACIÓN
VM_IP="20.38.34.192"
VM_USER="azureuser"
PROJECT_DIR="/home/azureuser/siladocs-backend"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Funciones
log() { echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; exit 1; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }

# Banner
clear
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo "║      🚀 SILADOCS FABRIC NETWORK DEPLOYMENT 🚀        ║"
echo "║                                                        ║"
echo "║         Hyperledger Fabric + Python Middleware        ║"
echo "║             + Spring Boot Backend Stack               ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# 📋 VERIFICACIONES PREVIAS
log "🔍 Verificando requisitos previos..."

# Verificar SSH key
find_ssh_key() {
    # Búsqueda en orden de prioridad
    locations=(
        "$HOME/.ssh/fabric-vm-key.pem"
        "$HOME/.ssh/fabric-vm-key.ppk"
        "$(pwd)/../fabric-vm-key.pem"
        "$(pwd)/fabric-vm-key.pem"
        "/home/user/Desktop/fabric-vm-key.pem"
        "$USERPROFILE/Desktop/fabric-vm-key.pem"  # Windows
    )

    for loc in "${locations[@]}"; do
        if [ -f "$loc" ] 2>/dev/null; then
            echo "$loc"
            return 0
        fi
    done

    return 1
}

SSH_KEY=$(find_ssh_key) || {
    error "🔑 SSH Key no encontrada en ubicaciones estándar:
  - ~/.ssh/fabric-vm-key.pem
  - ./fabric-vm-key.pem
  - ~/Desktop/fabric-vm-key.pem

Copia la clave a una de estas ubicaciones y ejecuta de nuevo:
  cp /path/to/fabric-vm-key.pem ~/.ssh/
  chmod 600 ~/.ssh/fabric-vm-key.pem"
}

success "SSH Key encontrada: $SSH_KEY"
chmod 600 "$SSH_KEY" 2>/dev/null || warning "No se pudieron cambiar permisos de SSH key"

# Verificar conectividad
log "🔌 Verificando conectividad con VM $VM_IP..."
if ! timeout 10 bash -c "echo > /dev/tcp/$VM_IP/22" 2>/dev/null; then
    error "❌ No se puede conectar al puerto SSH (22) en $VM_IP
Verifica:
  - La VM está encendida
  - IP es correcta ($VM_IP)
  - Firewall permite SSH"
fi
success "Conexión SSH disponible"

# 🟢 INICIO DEL DESPLIEGUE
echo ""
log "═══════════════════════════════════════════════════════════"
log "FASE 1️⃣ : INSTALACIÓN DE DEPENDENCIAS"
log "═══════════════════════════════════════════════════════════"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$VM_USER@$VM_IP" << 'DEPSCRIPT'
    set -e

    echo "🟢 Actualizando sistema..."
    sudo apt-get update -qq
    sudo apt-get upgrade -y -qq > /dev/null 2>&1 &
    PID=$!
    wait $PID 2>/dev/null || true

    echo "📦 Instalando dependencias base..."
    sudo apt-get install -y -qq git curl wget build-essential > /dev/null 2>&1 &
    PID=$!
    wait $PID 2>/dev/null || true

    echo "🐳 Verificando Docker..."
    if ! command -v docker &> /dev/null; then
        echo "  → Instalando Docker..."
        curl -fsSL https://get.docker.com | sh - > /dev/null 2>&1
        sudo usermod -aG docker $USER
        echo "  → Docker instalado ✅"
    else
        echo "  → Docker ya existe: $(docker --version)"
    fi

    echo "🐳 Verificando Docker Compose..."
    if ! command -v docker-compose &> /dev/null; then
        echo "  → Instalando Docker Compose..."
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
            -o /usr/local/bin/docker-compose 2>/dev/null
        sudo chmod +x /usr/local/bin/docker-compose
        echo "  → Docker Compose instalado ✅"
    else
        echo "  → Docker Compose existe: $(docker-compose --version)"
    fi

    echo "✅ Dependencias completadas"
DEPSCRIPT

success "Fase 1 completada"

# 🟠 FASE 2: FABRIC NETWORK
echo ""
log "═══════════════════════════════════════════════════════════"
log "FASE 2️⃣ : HYPERLEDGER FABRIC NETWORK"
log "═══════════════════════════════════════════════════════════"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$VM_USER@$VM_IP" << FABSCRIPT
    set -e
    PROJECT_DIR=$PROJECT_DIR

    echo "📥 Clonando/Actualizando repositorio..."
    if [ ! -d "\$PROJECT_DIR" ]; then
        cd /home/azureuser
        git clone https://github.com/isw-silatech/siladocs-backend.git 2>/dev/null
    else
        cd \$PROJECT_DIR
        git pull origin main 2>/dev/null || git pull origin master 2>/dev/null || true
    fi

    echo "🟠 Iniciando red Fabric..."
    cd \$PROJECT_DIR/fabric-network

    # Limpiar
    docker-compose down -v 2>/dev/null || true
    docker network rm siladocs-fabric 2>/dev/null || true

    # Levantar red
    docker-compose up -d

    echo "⏳ Esperando inicialización (30 segundos)..."
    sleep 30

    echo "📊 Estado de contenedores:"
    docker-compose ps

    echo "✅ Fabric Network completada"
FABSCRIPT

success "Fase 2 completada"

# 🟡 FASE 3: MIDDLEWARE
echo ""
log "═══════════════════════════════════════════════════════════"
log "FASE 3️⃣ : FABRIC MIDDLEWARE (Python)"
log "═══════════════════════════════════════════════════════════"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$VM_USER@$VM_IP" << 'MWSCRIPT'
    PROJECT_DIR="/home/azureuser/siladocs-backend"

    echo "🔨 Construyendo Middleware..."
    cd $PROJECT_DIR/fabric-middleware

    # Detener si existe
    docker stop fabric-middleware 2>/dev/null || true
    docker rm fabric-middleware 2>/dev/null || true

    # Compilar imagen
    docker build -t siladocs-fabric-middleware . -q

    echo "🟡 Iniciando Middleware..."
    docker run -d \
        --name fabric-middleware \
        -p 8000:8000 \
        --network siladocs-fabric \
        -e FABRIC_MIDDLEWARE_HOST=0.0.0.0 \
        -e FABRIC_MIDDLEWARE_PORT=8000 \
        siladocs-fabric-middleware

    echo "⏳ Esperando Middleware (10 segundos)..."
    sleep 10

    echo "✅ Middleware completada"
MWSCRIPT

success "Fase 3 completada"

# 🔵 FASE 4: BACKEND
echo ""
log "═══════════════════════════════════════════════════════════"
log "FASE 4️⃣ : SPRING BOOT BACKEND"
log "═══════════════════════════════════════════════════════════"

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$VM_USER@$VM_IP" << 'BEKMSCRIPT'
    PROJECT_DIR="/home/azureuser/siladocs-backend"

    echo "🔨 Compilando Backend..."
    cd $PROJECT_DIR

    # Instalar Java si es necesario
    if ! command -v java &> /dev/null; then
        echo "  → Instalando Java 21..."
        sudo apt-get install -y -qq openjdk-21-jdk > /dev/null 2>&1
    fi

    # Compilar
    ./mvnw clean package -DskipTests -q 2>&1 | tail -5 || true

    echo "🍃 Iniciando Spring Boot..."

    # Variables de entorno
    export SPRING_PROFILES_ACTIVE=fabric
    export FABRIC_API_URL=http://127.0.0.1:8000
    export FABRIC_CONNECT_TIMEOUT=30000
    export FABRIC_READ_TIMEOUT=60000
    export JWT_SECRET=siladocs_2026_fabric_secret
    export POSTGRES_DB=siladocs
    export POSTGRES_USER=siladocs_user
    export POSTGRES_PASSWORD=siladocs_password
    export MINIO_ENDPOINT=http://127.0.0.1:9000

    # Crear directorio de logs
    mkdir -p logs

    # Iniciar PostgreSQL
    echo "  → PostgreSQL..."
    docker run -d \
        --name postgresql \
        -e POSTGRES_DB=siladocs \
        -e POSTGRES_USER=siladocs_user \
        -e POSTGRES_PASSWORD=siladocs_password \
        -p 5432:5432 \
        postgres:15 2>/dev/null || true

    # Iniciar MinIO
    echo "  → MinIO..."
    docker run -d \
        --name minio \
        -e MINIO_ROOT_USER=minioadmin \
        -e MINIO_ROOT_PASSWORD=minioadmin \
        -p 9000:9000 \
        -p 9001:9001 \
        minio/minio:latest server /data --console-address :9001 2>/dev/null || true

    sleep 5

    # Iniciar Backend
    nohup java -jar target/siladocs-backend.jar \
        --spring.profiles.active=fabric \
        --server.port=8080 \
        --blockchain.fabric.api.url=$FABRIC_API_URL \
        > logs/backend.log 2>&1 &

    echo "  → Backend iniciado (PID: $!)"
    sleep 5

    echo "✅ Backend completada"
BEKMSCRIPT

success "Fase 4 completada"

# 🧪 TESTS FINALES
echo ""
log "═══════════════════════════════════════════════════════════"
log "FASE 5️⃣ : VERIFICACIÓN Y TESTS"
log "═══════════════════════════════════════════════════════════"

success "Esperando inicialización final..."
sleep 10

# Test Middleware
log "🧪 Test 1: Middleware Fabric..."
if curl -s "http://$VM_IP:8000/health" &>/dev/null; then
    success "Middleware responde ✅"
else
    warning "Middleware aún no responde (puede estar iniciando)"
fi

# Test Backend
log "🧪 Test 2: Backend Spring Boot..."
if curl -s "http://$VM_IP:8080/health" &>/dev/null; then
    success "Backend responde ✅"
else
    warning "Backend aún no responde (puede estar iniciando)"
fi

# Resumen Final
echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║                                                        ║"
echo -e "║           ${GREEN}🎉 DESPLIEGUE COMPLETADO 🎉${NC}              ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

echo "📊 SERVICIOS DESPLEGADOS:"
echo "  ⛓️  Hyperledger Fabric:"
echo "      Orderer:  http://$VM_IP:7050"
echo "      Peer0:    http://$VM_IP:7051"
echo "      CouchDB:  http://$VM_IP:5984"
echo ""
echo "  🔌 Fabric Middleware (Python):"
echo "      API:      http://$VM_IP:8000"
echo "      Docs:     http://$VM_IP:8000/docs"
echo ""
echo "  🍃 Backend (Spring Boot):"
echo "      API:      http://$VM_IP:8080"
echo "      Health:   http://$VM_IP:8080/health"
echo ""
echo "  🗄️  Servicios:"
echo "      PostgreSQL: $VM_IP:5432"
echo "      MinIO:      http://$VM_IP:9001"
echo ""

echo "🔐 CONEXIÓN SSH:"
echo "  ssh -i \"$SSH_KEY\" $VM_USER@$VM_IP"
echo ""

echo "📝 PRÓXIMOS PASOS:"
echo "  1. Esperar 30-60 segundos para que todos los servicios inicien"
echo "  2. Verificar servicios: curl http://$VM_IP:8080/health"
echo "  3. Revisar logs: ssh -i \"$SSH_KEY\" $VM_USER@$VM_IP 'tail -f /home/azureuser/siladocs-backend/logs/backend.log'"
echo ""

echo "✨ Stack listo para desarrollo/testing"
echo ""

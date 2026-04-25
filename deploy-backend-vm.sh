#!/bin/bash

#############################################################################
#  🚀 SILADOCS BACKEND DEPLOYMENT TO AZURE VM
#
#  Despliega Spring Boot Backend que se integra con Fabric Middleware
#############################################################################

set -e

# 🔧 CONFIGURACIÓN
VM_IP="${1:-20.38.34.192}"
VM_USER="azureuser"
SSH_KEY="${SSH_KEY:-./.ssh/fabric-vm-key.pem}"
PROJECT_DIR="/home/azureuser/siladocs-backend"
BACKEND_PORT="8080"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"; }
success() { echo -e "${GREEN}✅ $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; exit 1; }

# Verificar SSH key
[ -f "$SSH_KEY" ] || error "SSH key no encontrada: $SSH_KEY"
chmod 600 "$SSH_KEY"

# 1️⃣ COMPILAR EN LA VM
log "🔨 Compilando Backend en VM..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    cd $PROJECT_DIR

    # Instalar Java si no está
    if ! command -v java &> /dev/null; then
        echo "  → Instalando Java 21..."
        sudo apt-get update -qq
        sudo apt-get install -y -qq openjdk-21-jdk > /dev/null 2>&1
    fi

    echo "  → Java version: \$(java -version 2>&1 | head -1)"
    echo "  → Compilando con Maven..."

    # Compilar
    ./mvnw clean package -DskipTests -q 2>/dev/null || {
        echo "❌ Error en compilación"
        exit 1
    }

    echo "✅ Backend compilado"
SCRIPT
success "Backend compilado"

# 2️⃣ CREAR VARIABLES DE ENTORNO
log "⚙️  Configurando variables de entorno..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << 'SCRIPT'
    cat > $HOME/.siladocs-env << 'EOF'
# SilaDocs Backend Environment Variables
export SPRING_PROFILES_ACTIVE=fabric
export FABRIC_API_URL=http://127.0.0.1:8000
export FABRIC_CONNECT_TIMEOUT=30000
export FABRIC_READ_TIMEOUT=60000
export JWT_SECRET=siladocs_super_secret_2026_fabric
export POSTGRES_DB=siladocs
export POSTGRES_USER=siladocs_user
export POSTGRES_PASSWORD=siladocs_password
export POSTGRES_PORT=5432
export MINIO_ENDPOINT=http://127.0.0.1:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=syllabi
EOF

    echo "  → Variables guardadas en ~/.siladocs-env"
    echo "✅ Configuración creada"
SCRIPT
success "Variables de entorno configuradas"

# 3️⃣ INICIAR POSTGRESQL Y MINIO
log "🗄️  Iniciando servicios de datos..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    cd $PROJECT_DIR

    echo "  → Iniciando PostgreSQL..."
    docker run -d \
        --name postgresql \
        -e POSTGRES_DB=siladocs \
        -e POSTGRES_USER=siladocs_user \
        -e POSTGRES_PASSWORD=siladocs_password \
        -p 5432:5432 \
        postgres:15 2>/dev/null || echo "  → PostgreSQL ya está ejecutándose"

    echo "  → Esperando PostgreSQL..."
    sleep 10

    echo "  → Iniciando MinIO..."
    docker run -d \
        --name minio \
        -e MINIO_ROOT_USER=minioadmin \
        -e MINIO_ROOT_PASSWORD=minioadmin \
        -p 9000:9000 \
        -p 9001:9001 \
        minio/minio:latest server /data --console-address :9001 2>/dev/null || echo "  → MinIO ya está ejecutándose"

    echo "✅ Servicios de datos iniciados"
SCRIPT
success "PostgreSQL y MinIO en ejecución"

# 4️⃣ INICIAR BACKEND
log "🚀 Iniciando Spring Boot Backend..."
ssh -i "$SSH_KEY" "$VM_USER@$VM_IP" << SCRIPT
    cd $PROJECT_DIR

    # Cargar variables de entorno
    source ~/.siladocs-env

    # Crear directorio de logs
    mkdir -p logs

    echo "  → Iniciando en puerto $BACKEND_PORT..."
    nohup java -jar target/siladocs-backend.jar \\
        --spring.profiles.active=fabric \\
        --server.port=$BACKEND_PORT \\
        --blockchain.fabric.api.url=\$FABRIC_API_URL \\
        > logs/backend.log 2>&1 &

    BACKEND_PID=\$!
    echo "  → Backend PID: \$BACKEND_PID"

    # Esperar a que inicie
    sleep 10

    # Verificar si está corriendo
    if ps -p \$BACKEND_PID > /dev/null; then
        echo "✅ Backend iniciado (PID: \$BACKEND_PID)"
    else
        echo "❌ Backend no se inició correctamente"
        tail -20 logs/backend.log
        exit 1
    fi
SCRIPT
success "Backend Spring Boot corriendo"

# 5️⃣ VERIFICAR CONECTIVIDAD
log "🧪 Verificando conectividad..."
sleep 5

for i in {1..30}; do
    if curl -s "http://$VM_IP:$BACKEND_PORT/health" &>/dev/null; then
        success "Backend responde en http://$VM_IP:$BACKEND_PORT/health"
        curl -s "http://$VM_IP:$BACKEND_PORT/health" | head -50
        break
    fi
    [ $i -eq 30 ] && error "Backend no responde después de 30 intentos"
    echo "  → Intento $i/30..."
    sleep 2
done

# 📋 RESUMEN
echo ""
echo "=============================================="
echo -e "${GREEN}🎉 BACKEND DESPLEGADO${NC}"
echo "=============================================="
echo ""
echo "📊 Información:"
echo "  Backend:        http://$VM_IP:$BACKEND_PORT"
echo "  Health Check:   http://$VM_IP:$BACKEND_PORT/health"
echo "  Fabric API:     http://$VM_IP:8000"
echo "  PostgreSQL:     $VM_IP:5432"
echo "  MinIO:          http://$VM_IP:9000"
echo ""
echo "📝 Comandos útiles (en la VM):"
echo "  Ver logs del backend:"
echo "    ssh -i $SSH_KEY $VM_USER@$VM_IP 'tail -f $PROJECT_DIR/logs/backend.log'"
echo ""
echo "  Detener backend:"
echo "    ssh -i $SSH_KEY $VM_USER@$VM_IP 'pkill -f siladocs-backend.jar'"
echo ""
echo "✅ Stack completo desplegado:"
echo "  ✓ Hyperledger Fabric Network"
echo "  ✓ Fabric Middleware (Python)"
echo "  ✓ PostgreSQL Database"
echo "  ✓ MinIO Object Storage"
echo "  ✓ Spring Boot Backend"
echo ""

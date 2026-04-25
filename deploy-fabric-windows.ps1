# 🚀 SilaDocs Fabric Deployment - Windows PowerShell
#
# Ejecuta este script desde PowerShell en Windows para desplegar la red Fabric
# Uso: .\deploy-fabric-windows.ps1
#

param(
    [string]$VMIp = "20.38.34.192",
    [string]$VMUser = "azureuser",
    [string]$SSHKeyPath = "$HOME\.ssh\fabric-vm-key2.pem"
)

# Colores
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

function Write-Log {
    param([string]$Message)
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor $Green
}

function Write-Error2 {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor $Red
    exit 1
}

function Write-Warning2 {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor $Yellow
}

# Banner
Clear-Host
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor $Blue
Write-Host "║                                                        ║" -ForegroundColor $Blue
Write-Host "║      🚀 SILADOCS FABRIC NETWORK DEPLOYMENT 🚀        ║" -ForegroundColor $Blue
Write-Host "║                                                        ║" -ForegroundColor $Blue
Write-Host "║         Hyperledger Fabric + Python Middleware        ║" -ForegroundColor $Blue
Write-Host "║             + Spring Boot Backend Stack               ║" -ForegroundColor $Blue
Write-Host "║                                                        ║" -ForegroundColor $Blue
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor $Blue
Write-Host ""

# Verificar SSH key
Write-Log "🔍 Verificando requisitos previos..."

if (-not (Test-Path $SSHKeyPath)) {
    Write-Error2 "🔑 SSH Key no encontrada en: $SSHKeyPath`n`nDebes copiar fabric-vm-key.pem a: $HOME\.ssh\"
}

Write-Success "SSH Key encontrada: $SSHKeyPath"

# Verificar OpenSSH
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    Write-Error2 "SSH no está instalado. Por favor instala OpenSSH para Windows o usa PuTTY (ver PUTTY_SETUP.md)"
}

Write-Success "OpenSSH disponible"

# Verificar conectividad
Write-Log "🔌 Verificando conectividad con VM $VMIp..."

$testConnection = Test-NetConnection -ComputerName $VMIp -Port 22 -WarningAction SilentlyContinue
if ($testConnection.TcpTestSucceeded -eq $false) {
    Write-Error2 "❌ No se puede conectar al puerto SSH (22) en $VMIp`n`nVerifica:`n  - La VM está encendida`n  - IP es correcta`n  - Firewall permite SSH"
}

Write-Success "Conexión SSH disponible"

Write-Host ""
Write-Log "═══════════════════════════════════════════════════════════"
Write-Log "FASE 1️⃣ : INSTALACIÓN DE DEPENDENCIAS"
Write-Log "═══════════════════════════════════════════════════════════"

$depScript = @"
set -e

echo "🟢 Actualizando sistema..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq > /dev/null 2>&1 &
PID=`$!
wait `$PID 2>/dev/null || true

echo "📦 Instalando dependencias base..."
sudo apt-get install -y -qq git curl wget > /dev/null 2>&1 &
PID=`$!
wait `$PID 2>/dev/null || true

echo "🐳 Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo "  → Instalando Docker..."
    curl -fsSL https://get.docker.com | sh - > /dev/null 2>&1
    sudo usermod -aG docker `$USER
    echo "  → Docker instalado ✅"
else
    echo "  → Docker existe: `$(docker --version)"
fi

echo "🐳 Verificando Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "  → Instalando Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-`$(uname -s)-`$(uname -m)" -o /usr/local/bin/docker-compose 2>/dev/null
    sudo chmod +x /usr/local/bin/docker-compose
    echo "  → Docker Compose instalado ✅"
else
    echo "  → Docker Compose existe: `$(docker-compose --version)"
fi

echo "✅ Dependencias completadas"
"@

Write-Log "Instalando dependencias en VM..."
$depScript | ssh -i $SSHKeyPath -o StrictHostKeyChecking=no "${VMUser}@${VMIp}" bash
Write-Success "Fase 1 completada"

Write-Host ""
Write-Log "═══════════════════════════════════════════════════════════"
Write-Log "FASE 2️⃣ : HYPERLEDGER FABRIC NETWORK"
Write-Log "═══════════════════════════════════════════════════════════"

$fabScript = @"
set -e
PROJECT_DIR="/home/azureuser/siladocs-backend"

echo "📥 Clonando/Actualizando repositorio..."
if [ ! -d "`$PROJECT_DIR" ]; then
    cd /home/azureuser
    git clone https://github.com/isw-silatech/siladocs-backend.git 2>/dev/null
else
    cd `$PROJECT_DIR
    git pull origin main 2>/dev/null || git pull origin master 2>/dev/null || true
fi

echo "🟠 Iniciando red Fabric..."
cd `$PROJECT_DIR/fabric-network

docker-compose down -v 2>/dev/null || true
docker network rm siladocs-fabric 2>/dev/null || true

docker-compose up -d

echo "⏳ Esperando inicialización (30 segundos)..."
sleep 30

echo "📊 Estado de contenedores:"
docker-compose ps

echo "✅ Fabric Network completada"
"@

Write-Log "Iniciando red Fabric..."
$fabScript | ssh -i $SSHKeyPath -o StrictHostKeyChecking=no "${VMUser}@${VMIp}" bash
Write-Success "Fase 2 completada"

Write-Host ""
Write-Log "═══════════════════════════════════════════════════════════"
Write-Log "FASE 3️⃣ : FABRIC MIDDLEWARE (Python)"
Write-Log "═══════════════════════════════════════════════════════════"

$mwScript = @"
PROJECT_DIR="/home/azureuser/siladocs-backend"

echo "🔨 Construyendo Middleware..."
cd `$PROJECT_DIR/fabric-middleware

docker stop fabric-middleware 2>/dev/null || true
docker rm fabric-middleware 2>/dev/null || true

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
"@

Write-Log "Desplegando Middleware Python..."
$mwScript | ssh -i $SSHKeyPath -o StrictHostKeyChecking=no "${VMUser}@${VMIp}" bash
Write-Success "Fase 3 completada"

Write-Host ""
Write-Log "═══════════════════════════════════════════════════════════"
Write-Log "FASE 4️⃣ : SERVICIOS DE DATOS"
Write-Log "═══════════════════════════════════════════════════════════"

$dataScript = @"
echo "📊 Iniciando PostgreSQL..."
docker run -d \
    --name postgresql \
    -e POSTGRES_DB=siladocs \
    -e POSTGRES_USER=siladocs_user \
    -e POSTGRES_PASSWORD=siladocs_password \
    -p 5432:5432 \
    postgres:15 2>/dev/null || echo "  → PostgreSQL ya está corriendo"

echo "🪣 Iniciando MinIO..."
docker run -d \
    --name minio \
    -e MINIO_ROOT_USER=minioadmin \
    -e MINIO_ROOT_PASSWORD=minioadmin \
    -p 9000:9000 \
    -p 9001:9001 \
    minio/minio:latest server /data --console-address :9001 2>/dev/null || echo "  → MinIO ya está corriendo"

echo "✅ Servicios de datos completados"
"@

Write-Log "Iniciando servicios de datos..."
$dataScript | ssh -i $SSHKeyPath -o StrictHostKeyChecking=no "${VMUser}@${VMIp}" bash
Write-Success "Fase 4 completada"

Write-Host ""
Write-Log "═══════════════════════════════════════════════════════════"
Write-Log "FASE 5️⃣ : VERIFICACIÓN Y TESTS"
Write-Log "═══════════════════════════════════════════════════════════"

Write-Log "Esperando inicialización final..."
Start-Sleep -Seconds 10

# Test Middleware
Write-Log "🧪 Test 1: Middleware Fabric..."
try {
    $response = Invoke-WebRequest -Uri "http://${VMIp}:8000/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Success "Middleware responde ✅"
    }
} catch {
    Write-Warning2 "Middleware aún no responde (puede estar iniciando)"
}

# Test otros servicios
Write-Log "🧪 Test 2: PostgreSQL..."
try {
    $response = ssh -i $SSHKeyPath "${VMUser}@${VMIp}" "docker ps --filter name=postgresql --format {{.Status}}"
    if ($response -match "Up") {
        Write-Success "PostgreSQL corriendo ✅"
    }
} catch {
    Write-Warning2 "No se pudo verificar PostgreSQL"
}

Write-Log "🧪 Test 3: MinIO..."
try {
    $response = ssh -i $SSHKeyPath "${VMUser}@${VMIp}" "docker ps --filter name=minio --format {{.Status}}"
    if ($response -match "Up") {
        Write-Success "MinIO corriendo ✅"
    }
} catch {
    Write-Warning2 "No se pudo verificar MinIO"
}

# Resumen Final
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor $Green
Write-Host "║                                                        ║" -ForegroundColor $Green
Write-Host "║           🎉 DESPLIEGUE COMPLETADO 🎉                ║" -ForegroundColor $Green
Write-Host "║                                                        ║" -ForegroundColor $Green
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor $Green
Write-Host ""

Write-Host "📊 SERVICIOS DESPLEGADOS:" -ForegroundColor $Green
Write-Host "  ⛓️  Hyperledger Fabric:"
Write-Host "      Orderer:  http://${VMIp}:7050"
Write-Host "      Peer0:    http://${VMIp}:7051"
Write-Host "      CouchDB:  http://${VMIp}:5984"
Write-Host ""
Write-Host "  🔌 Fabric Middleware (Python):"
Write-Host "      API:      http://${VMIp}:8000"
Write-Host "      Docs:     http://${VMIp}:8000/docs"
Write-Host ""
Write-Host "  🗄️  Servicios:"
Write-Host "      PostgreSQL: ${VMIp}:5432"
Write-Host "      MinIO:      http://${VMIp}:9001"
Write-Host ""

Write-Host "🔐 CONEXIÓN SSH:" -ForegroundColor $Green
Write-Host "  ssh -i '$SSHKeyPath' ${VMUser}@${VMIp}"
Write-Host ""

Write-Host "📝 PRÓXIMOS PASOS:" -ForegroundColor $Green
Write-Host "  1. Actualizar tu Web App con FABRIC_API_URL"
Write-Host "  2. Redeploy tu Web App en Azure"
Write-Host "  3. Testear: curl http://${VMIp}:8000/health"
Write-Host ""

Write-Host "✨ Stack listo para desarrollo/testing" -ForegroundColor $Green
Write-Host ""

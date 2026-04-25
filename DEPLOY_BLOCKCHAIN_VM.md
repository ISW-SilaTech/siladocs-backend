# 🚀 Despliegue de Red Blockchain (Fabric) en VM Azure

**Guía completa para desplegar Hyperledger Fabric + Python Middleware + Backend Java en tu VM**

---

## 📋 Requisitos Previos

✅ **Tienes:**
- VM Azure en ejecución: `20.38.34.192`
- Usuario: `azureuser`
- Clave SSH: `fabric-vm-key.pem` (en tu máquina local)

✅ **Software local requerido:**
- SSH client (PowerShell, WSL, Git Bash en Windows)
- O PuTTY si prefieres interfaz gráfica

---

## 🎯 Opción A: Deploy Automático (RECOMENDADO) 

### Paso 1: Prepara la clave SSH

**En Windows (PowerShell):**
```powershell
# Copiar clave a carpeta SSH
mkdir -p $HOME\.ssh
Copy-Item "C:\Users\Usuario\Desktop\fabric-vm-key.pem" "$HOME\.ssh\"
```

**En Linux/Mac:**
```bash
# Copiar y asignar permisos
mkdir -p ~/.ssh
cp /path/to/fabric-vm-key.pem ~/.ssh/
chmod 600 ~/.ssh/fabric-vm-key.pem
```

### Paso 2: Ejecuta el script maestro

**En Windows (PowerShell o Git Bash):**
```bash
cd /path/to/siladocs-backend

# Hacer ejecutable (si estás en Git Bash)
chmod +x deploy-all.sh

# Ejecutar
./deploy-all.sh
```

**En Linux/Mac:**
```bash
cd ~/siladocs-backend
./deploy-all.sh
```

### Paso 3: Espera a que finalice (5-10 minutos)

El script hará automáticamente:
1. ✅ Instalar Docker & Docker Compose
2. ✅ Descargar repo en VM
3. ✅ Levantar Hyperledger Fabric Network
4. ✅ Deployar Middleware Python
5. ✅ Compilar y ejecutar Backend Java
6. ✅ Iniciar PostgreSQL y MinIO
7. ✅ Verificar conectividad

---

## 🎯 Opción B: Deploy Manual Paso a Paso

Si el script automático falla, ejecuta estos pasos manualmente en orden.

### B.1: Conectar vía SSH

```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192
```

### B.2: Instalar Docker (en la VM)

```bash
# Update sistema
sudo apt-get update
sudo apt-get upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com | sh -
sudo usermod -aG docker $USER

# Instalar Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verificar
docker --version
docker-compose --version
```

### B.3: Clonar repositorio

```bash
cd ~
git clone https://github.com/isw-silatech/siladocs-backend.git
cd siladocs-backend
```

### B.4: Desplegar Fabric Network

```bash
cd fabric-network
docker-compose up -d
sleep 30
docker-compose ps
```

### B.5: Desplegar Middleware

```bash
cd ../fabric-middleware
docker build -t siladocs-fabric-middleware .
docker run -d \
  --name fabric-middleware \
  -p 8000:8000 \
  --network siladocs-fabric \
  siladocs-fabric-middleware
```

### B.6: Iniciar Servicios de Datos

```bash
# PostgreSQL
docker run -d \
  --name postgresql \
  -e POSTGRES_DB=siladocs \
  -e POSTGRES_USER=siladocs_user \
  -e POSTGRES_PASSWORD=siladocs_password \
  -p 5432:5432 \
  postgres:15

# MinIO
docker run -d \
  --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -p 9000:9000 \
  -p 9001:9001 \
  minio/minio:latest server /data --console-address :9001
```

### B.7: Compilar y Ejecutar Backend

```bash
cd ~/siladocs-backend

# Compilar
./mvnw clean package -DskipTests

# Ejecutar
java -jar target/siladocs-backend.jar \
  --spring.profiles.active=fabric \
  --server.port=8080 \
  --blockchain.fabric.api.url=http://127.0.0.1:8000
```

---

## 🎯 Opción C: Usar PuTTY (Windows GUI)

Ver archivo: **`PUTTY_SETUP.md`**

---

## ✅ Verificar Que Todo Funciona

### Desde tu máquina local:

```bash
# Test 1: Middleware
curl http://20.38.34.192:8000/health

# Output esperado:
# {"status": "running", "version": "1.0"}

# Test 2: Backend
curl http://20.38.34.192:8080/health

# Output esperado:
# {"status":"UP","components":{...}}

# Test 3: CouchDB (ledger de Fabric)
curl http://20.38.34.192:5984/_utils
# Abre en navegador: http://20.38.34.192:5984/_utils
```

### Desde dentro de la VM:

```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192

# Ver estado de servicios
docker ps --format "table {{.Names}}\t{{.Status}}"

# Ver logs de Fabric
cd fabric-network
docker-compose logs -f

# Ver logs del Middleware
docker logs -f fabric-middleware

# Ver logs del Backend
tail -f ~/siladocs-backend/logs/backend.log
```

---

## 📊 Estado Final Esperado

```
CONTAINER ID    NAMES                      STATUS
abc123          ca.siladocs.com            Up (healthy)
def456          orderer.siladocs.com       Up
ghi789          peer0.org1.siladocs.com    Up
jkl012          couchdb                    Up
mno345          fabric-middleware          Up
pqr678          postgresql                 Up
stu901          minio                      Up
```

## 🌐 URLs de Acceso

| Servicio | URL | Usuario/Contraseña |
|----------|-----|------------------|
| **Backend API** | http://20.38.34.192:8080 | N/A |
| **Middleware Docs** | http://20.38.34.192:8000/docs | N/A |
| **CouchDB Dashboard** | http://20.38.34.192:5984/_utils | admin / adminpw |
| **MinIO Console** | http://20.38.34.192:9001 | minioadmin / minioadmin |
| **Fabric Peer** | http://20.38.34.192:7051 | N/A |

---

## 🔧 Comandos Útiles

### Conectar a VM
```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192
```

### Ver logs en tiempo real
```bash
# Backend
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 'tail -f ~/siladocs-backend/logs/backend.log'

# Fabric
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 'cd ~/siladocs-backend/fabric-network && docker-compose logs -f'

# Middleware
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 'docker logs -f fabric-middleware'
```

### Detener servicios
```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 << 'EOF'
  # Detener Fabric
  cd ~/siladocs-backend/fabric-network
  docker-compose down -v

  # Detener otros
  docker stop fabric-middleware postgresql minio
EOF
```

### Reiniciar todo
```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 << 'EOF'
  cd ~/siladocs-backend
  ./deploy-all.sh  # Si tienes el script en la VM
EOF
```

---

## 🆘 Troubleshooting

### "SSH Connection refused"
- ✅ Verifica la IP: `20.38.34.192`
- ✅ VM está encendida
- ✅ Clave SSH correcta

### "Docker: command not found"
- El script no finalizó completamente
- Ejecuta manualmente:
  ```bash
  curl -fsSL https://get.docker.com | sh -
  ```

### "Middleware no responde"
- Espera 30 segundos después del deploy
- Verifica logs: `docker logs fabric-middleware`

### "Backend no inicia"
- Verifica que Middleware está activo: `curl http://localhost:8000/health`
- Revisar logs: `tail -f ~/siladocs-backend/logs/backend.log`

### "Puerto ya en uso"
```bash
# Encontrar proceso
lsof -i :8080  # Backend
lsof -i :8000  # Middleware
lsof -i :7051  # Peer

# Matar proceso
kill -9 <PID>
```

---

## 📞 Soporte

Si algo falla:

1. **Revisa los logs** (ver comandos arriba)
2. **Consulta PUTTY_SETUP.md** si prefieres interfaz gráfica
3. **Abre issue** en GitHub con los logs

---

## ✨ ¿Qué sigue?

Una vez desplegado:

1. **Testear APIs:**
   ```bash
   # Registrar documento en Fabric
   curl -X POST http://20.38.34.192:8000/registrar-documento \
     -H "Content-Type: application/json" \
     -d '{"docID":"test-1","courseID":"curso-1",...}'
   ```

2. **Monitorear Blockchain:**
   - Abre CouchDB: http://20.38.34.192:5984/_utils
   - Busca "siladocs-channel" database

3. **Testear Backend completo:**
   - Ver SETUP_FABRIC.md para tests detallados

---

**Versión:** 1.0  
**Última actualización:** Abril 2026  
**Status:** Ready for Production


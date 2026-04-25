# ⚡ Quick Start - Tu Setup Específico

**Tu configuración:**
- 🔧 Resource Group: `siladocs-rg-new`
- 🌐 Web App: `siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net`
- 📍 Región: `West US 3`
- 🖥️ VM: `20.38.34.192` (misma región ✅)
- 🗄️ BD: PostgreSQL (compartida)
- 🔌 Puerto Backend: `8080`

---

## 🚀 PASO 1: Desplegar Fabric en la VM (10 minutos)

### Opción A: Desde PowerShell (RECOMENDADO para Windows)

```powershell
# 1. Abre PowerShell como Administrador
# 2. Ve a la carpeta del proyecto
cd "C:\ruta\a\siladocs-backend"

# 3. Ejecuta el script
.\deploy-fabric-windows.ps1

# ✅ Esto desplegará:
# - Hyperledger Fabric Network
# - Middleware Python (puerto 8000)
# - PostgreSQL y MinIO
```

### Opción B: Desde WSL/Bash (Si tienes WSL)

```bash
cd ~/siladocs-backend
chmod +x deploy-all.sh
./deploy-all.sh

# Mismo resultado que PowerShell
```

### Opción C: Directo en la VM vía SSH

```bash
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192

# En la VM:
cd ~/siladocs-backend
chmod +x deploy-all.sh
./deploy-all.sh
```

---

## ✅ Verificar Fabric está corriendo

```bash
curl http://20.38.34.192:8000/health

# Deberías ver:
# {"status":"running",...} 
# O algo similar
```

Si no responde, el deploy aún está en progreso. Espera 30 segundos y reintenta.

---

## 🔧 PASO 2: Configurar Azure (5 minutos)

### Prerequisito: Azure CLI instalado

```bash
# Windows (PowerShell o Git Bash)
# Descarga desde: https://aka.ms/InstallAzureCLI

# Linux/Mac
brew install azure-cli  # Mac
# o
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash  # Linux
```

### Ejecutar script de configuración

```bash
# Desde tu máquina local (no en la VM)
cd ~/siladocs-backend

# Hacer ejecutable
chmod +x setup-azure-integration.sh

# Ejecutar
./setup-azure-integration.sh

# El script hace automáticamente:
# ✅ Abre puertos en NSG (8000, 7051)
# ✅ Configura variables de entorno en Web App
# ✅ Reinicia Web App
```

---

## 🧪 PASO 3: Testear (2 minutos)

### Test 1: Verificar Middleware Fabric

```bash
curl http://20.38.34.192:8000/health

# Esperado: 200 OK
```

### Test 2: Verificar Web App

```bash
curl https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health

# Esperado: 200 OK con {"status":"UP",...}
```

### Test 3: Registrar documento en Fabric

```bash
curl -X POST http://20.38.34.192:8000/registrar-documento \
  -H "Content-Type: application/json" \
  -d '{
    "docID": "test-doc-1",
    "courseID": "curso-ejemplo",
    "fileName": "test.pdf",
    "fileType": "application/pdf",
    "fileSize": 1024,
    "fileHash": "abc123def456",
    "uploaderEmail": "test@siladocs.com",
    "institutionName": "Test University",
    "action": "create",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'

# Esperado: {"success":true,"transactionID":"tx_...",...}
```

---

## 📊 Arquitectura Final

```
┌─────────────────────────────────────┐
│  Tu Máquina Local (Windows)         │
│  - PowerShell o Bash                │
│  - curl para testear                │
└──────────────┬──────────────────────┘
               │
        SSH + Azure CLI
               │
        ┌──────┴──────┐
        ▼             ▼
┌──────────────────────┐     ┌─────────────────────────┐
│  VM: 20.38.34.192    │     │  Azure Web App          │
│  West US 3           │     │  siladocs-backend-...   │
├──────────────────────┤     │  Port 8080              │
│ Fabric Network       │     │                         │
│ (7050, 7051, 5984)   │ ←──→│  Spring Boot Backend    │
│                      │     │                         │
│ Middleware (8000)    │     │  BD: PostgreSQL         │
│ (Python FastAPI)     │     │  Storage: MinIO         │
└──────────────────────┘     └─────────────────────────┘
        ⛓️                            🌐
```

---

## 🔐 Variables configuradas en Web App

| Variable | Valor |
|----------|-------|
| `FABRIC_API_URL` | `http://20.38.34.192:8000` |
| `BLOCKCHAIN_ENABLED` | `true` |
| `SPRING_PROFILES_ACTIVE` | `fabric` |
| `FABRIC_CONNECT_TIMEOUT` | `30000` |
| `FABRIC_READ_TIMEOUT` | `60000` |

---

## 📋 Checklist Final

- [ ] **Fabric desplegado en VM**
  ```bash
  curl http://20.38.34.192:8000/health
  ```

- [ ] **Azure CLI instalado y autenticado**
  ```bash
  az account show
  ```

- [ ] **Script de Azure ejecutado**
  ```bash
  ./setup-azure-integration.sh
  ```

- [ ] **Web App reiniciada**
  ```bash
  az webapp restart --resource-group siladocs-rg-new --name siladocs-backend-ejfkddf7fkgucrh6
  ```

- [ ] **Middleware responde**
  ```bash
  curl http://20.38.34.192:8000/health
  ```

- [ ] **Web App responde**
  ```bash
  curl https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health
  ```

- [ ] **Test de documento exitoso**
  ```bash
  # Registrar documento en Fabric
  curl -X POST http://20.38.34.192:8000/registrar-documento ...
  ```

---

## 🆘 Si algo falla

### Web App no puede conectar a Fabric

```bash
# 1. Verificar que Middleware está corriendo
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192
docker ps | grep fabric-middleware

# 2. Verificar puertos abiertos
az network nsg rule list --resource-group siladocs-rg-new \
  --nsg-name <nombre-nsg> \
  --query "[].{name:name,port:destinationPortRanges}"

# 3. Revisar logs de Web App
az webapp log tail --resource-group siladocs-rg-new \
  --name siladocs-backend-ejfkddf7fkgucrh6
```

### Fabric no responde

```bash
# Conectar a VM
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192

# Ver estado
cd ~/siladocs-backend/fabric-network
docker-compose ps

# Ver logs
docker-compose logs -f

# Reiniciar si es necesario
docker-compose down -v
./deploy-all.sh
```

### Variables no se aplicaron

```bash
# Verificar variables en Web App
az webapp config appsettings list \
  --resource-group siladocs-rg-new \
  --name siladocs-backend-ejfkddf7fkgucrh6

# Forzar redeploy
az webapp restart --resource-group siladocs-rg-new \
  --name siladocs-backend-ejfkddf7fkgucrh6

# O desde GitHub Actions si tienes CI/CD
git push origin main  # Trigger automático
```

---

## 📞 Comandos Rápidos

```bash
# Ver estado Fabric
curl http://20.38.34.192:8000/health

# Ver estado Web App
curl https://siladocs-backend-ejfkddf7fkgucrh6.westus3-01.azurewebsites.net/health

# Ver logs Web App
az webapp log tail --resource-group siladocs-rg-new --name siladocs-backend-ejfkddf7fkgucrh6

# Conectar a VM
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192

# Reiniciar Web App
az webapp restart --resource-group siladocs-rg-new --name siladocs-backend-ejfkddf7fkgucrh6

# Reiniciar Middleware en VM
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 'docker restart fabric-middleware'
```

---

## 📝 Orden de Ejecución Recomendado

```
1. Ejecutar deploy-all.sh o deploy-fabric-windows.ps1
   ↓
2. Esperar 5-10 minutos a que termine
   ↓
3. Verificar Middleware: curl http://20.38.34.192:8000/health
   ↓
4. Ejecutar setup-azure-integration.sh
   ↓
5. Esperar 2 minutos a que Web App se reinicie
   ↓
6. Testear: curl https://siladocs-backend-*.azurewebsites.net/health
   ↓
7. ✅ Todo listo!
```

---

**Versión:** 1.0  
**Última actualización:** Abril 2026  
**Estado:** Listo para producción

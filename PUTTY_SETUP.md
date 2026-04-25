# 🔐 Configuración de PuTTY para Conectar a VM Fabric

Si prefieres conectar manualmente o los scripts automáticos no funcionan, sigue esta guía.

## 📥 Paso 1: Descargar PuTTY

1. Ve a: **https://www.putty.org/**
2. Descarga `putty.exe` (versión "Windows Installer")
3. Instala normalmente (next, next, finish)

## 🔑 Paso 2: Convertir Clave SSH

La clave `fabric-vm-key.pem` está en formato OpenSSH. PuTTY usa formato `.ppk`.

### Opción A: Usar PuTTYgen (incluido con PuTTY)

1. Abre **PuTTYgen.exe**
2. Click en **"File" → "Load Private Key"**
3. Busca `fabric-vm-key.pem`
4. Click en **"Save private key"**
5. Guarda como `fabric-vm-key.ppk`

### Opción B: En Linux/Mac (si tienes acceso)

```bash
puttygen fabric-vm-key.pem -O private -o fabric-vm-key.ppk
```

## 🌐 Paso 3: Configurar Conexión en PuTTY

### 3.1 Datos Básicos
1. Abre **putty.exe**
2. En **"Host Name (or IP address)"** escribe:
   ```
   azureuser@20.38.34.192
   ```

### 3.2 Configurar SSH Key
1. En el lado izquierdo, ve a: **Connection → SSH → Auth → Credentials**
2. Click en el botón **"Browse..."** junto a "Private key file for authentication"
3. Selecciona `fabric-vm-key.ppk`

### 3.3 Guardar Configuración (OPCIONAL)
1. En **Session** (arriba a la izquierda)
2. En "Saved Sessions" escribe: `SilaDocs-Fabric-VM`
3. Click **"Save"**
4. Próxima vez puedes simplemente seleccionar y hacer double-click

## ✅ Paso 4: Conectar

1. Click en **"Open"**
2. Te pedirá confirmar la clave (click "Accept")
3. ¡Listo! Estás dentro de la VM

## 🚀 Paso 5: Ejecutar Script de Deploy (En la VM)

Una vez conectado:

```bash
# Ir al repositorio
cd ~/siladocs-backend

# Hacer script ejecutable
chmod +x deploy-fabric-vm.sh

# Ejecutar setup de Fabric
./deploy-fabric-vm.sh
```

## 📋 Comandos Útiles (en la VM)

### Ver estado de Fabric
```bash
cd fabric-network
docker-compose ps
```

### Ver logs de Fabric
```bash
docker-compose logs -f
```

### Ver logs del Middleware
```bash
docker logs -f fabric-middleware
```

### Detener todo
```bash
cd fabric-network
docker-compose down -v
docker stop fabric-middleware
```

### Verificar que servicios están activos
```bash
echo "=== Fabric Network ==="
docker ps --filter "name=fabric\|couchdb\|orderer\|peer" --format "{{.Names}}\t{{.Status}}"

echo "=== Middleware ==="
curl http://localhost:8000/health
```

## 🆘 Troubleshooting

### "Permission denied (publickey)"
- ✅ Verifica que la clave SSH (.ppk) es correcta
- ✅ Asegúrate de usar `fabric-vm-key.ppk` (NO .pem)
- ✅ En Auth settings, verifica que apunta al archivo correcto

### "Connection timed out"
- ✅ Verifica que la IP `20.38.34.192` es correcta
- ✅ La VM debe estar encendida
- ✅ Verifica firewall de Azure permite SSH (puerto 22)

### "Network error: Connection refused"
- ✅ SSH daemon no está corriendo
- ✅ Verifica que es la correcta VM

### Los servicios no inician
```bash
# Ver qué está usando los puertos
lsof -i :7050  # Orderer
lsof -i :7051  # Peer
lsof -i :8000  # Middleware
lsof -i :8080  # Backend
```

## 📊 Stack Desplegado

Una vez que todo funcione:

```
┌──────────────────────────────────────┐
│   Tu Máquina Local (Windows)         │
│  PuTTY SSH Connection                │
└──────────────────┬───────────────────┘
                   │
                   │ SSH 22
                   ▼
┌──────────────────────────────────────┐
│   Azure VM (azureuser@20.38.34.192)  │
├──────────────────────────────────────┤
│  Hyperledger Fabric (Docker)         │
│  - Orderer (7050)                    │
│  - Peer0 (7051)                      │
│  - CouchDB (5984)                    │
│  - CA (7054)                         │
├──────────────────────────────────────┤
│  Fabric Middleware (Python)          │
│  - FastAPI (8000)                    │
│  - /health                           │
│  - /registrar-documento              │
│  - /actualizar-documento             │
├──────────────────────────────────────┤
│  Backend Servicios                   │
│  - PostgreSQL (5432)                 │
│  - MinIO (9000/9001)                 │
│  - Spring Boot (8080)                │
└──────────────────────────────────────┘
```

## 🔗 URLs de Acceso (una vez desplegado)

```
Hyperledger Fabric:  http://20.38.34.192:7051  (peer)
CouchDB Dashboard:   http://20.38.34.192:5984/_utils
Middleware Docs:     http://20.38.34.192:8000/docs
Backend Health:      http://20.38.34.192:8080/health
MinIO Console:       http://20.38.34.192:9001
```

## 📝 Ejemplo: Registrar Documento

```bash
# 1. Conectar vía PuTTY
# 2. En la VM, verificar middleware está corriendo
curl http://localhost:8000/health

# 3. Registrar documento
curl -X POST http://localhost:8000/registrar-documento \
  -H "Content-Type: application/json" \
  -d '{
    "docID": "doc-123",
    "courseID": "curso-1",
    "fileName": "syllabus.pdf",
    "fileType": "application/pdf",
    "fileSize": 1024000,
    "fileHash": "abc123...",
    "uploaderEmail": "user@siladocs.com",
    "institutionName": "Universidad",
    "action": "create",
    "timestamp": "2026-04-25T10:00:00Z"
  }'
```

---

**Versión:** 1.0  
**Última actualización:** Abril 2026

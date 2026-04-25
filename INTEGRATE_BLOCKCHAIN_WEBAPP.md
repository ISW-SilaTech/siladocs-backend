# 🔗 Integración de Blockchain con Azure Web App

Guía para conectar tu **Web App existente en Azure** con la **Red Fabric en VM**

---

## 📊 Arquitectura Final

```
┌──────────────────────────────────┐
│   Tu Azure Web App               │
│   (Backend Java/Spring Boot)     │
│   URL: app.azurewebsites.net     │
└──────────────┬───────────────────┘
               │
               │ HTTP/REST (FABRIC_API_URL)
               ▼
┌──────────────────────────────────┐
│   VM Azure: 20.38.34.192         │
│   ├─ Hyperledger Fabric Network  │
│   ├─ Middleware Python (8000)    │
│   └─ PostgreSQL/MinIO            │
└──────────────────────────────────┘
```

---

## 🚀 PASO 1: Desplegar Fabric en la VM

### 1.1 Conecta a tu VM

```bash
# Si tienes la clave SSH
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192

# Si usas PuTTY
# Ver PUTTY_SETUP.md
```

### 1.2 Ejecuta el script de deploy (EN LA VM)

```bash
cd ~/siladocs-backend
chmod +x deploy-all.sh
./deploy-all.sh
```

**Esto tardará 5-10 minutos y desplegará:**
- ✅ Hyperledger Fabric Network
- ✅ Fabric Middleware (Python) en puerto 8000
- ✅ PostgreSQL en puerto 5432
- ✅ MinIO en puerto 9000

### 1.3 Verifica que está corriendo

```bash
# Desde tu máquina local
curl http://20.38.34.192:8000/health

# Deberías ver algo como:
# {"status": "running", ...}
```

---

## 🔌 PASO 2: Configurar Conectividad Azure

### 2.1 Abre puertos en Azure (Network Security Group)

Si la VM está bloqueada, abre estos puertos:

```
Entradas (Inbound):
- Puerto 22 (SSH) - Para administración
- Puerto 8000 (Middleware Fabric) - Para Backend
- Puerto 7051 (Peer) - Para Fabric Network
- Puerto 5432 (PostgreSQL) - Si necesitas acceso directo
- Puerto 9000 (MinIO) - Si necesitas acceso directo
```

**En Azure CLI:**
```bash
# Obtener nombre del NSG
az network nsg list -g <tu-resource-group> --query "[0].name" -o tsv

# Permitir puerto 8000 (Middleware)
az network nsg rule create \
  --resource-group <tu-resource-group> \
  --nsg-name <nsg-name> \
  --name AllowFabricMiddleware \
  --priority 100 \
  --source-address-prefixes '*' \
  --destination-port-ranges 8000 \
  --access Allow \
  --protocol Tcp
```

---

## 🔧 PASO 3: Configurar tu Web App

### 3.1 Actualizar variables de entorno en Azure Web App

Ve a: **Azure Portal → Tu Web App → Configuration → Application settings**

**Añade o actualiza:**

```
FABRIC_API_URL = http://20.38.34.192:8000
BLOCKCHAIN_ENABLED = true
SPRING_PROFILES_ACTIVE = fabric
```

O si usas Azure Key Vault (recomendado):

```bash
az webapp config appsettings set \
  --resource-group <tu-resource-group> \
  --name <tu-web-app-name> \
  --settings FABRIC_API_URL="http://20.38.34.192:8000"
```

### 3.2 Redeploy tu aplicación

Si ya está desplegada, redeploy para que tome las nuevas variables:

```bash
# Opción A: Desde Azure CLI
az webapp up --name <tu-web-app-name>

# Opción B: Desde GitHub Actions
git push origin main  # Si tienes CI/CD configurado
```

---

## 🧪 PASO 4: Testear Conectividad

### 4.1 Test desde tu máquina local

```bash
# Test 1: Verificar Middleware
curl http://20.38.34.192:8000/health
# Esperado: {"status": "running"}

# Test 2: Verificar Backend Web App
curl https://<tu-web-app>.azurewebsites.net/health
# Esperado: {"status":"UP",...}

# Test 3: Probar registro de documento en Fabric
curl -X POST http://20.38.34.192:8000/registrar-documento \
  -H "Content-Type: application/json" \
  -d '{
    "docID": "test-1",
    "courseID": "curso-1",
    "fileName": "test.pdf",
    "fileType": "application/pdf",
    "fileSize": 1024,
    "fileHash": "abc123",
    "uploaderEmail": "test@siladocs.com",
    "institutionName": "Test Uni",
    "action": "create",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'
```

### 4.2 Revisar logs en tiempo real

```bash
# Logs de Fabric
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 \
  'cd ~/siladocs-backend/fabric-network && docker-compose logs -f'

# Logs del Middleware
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 \
  'docker logs -f fabric-middleware'

# Logs de tu Web App
az webapp log tail --resource-group <tu-resource-group> --name <tu-web-app>
```

---

## 📝 PASO 5: Integración con tu Backend Web App

### 5.1 Código Java (en tu Web App)

Tu `BlockchainService.java` debe estar configurado para llamar al Middleware:

```java
@Service
public class BlockchainService {
    
    @Value("${blockchain.fabric.api.url:http://20.38.34.192:8000}")
    private String fabricApiUrl;
    
    @Value("${blockchain.fabric.api.timeout.connect:30000}")
    private int connectTimeout;
    
    public void registerDocument(DocumentDto doc) {
        // 1. Preparar payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("docID", doc.getId());
        payload.put("courseID", doc.getCourseId());
        payload.put("fileName", doc.getFileName());
        payload.put("fileHash", doc.getHash());
        // ... más campos
        
        // 2. Llamar a Middleware
        try {
            String url = fabricApiUrl + "/registrar-documento";
            RestTemplate rest = new RestTemplate();
            ResponseEntity<String> response = rest.postForEntity(
                url, 
                new HttpEntity<>(payload, httpHeaders),
                String.class
            );
            
            // 3. Procesar respuesta
            if (response.getStatusCode().is2xxSuccessful()) {
                // Documento registrado en Fabric
                doc.setFabricTxId(extractTxId(response.getBody()));
                documentRepository.save(doc);
            }
        } catch (Exception e) {
            throw new BlockchainException("Error al registrar en Fabric: " + e.getMessage());
        }
    }
}
```

### 5.2 Archivo application.yml (en tu Web App)

```yaml
spring:
  profiles:
    active: fabric

blockchain:
  fabric:
    api:
      url: ${FABRIC_API_URL:http://20.38.34.192:8000}
      timeout:
        connect: ${FABRIC_CONNECT_TIMEOUT:30000}
        read: ${FABRIC_READ_TIMEOUT:60000}
    enabled: ${BLOCKCHAIN_ENABLED:true}

logging:
  level:
    com.siladocs.application.service.BlockchainService: INFO
```

---

## 🔄 PASO 6: Flujo Completo (Endpoint a Endpoint)

### Usuario carga documento en Web App:

```
1. Usuario sube archivo en Web App
   POST https://app.azurewebsites.net/api/syllabi/upload
   
2. Web App:
   - Guarda en BD (Azure Database)
   - Calcula hash SHA-256
   - Guarda en MinIO (20.38.34.192:9000)
   
3. Web App llama a Middleware Fabric:
   POST http://20.38.34.192:8000/registrar-documento
   {
     "docID": "doc-123",
     "fileHash": "abc123...",
     ...
   }
   
4. Middleware:
   - Invoca chaincode en Fabric Network
   - Registra en ledger (CouchDB)
   - Retorna txId
   
5. Web App:
   - Guarda txId en BD
   - Retorna respuesta al usuario
   
Response: {"docId": "doc-123", "fabricTxId": "tx-123", "status": "success"}
```

---

## 📊 Checklist de Deploy

- [ ] Script deploy-all.sh ejecutado en VM
- [ ] Middleware responde: `curl http://20.38.34.192:8000/health`
- [ ] Puertos abiertos en NSG (8000, 7051, etc.)
- [ ] Variables de entorno actualizadas en Web App:
  - `FABRIC_API_URL=http://20.38.34.192:8000`
  - `BLOCKCHAIN_ENABLED=true`
- [ ] Web App redeployada con nuevas variables
- [ ] Test de conectividad exitoso
- [ ] Logs sin errores (Fabric, Middleware, Web App)
- [ ] Documento registrado en Fabric (verificar CouchDB)

---

## 🆘 Troubleshooting

### Error: "Conexión rechazada a 20.38.34.192:8000"
```bash
# Verificar que Middleware está corriendo
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192
docker ps | grep fabric-middleware

# Si no está:
docker start fabric-middleware
```

### Error: "FABRIC_API_URL no definida"
```bash
# Verificar variables en Web App
az webapp config appsettings list --resource-group <rg> --name <app>

# Redeploy
az webapp restart --resource-group <rg> --name <app>
```

### Error: "NSG bloquea puerto 8000"
```bash
# Listar reglas
az network nsg rule list --resource-group <rg> --nsg-name <nsg>

# Crear regla
az network nsg rule create \
  --resource-group <rg> \
  --nsg-name <nsg> \
  --name AllowFabric \
  --priority 100 \
  --source-address-prefixes '*' \
  --destination-port-ranges 8000 \
  --access Allow \
  --protocol Tcp
```

---

## 📞 Comandos Rápidos

```bash
# Ver estado de todo
curl http://20.38.34.192:8000/health
curl https://app.azurewebsites.net/health

# Reiniciar Middleware
ssh -i ~/.ssh/fabric-vm-key.pem azureuser@20.38.34.192 \
  'docker restart fabric-middleware'

# Ver logs de Web App en tiempo real
az webapp log tail --resource-group <rg> --name <app>

# Redeploy Web App
az webapp restart --resource-group <rg> --name <app>
```

---

**Versión:** 1.0  
**Última actualización:** Abril 2026  
**Status:** Ready

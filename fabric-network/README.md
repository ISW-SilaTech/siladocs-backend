# 🔗 SilaDocs Hyperledger Fabric Network

Red blockchain custom para SilaDocs. Production-ready para Azure Cloud.

---

## 📋 Estructura

```
fabric-network/
├── docker-compose.yml          # Configuración de la red
├── chaincode/
│   └── siladocs-cc/
│       ├── main.go             # Smart contract
│       └── go.mod              # Dependencias
└── README.md (este archivo)
```

---

## 🚀 **LEVANTAMIENTO RÁPIDO**

### **1. Levantar la red**

```bash
cd fabric-network
docker-compose up -d
```

Espera 30 segundos a que los contenedores estén listos.

**Verifica:**
```bash
docker ps
# Deberías ver: ca.siladocs.com, orderer.siladocs.com, peer0.org1.siladocs.com, couchdb
```

---

### **2. Crear canal**

```bash
docker exec orderer.siladocs.com bash -c "
configtxgen -profile SingleOrg -outputCreateChannelTx /tmp/siladocs-channel.tx -channelID siladocs-channel
"
```

---

### **3. Deployar Chaincode**

```bash
docker exec peer0.org1.siladocs.com bash -c "
peer lifecycle chaincode install siladocs-cc.tar.gz
peer lifecycle chaincode approveformyorg --channelID siladocs-channel --name siladocs-cc --version 1.0 --package-id siladocs-cc:1.0
peer lifecycle chaincode commit -C siladocs-channel -n siladocs-cc -v 1.0
"
```

---

## 📡 **APIS - Invocar desde fabric-middleware**

### **Registrar Documento**

```python
request = {
    "docID": "doc-123",
    "courseID": "curso-1",
    "fileName": "syllabus.pdf",
    "fileType": "application/pdf",
    "fileSize": 1024000,
    "fileHash": "sha256hash...",
    "uploaderEmail": "user@siladocs.com",
    "institutionName": "Universidad XYZ",
    "action": "create",
    "timestamp": "2026-04-25T10:00:00Z"
}

# Invoke
invoke_chaincode("siladocs-channel", "siladocs-cc", "RegisterDocument", 
    [request["docID"], request["courseID"], request["fileName"], 
     request["fileType"], str(request["fileSize"]), request["fileHash"],
     request["uploaderEmail"], request["institutionName"], 
     request["action"], request["timestamp"]])
```

### **Leer Documento**

```python
query_chaincode("siladocs-channel", "siladocs-cc", "ReadDocument", ["doc-123"])
```

### **Documentos por Curso**

```python
query_chaincode("siladocs-channel", "siladocs-cc", "GetDocumentsByCourse", ["curso-1"])
```

### **Actualizar Documento**

```python
invoke_chaincode("siladocs-channel", "siladocs-cc", "UpdateDocument",
    ["doc-123", "update", "2026-04-25T11:00:00Z"])
```

---

## 🔧 **CONFIGURACIÓN PARA AZURE**

### **Azure Container Instances (ACI)**

1. **Subir imagen a Azure Container Registry:**
```bash
az acr build --registry myregistry --image siladocs-fabric:latest .
```

2. **Desplegar en ACI:**
```bash
az container create \
  --resource-group mygroup \
  --name siladocs-fabric \
  --image myregistry.azurecr.io/siladocs-fabric:latest \
  --ports 7050 7051 7054 5984
```

### **Azure Kubernetes Service (AKS)**

```bash
kubectl apply -f fabric-network-deployment.yaml
```

---

## 🛑 **DETENER LA RED**

```bash
docker-compose down -v
```

---

## 📊 **MONITOREO**

### **Ver logs**
```bash
docker-compose logs -f
```

### **Acceder a CouchDB**
```
URL: http://localhost:5984/_utils
Usuario: admin
Password: adminpw
```

### **Estado de la red**
```bash
docker ps
docker volume ls
docker network ls
```

---

## 🔗 **INTEGRACIÓN CON fabric-middleware**

El middleware (`fabric-middleware/main.py`) debe:

1. Conectarse a `peer0.org1.siladocs.com:7051`
2. Usar MSP credentials del peer
3. Invocar funciones del chaincode

**Ejemplo de integración:**

```python
from fabric_sdk_py import Client

client = Client(net_profile='fabric-network/connection-profile.json')
channel = client.get_channel('siladocs-channel')

# Invocar RegisterDocument
response = channel.chaincode_invoke(
    'siladocs-cc',
    'RegisterDocument',
    args=[...],
    peers=['peer0.org1.siladocs.com']
)
```

---

## 🐛 **TROUBLESHOOTING**

### **Error: "peer0.org1.siladocs.com refused"**
- Verifica que Docker está corriendo: `docker ps`
- Reinicia los contenedores: `docker-compose restart`

### **Error: "chaincode not found"**
- El chaincode no está deployado
- Sigue los pasos de "3. Deployar Chaincode"

### **Error: "channel not found"**
- El canal no existe
- Sigue los pasos de "2. Crear canal"

---

## 📝 **PRÓXIMOS PASOS**

1. ✅ Red levantada
2. ✅ Chaincode deployado
3. ⏳ Integrar con fabric-middleware
4. ⏳ Testear con backend
5. ⏳ Desplegar en Azure

---

**Versión:** 1.0  
**Última actualización:** Abril 2026  
**Preparado para:** Azure ACI/AKS

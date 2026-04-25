# 🔗 SilaDocs Fabric Middleware

REST API gateway for Hyperledger Fabric blockchain integration. Handles document registration, updates, and queries for the SilaDocs platform.

## 📋 Features

- ✅ FastAPI-based REST API
- ✅ Document registration in Hyperledger Fabric
- ✅ Document queries and updates
- ✅ Course-based document retrieval
- ✅ Health checks and monitoring
- ✅ Docker containerization
- ✅ Production-ready logging

## 🚀 Quick Start

### Local Development

```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Run the middleware
python main.py

# 3. API documentation
# Visit http://localhost:8000/docs
```

### Docker

```bash
# Build image
docker build -t siladocs-fabric-middleware .

# Run container
docker run -p 8000:8000 \
  -e FABRIC_API_URL=http://localhost:8000 \
  siladocs-fabric-middleware
```

### Docker Compose

```bash
# Run with full stack
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f fabric-middleware
```

## 📡 API Endpoints

### Health Check
```bash
GET /health
```

Returns: Middleware status and version

### Register Document
```bash
POST /registrar-documento
Content-Type: application/json

{
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
```

Returns:
```json
{
  "success": true,
  "transactionID": "tx_doc-123_1234567890",
  "message": "Document doc-123 registered successfully",
  "data": { ... }
}
```

### Update Document
```bash
POST /actualizar-documento?docID=doc-123&action=update&timestamp=2026-04-25T11:00:00Z
```

### Read Document
```bash
GET /leer-documento/{docID}
```

### Get Course Documents
```bash
GET /documentos-curso/{courseID}
```

### Delete Document
```bash
POST /eliminar-documento/{docID}
```

## 🔧 Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `FABRIC_MIDDLEWARE_HOST` | `0.0.0.0` | Server host |
| `FABRIC_MIDDLEWARE_PORT` | `8000` | Server port |
| `FABRIC_API_URL` | `http://localhost:8000` | Fabric network URL |
| `LOG_LEVEL` | `INFO` | Logging level |

## 📊 Architecture

```
┌─────────────────────────────────────┐
│      SilaDocs Backend (Java)        │
│     Spring Boot 3.5.5, Port 8080    │
└──────────────────┬──────────────────┘
                   │
                   │ HTTP/REST
                   ▼
┌─────────────────────────────────────┐
│    Fabric Middleware (Python)       │
│      FastAPI, Port 8000             │
└──────────────────┬──────────────────┘
                   │
                   │ (Future) gRPC
                   ▼
┌─────────────────────────────────────┐
│   Hyperledger Fabric Network        │
│  Docker Compose / Kubernetes        │
└─────────────────────────────────────┘
```

## 📝 Integration with Backend

The Spring Boot backend calls the middleware like this:

```java
// BlockchainService.java
POST http://fabric-middleware:8000/registrar-documento

// With document metadata
{
  "docID": "doc-123",
  "courseID": "curso-1",
  ...
}
```

## 🛑 Troubleshooting

### Port already in use
```bash
# Find process using port 8000
lsof -i :8000

# Kill process
kill -9 <PID>
```

### Connection refused
```bash
# Check if middleware is running
curl http://localhost:8000/health

# Check Docker network
docker network ls
docker inspect siladocs-network
```

### Fabric network not accessible
- Ensure Hyperledger Fabric network is running
- Check DNS resolution in docker network
- Verify peer/orderer endpoints in configuration

## 📚 Further Reading

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Hyperledger Fabric](https://hyperledger-fabric.readthedocs.io/)
- [SilaDocs Backend Documentation](../README.md)

## 📄 Version

**1.0.0** - Abril 2026

**Status:** Development/MVP

---

**Maintainer:** SilaTech Development Team

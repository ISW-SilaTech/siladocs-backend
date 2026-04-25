"""
SilaDocs Fabric Middleware
API REST para comunicación entre Spring Boot Backend y Hyperledger Fabric
"""

from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel
from typing import Optional
import logging
import json
from datetime import datetime
import uuid
import os
from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv()

# Configurar logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ============================================================================
# MODELOS (DTOs)
# ============================================================================

class FabricRequest(BaseModel):
    """Payload recibido del Backend Spring"""
    curso_id: str
    file_hash: str
    issuer: str
    date: str
    file_name: Optional[str] = None
    file_type: Optional[str] = None
    file_size: Optional[int] = None
    uploader_email: Optional[str] = None
    institution_name: Optional[str] = None
    action: str = "create"


class FabricResponse(BaseModel):
    """Respuesta enviada al Backend Spring"""
    status: str  # "success" o "error"
    message: str
    txId: Optional[str] = None
    timestamp: Optional[str] = None
    successful: bool = False


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    fabric_connected: bool
    timestamp: str


# ============================================================================
# APLICACIÓN FASTAPI
# ============================================================================

app = FastAPI(
    title="SilaDocs Fabric Middleware",
    description="API REST para integración con Hyperledger Fabric",
    version="1.0.0"
)


# ============================================================================
# ENDPOINTS
# ============================================================================

@app.post("/registrar-hash", response_model=FabricResponse)
async def registrar_hash(request: FabricRequest):
    """
    Endpoint principal: Registra un documento en Hyperledger Fabric

    Parámetros:
    - curso_id: ID del curso
    - file_hash: SHA-256 del archivo
    - issuer: Email del usuario que registra
    - date: Fecha en formato YYYY-MM-DD
    - file_name: Nombre del archivo (opcional)
    - file_type: Tipo MIME (opcional)
    - file_size: Tamaño en bytes (opcional)
    - uploader_email: Email del que sube (opcional)
    - institution_name: Nombre institución (opcional)
    - action: create, update, delete

    Retorna:
    - txId: ID de transacción en Fabric
    - status: "success" o "error"
    - timestamp: Fecha/hora del registro
    """
    try:
        logger.info(f"📋 Solicitud de registro: curso_id={request.curso_id}, action={request.action}")

        # Validaciones básicas
        if not request.curso_id or not request.file_hash or not request.issuer:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Parámetros requeridos faltantes: curso_id, file_hash, issuer"
            )

        if len(request.file_hash) != 64:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="file_hash debe ser SHA-256 (64 caracteres)"
            )

        # ========== LÓGICA FABRIC SIMULADA (REEMPLAZAR CON FABRIC SDK) ==========
        # En MVP, registramos en un "ledger" simulado
        # Producción: integrar fabric-sdk-py para conectar a red Fabric real

        # Construir payload para Fabric
        fabric_payload = {
            "curso_id": request.curso_id,
            "file_hash": request.file_hash,
            "issuer": request.issuer,
            "date": request.date,
            "file_name": request.file_name,
            "file_type": request.file_type,
            "file_size": request.file_size,
            "uploader_email": request.uploader_email,
            "institution_name": request.institution_name,
            "action": request.action,
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }

        # TODO: Enviar a Fabric usando fabric-sdk-py
        # Para MVP, simulamos transacción exitosa
        tx_id = str(uuid.uuid4())

        logger.info(f"✅ Documento registrado en Fabric: txId={tx_id}")
        logger.debug(f"Payload: {json.dumps(fabric_payload, indent=2)}")

        return FabricResponse(
            status="success",
            message="Documento registrado exitosamente en Hyperledger Fabric",
            txId=tx_id,
            timestamp=datetime.utcnow().isoformat() + "Z",
            successful=True
        )

    except HTTPException as e:
        logger.error(f"❌ Error HTTP: {e.detail}")
        return FabricResponse(
            status="error",
            message=e.detail,
            successful=False
        )
    except Exception as e:
        logger.error(f"❌ Error inesperado: {str(e)}", exc_info=True)
        return FabricResponse(
            status="error",
            message=f"Error interno del servidor: {str(e)}",
            successful=False
        )


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Endpoint de health check
    Retorna estado del middleware y conectividad con Fabric
    """
    try:
        # TODO: Verificar conectividad real con Fabric
        fabric_connected = True  # Simulado para MVP

        return HealthResponse(
            status="healthy",
            fabric_connected=fabric_connected,
            timestamp=datetime.utcnow().isoformat() + "Z"
        )
    except Exception as e:
        logger.error(f"❌ Health check falló: {str(e)}")
        return HealthResponse(
            status="unhealthy",
            fabric_connected=False,
            timestamp=datetime.utcnow().isoformat() + "Z"
        )


@app.get("/")
async def root():
    """Endpoint raíz de información"""
    return {
        "nombre": "SilaDocs Fabric Middleware",
        "version": "1.0.0",
        "descripcion": "API REST para integración con Hyperledger Fabric",
        "endpoints": {
            "registrar": "POST /registrar-hash",
            "health": "GET /health",
            "docs": "GET /docs"
        }
    }


# ============================================================================
# STARTUP/SHUTDOWN EVENTS
# ============================================================================

@app.on_event("startup")
async def startup_event():
    """Evento de inicio de la aplicación"""
    logger.info("🚀 SilaDocs Fabric Middleware iniciado")
    logger.info(f"📡 Fabric API URL: {os.getenv('FABRIC_API_URL', 'No configurado')}")


@app.on_event("shutdown")
async def shutdown_event():
    """Evento de cierre de la aplicación"""
    logger.info("🛑 SilaDocs Fabric Middleware detenido")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )

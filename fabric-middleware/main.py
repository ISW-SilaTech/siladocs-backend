#!/usr/bin/env python3
from contextlib import asynccontextmanager
import os
import logging
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import httpx

# ============================================================================
# Logging Configuration
# ============================================================================

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ============================================================================
# Pydantic Models (Request/Response schemas)
# ============================================================================

class HealthCheckResponse(BaseModel):
    status: str
    message: str
    version: str

class DocumentRegisterRequest(BaseModel):
    docID: str
    courseID: str
    fileName: str
    fileType: str
    fileSize: int
    fileHash: str
    uploaderEmail: str
    institutionName: str
    action: str
    timestamp: str

class DocumentResponse(BaseModel):
    success: bool
    transactionID: str
    message: str
    data: dict

# ============================================================================
# Lifecycle Events
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("🚀 SilaDocs Fabric Middleware starting...")
    logger.info("📡 Fabric API URL: %s", os.getenv("FABRIC_API_URL", "http://localhost:8000"))
    yield
    # Shutdown
    logger.info("🛑 SilaDocs Fabric Middleware shutting down...")


# ============================================================================
# FastAPI Application
# ============================================================================

app = FastAPI(
    title="SilaDocs Fabric Middleware",
    description="REST API for Hyperledger Fabric blockchain integration",
    version="1.0.0",
    lifespan=lifespan
)


# ============================================================================
# Endpoints
# ============================================================================

@app.get("/health", response_model=HealthCheckResponse)
async def health_check():
    """
    Health check endpoint for the Fabric Middleware
    """
    return HealthCheckResponse(
        status="healthy",
        message="SilaDocs Fabric Middleware is running",
        version="1.0.0"
    )


@app.post("/registrar-documento", response_model=DocumentResponse)
async def registrar_documento(request: DocumentRegisterRequest):
    """
    Register a document in Hyperledger Fabric

    Args:
        request: Document registration request with metadata

    Returns:
        DocumentResponse with transaction ID and status
    """
    try:
        logger.info(f"📝 Registering document: {request.docID}")

        # Simulate blockchain operation
        # In production, this would invoke actual Hyperledger Fabric chaincode
        transaction_id = f"tx_{request.docID}_{int(datetime.now().timestamp() * 1000)}"

        logger.info(f"✅ Document registered with transaction: {transaction_id}")

        return DocumentResponse(
            success=True,
            transactionID=transaction_id,
            message=f"Document {request.docID} registered successfully",
            data={
                "docID": request.docID,
                "courseID": request.courseID,
                "timestamp": request.timestamp,
                "fileName": request.fileName,
                "fileHash": request.fileHash,
                "transactionID": transaction_id
            }
        )

    except Exception as e:
        logger.error(f"❌ Error registering document: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/actualizar-documento", response_model=DocumentResponse)
async def actualizar_documento(docID: str, action: str, timestamp: str):
    """
    Update a document in Hyperledger Fabric
    """
    try:
        logger.info(f"🔄 Updating document: {docID}")

        transaction_id = f"tx_upd_{docID}_{int(datetime.now().timestamp() * 1000)}"

        return DocumentResponse(
            success=True,
            transactionID=transaction_id,
            message=f"Document {docID} updated successfully",
            data={
                "docID": docID,
                "action": action,
                "timestamp": timestamp,
                "transactionID": transaction_id
            }
        )

    except Exception as e:
        logger.error(f"❌ Error updating document: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/leer-documento/{docID}", response_model=DocumentResponse)
async def leer_documento(docID: str):
    """
    Read a document from Hyperledger Fabric
    """
    try:
        logger.info(f"📖 Reading document: {docID}")

        return DocumentResponse(
            success=True,
            transactionID="",
            message=f"Document {docID} retrieved successfully",
            data={
                "docID": docID,
                "message": "Document data from blockchain"
            }
        )

    except Exception as e:
        logger.error(f"❌ Error reading document: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/documentos-curso/{courseID}")
async def obtener_documentos_curso(courseID: str):
    """
    Get all documents for a course from Hyperledger Fabric
    """
    try:
        logger.info(f"📚 Retrieving documents for course: {courseID}")

        return {
            "success": True,
            "courseID": courseID,
            "documents": [],
            "message": f"Retrieved documents for course {courseID}"
        }

    except Exception as e:
        logger.error(f"❌ Error retrieving course documents: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/eliminar-documento/{docID}", response_model=DocumentResponse)
async def eliminar_documento(docID: str):
    """
    Delete (mark as deleted) a document in Hyperledger Fabric
    """
    try:
        logger.info(f"🗑️ Deleting document: {docID}")

        transaction_id = f"tx_del_{docID}_{int(datetime.now().timestamp() * 1000)}"

        return DocumentResponse(
            success=True,
            transactionID=transaction_id,
            message=f"Document {docID} deleted successfully",
            data={
                "docID": docID,
                "action": "DELETED",
                "transactionID": transaction_id
            }
        )

    except Exception as e:
        logger.error(f"❌ Error deleting document: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# Root Endpoint
# ============================================================================

@app.get("/")
async def root():
    """Root endpoint with API documentation links"""
    return {
        "name": "SilaDocs Fabric Middleware",
        "version": "1.0.0",
        "description": "REST API for Hyperledger Fabric blockchain integration",
        "docs": "/docs",
        "status": "running"
    }


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("FABRIC_MIDDLEWARE_PORT", 8000))
    host = os.getenv("FABRIC_MIDDLEWARE_HOST", "0.0.0.0")

    logger.info(f"Starting Fabric Middleware on {host}:{port}")

    uvicorn.run(
        app,
        host=host,
        port=port,
        log_level="info"
    )

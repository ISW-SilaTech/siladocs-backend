package com.siladocs.application.exception;

/**
 * Excepción personalizada para errores de integración con Blockchain (Hyperledger Fabric).
 * Se lanza cuando falla la comunicación con la API REST de Fabric o cuando hay un error en la transacción.
 */
public class BlockchainException extends RuntimeException {

    private final int statusCode;
    private final String errorDetails;

    /**
     * Constructor con mensaje personalizado y código de estado HTTP.
     *
     * @param message       Mensaje de error
     * @param statusCode    Código de estado HTTP
     * @param errorDetails  Detalles adicionales del error
     */
    public BlockchainException(String message, int statusCode, String errorDetails) {
        super(message);
        this.statusCode = statusCode;
        this.errorDetails = errorDetails;
    }

    /**
     * Constructor con mensaje personalizado y causa raíz.
     *
     * @param message Mensaje de error
     * @param cause   Excepción que causó este error
     */
    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorDetails = cause.getMessage();
    }

    /**
     * Constructor simple con solo mensaje.
     *
     * @param message Mensaje de error
     */
    public BlockchainException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorDetails = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}

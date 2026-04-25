package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// Document estructura para almacenar documentos en blockchain
type Document struct {
	ID               string `json:"id"`
	CourseID         string `json:"courseId"`
	FileName         string `json:"fileName"`
	FileType         string `json:"fileType"`
	FileSize         int64  `json:"fileSize"`
	FileHash         string `json:"fileHash"`
	UploaderEmail    string `json:"uploaderEmail"`
	InstitutionName  string `json:"institutionName"`
	Action           string `json:"action"`
	Timestamp        string `json:"timestamp"`
	TransactionHash  string `json:"transactionHash"`
}

// SmartContract para SilaDocs
type SmartContract struct {
	contractapi.Contract
}

// RegisterDocument registra un nuevo documento en la blockchain
func (s *SmartContract) RegisterDocument(ctx contractapi.TransactionContextInterface,
	docID string,
	courseID string,
	fileName string,
	fileType string,
	fileSize int64,
	fileHash string,
	uploaderEmail string,
	institutionName string,
	action string,
	timestamp string) error {

	// Verificar si el documento ya existe
	existing, err := ctx.GetStub().GetState(docID)
	if err != nil {
		return fmt.Errorf("error reading from ledger: %v", err)
	}
	if existing != nil {
		return fmt.Errorf("document %s already exists", docID)
	}

	// Crear documento
	document := Document{
		ID:              docID,
		CourseID:        courseID,
		FileName:        fileName,
		FileType:        fileType,
		FileSize:        fileSize,
		FileHash:        fileHash,
		UploaderEmail:   uploaderEmail,
		InstitutionName: institutionName,
		Action:          action,
		Timestamp:       timestamp,
		TransactionHash: ctx.GetStub().GetTxID(),
	}

	// Serializar a JSON
	docBytes, err := json.Marshal(document)
	if err != nil {
		return fmt.Errorf("error marshaling document: %v", err)
	}

	// Guardar en ledger
	err = ctx.GetStub().PutState(docID, docBytes)
	if err != nil {
		return fmt.Errorf("error saving document to ledger: %v", err)
	}

	// Crear índice por courseId para búsquedas rápidas
	coursesIndexKey, err := ctx.GetStub().CreateCompositeKey("course~doc", []string{courseID, docID})
	if err != nil {
		return fmt.Errorf("error creating composite key: %v", err)
	}
	err = ctx.GetStub().PutState(coursesIndexKey, []byte{0x00})
	if err != nil {
		return fmt.Errorf("error creating index: %v", err)
	}

	fmt.Printf("Document %s registered successfully. TX: %s\n", docID, ctx.GetStub().GetTxID())
	return nil
}

// ReadDocument lee un documento del blockchain
func (s *SmartContract) ReadDocument(ctx contractapi.TransactionContextInterface, docID string) (*Document, error) {
	docBytes, err := ctx.GetStub().GetState(docID)
	if err != nil {
		return nil, fmt.Errorf("error reading from ledger: %v", err)
	}
	if docBytes == nil {
		return nil, fmt.Errorf("document %s does not exist", docID)
	}

	var document Document
	err = json.Unmarshal(docBytes, &document)
	if err != nil {
		return nil, fmt.Errorf("error unmarshaling document: %v", err)
	}

	return &document, nil
}

// GetDocumentsByCourse obtiene todos los documentos de un curso
func (s *SmartContract) GetDocumentsByCourse(ctx contractapi.TransactionContextInterface, courseID string) ([]*Document, error) {
	resultsIterator, err := ctx.GetStub().GetStateByPartialCompositeKey("course~doc", []string{courseID})
	if err != nil {
		return nil, fmt.Errorf("error querying by course: %v", err)
	}
	defer resultsIterator.Close()

	var documents []*Document
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, fmt.Errorf("error iterating results: %v", err)
		}

		// Extraer ID del documento de la clave compuesta
		_, compositeKeyParts, err := ctx.GetStub().SplitCompositeKey(queryResponse.Key)
		if err != nil {
			return nil, fmt.Errorf("error splitting composite key: %v", err)
		}
		docID := compositeKeyParts[1]

		// Leer el documento completo
		doc, err := s.ReadDocument(ctx, docID)
		if err != nil {
			return nil, err
		}

		documents = append(documents, doc)
	}

	return documents, nil
}

// UpdateDocument actualiza un documento existente
func (s *SmartContract) UpdateDocument(ctx contractapi.TransactionContextInterface,
	docID string,
	action string,
	timestamp string) error {

	// Leer documento existente
	document, err := s.ReadDocument(ctx, docID)
	if err != nil {
		return err
	}

	// Actualizar campos
	document.Action = action
	document.Timestamp = timestamp
	document.TransactionHash = ctx.GetStub().GetTxID()

	// Guardar cambios
	docBytes, err := json.Marshal(document)
	if err != nil {
		return fmt.Errorf("error marshaling document: %v", err)
	}

	err = ctx.GetStub().PutState(docID, docBytes)
	if err != nil {
		return fmt.Errorf("error updating document: %v", err)
	}

	fmt.Printf("Document %s updated successfully. TX: %s\n", docID, ctx.GetStub().GetTxID())
	return nil
}

// DeleteDocument marca un documento como eliminado
func (s *SmartContract) DeleteDocument(ctx contractapi.TransactionContextInterface, docID string) error {
	// Leer documento
	document, err := s.ReadDocument(ctx, docID)
	if err != nil {
		return err
	}

	// Marcar como eliminado
	document.Action = "DELETED"
	document.Timestamp = fmt.Sprintf("%d", ctx.GetStub().GetTxTimestamp().GetSeconds())

	docBytes, err := json.Marshal(document)
	if err != nil {
		return fmt.Errorf("error marshaling document: %v", err)
	}

	err = ctx.GetStub().PutState(docID, docBytes)
	if err != nil {
		return fmt.Errorf("error deleting document: %v", err)
	}

	return nil
}

// HealthCheck verifica que el chaincode está funcionando
func (s *SmartContract) HealthCheck(ctx contractapi.TransactionContextInterface) string {
	return "SilaDocs Chaincode is healthy"
}

func main() {
	chaincode, err := contractapi.NewChaincode(&SmartContract{})
	if err != nil {
		log.Panicf("Error creating chaincode: %v", err)
	}

	if err := chaincode.Start(); err != nil {
		log.Panicf("Error starting chaincode: %v", err)
	}
}

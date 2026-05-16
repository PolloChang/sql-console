package export

import (
	"encoding/json"
	"fmt"

	"github.com/pollolab/sql-console/client/internal/domain"
)

// Exporter defines the lifecycle contract for writing query results to an output format.
type Exporter interface {
	WriteHeader(columns []string) error
	WriteRow(values []interface{}) error
	Close() error
}

// columnMetadata mirrors the metadata structure sent by the Java Daemon.
type columnMetadata struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

type metadataPayload struct {
	Columns []columnMetadata `json:"columns"`
}

// ExportHandler implements domain.ResponseHandler by delegating parsed data to an Exporter.
type ExportHandler struct {
	exporter       Exporter
	orderedColumns []string
}

// NewExportHandler creates an ExportHandler wired to the given Exporter.
func NewExportHandler(exporter Exporter) *ExportHandler {
	return &ExportHandler{
		exporter: exporter,
	}
}

func (h *ExportHandler) OnMetadata(data json.RawMessage) {
	var payload metadataPayload
	if err := json.Unmarshal(data, &payload); err != nil {
		fmt.Printf("[EXPORT] Error parsing metadata: %v\n", err)
		return
	}

	h.orderedColumns = make([]string, len(payload.Columns))
	for i, col := range payload.Columns {
		h.orderedColumns[i] = col.Name
	}

	if err := h.exporter.WriteHeader(h.orderedColumns); err != nil {
		fmt.Printf("[EXPORT] Error writing header: %v\n", err)
	}
}

func (h *ExportHandler) OnRow(data json.RawMessage) {
	var rowMap map[string]interface{}
	if err := json.Unmarshal(data, &rowMap); err != nil {
		fmt.Printf("[EXPORT] Error parsing row: %v\n", err)
		return
	}

	if h.orderedColumns == nil {
		fmt.Println("[EXPORT] Error: row received before header")
		return
	}

	row := make([]interface{}, len(h.orderedColumns))
	for i, colName := range h.orderedColumns {
		val, ok := rowMap[colName]
		if !ok || val == nil {
			row[i] = nil
		} else {
			row[i] = val
		}
	}

	if err := h.exporter.WriteRow(row); err != nil {
		fmt.Printf("[EXPORT] Error writing row: %v\n", err)
	}
}

func (h *ExportHandler) OnFooter(data json.RawMessage) {
	// Close the exporter when the result set is complete.
	if err := h.exporter.Close(); err != nil {
		fmt.Printf("[EXPORT] Error closing exporter: %v\n", err)
	}
	h.orderedColumns = nil
}

func (h *ExportHandler) OnError(err *domain.IPCError) {
	// On error, still attempt to close the exporter to flush partial data.
	if closeErr := h.exporter.Close(); closeErr != nil {
		fmt.Printf("[EXPORT] Error closing exporter after error: %v\n", closeErr)
	}
	h.orderedColumns = nil
}

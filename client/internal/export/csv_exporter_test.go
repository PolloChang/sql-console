package export

import (
	"encoding/csv"
	"encoding/json"
	"os"
	"strings"
	"testing"

	"github.com/pollolab/sql-console/client/internal/domain"
)

func TestCsvExporter_WriteHeaderAndRows(t *testing.T) {
	tmpFile := t.TempDir() + "/test_output.csv"

	exporter, err := NewCsvExporter(tmpFile)
	if err != nil {
		t.Fatalf("Failed to create CsvExporter: %v", err)
	}

	// Write header
	if err := exporter.WriteHeader([]string{"ID", "NAME", "SCORE"}); err != nil {
		t.Fatalf("WriteHeader failed: %v", err)
	}

	// Write rows
	rows := [][]interface{}{
		{1, "Antigravity", 99.5},
		{2, "Senior Programmer", nil},
		{3, nil, 0},
	}
	for _, row := range rows {
		if err := exporter.WriteRow(row); err != nil {
			t.Fatalf("WriteRow failed: %v", err)
		}
	}

	// Close
	if err := exporter.Close(); err != nil {
		t.Fatalf("Close failed: %v", err)
	}

	// Verify file content
	data, err := os.ReadFile(tmpFile)
	if err != nil {
		t.Fatalf("Failed to read output file: %v", err)
	}

	content := string(data)
	// Skip BOM (3 bytes)
	if len(content) >= 3 && content[:3] == "\xEF\xBB\xBF" {
		content = content[3:]
	}

	reader := csv.NewReader(strings.NewReader(content))
	records, err := reader.ReadAll()
	if err != nil {
		t.Fatalf("Failed to parse CSV: %v", err)
	}

	// Expect 4 records: 1 header + 3 rows
	if len(records) != 4 {
		t.Errorf("Expected 4 records, got %d", len(records))
	}

	// Verify header
	if records[0][0] != "ID" || records[0][1] != "NAME" || records[0][2] != "SCORE" {
		t.Errorf("Unexpected header: %v", records[0])
	}

	// Verify NULL handling (row 2, column 2 should be empty string)
	if records[2][2] != "" {
		t.Errorf("Expected empty string for nil value, got: %q", records[2][2])
	}
}

func TestExportHandler_FullLifecycle(t *testing.T) {
	tmpFile := t.TempDir() + "/lifecycle_test.csv"

	exporter, err := NewCsvExporter(tmpFile)
	if err != nil {
		t.Fatalf("Failed to create CsvExporter: %v", err)
	}

	handler := NewExportHandler(exporter)

	// Simulate NDJSON stream
	headerJSON := json.RawMessage(`{"columns": [{"name": "ID", "type": "INT"}, {"name": "NAME", "type": "VARCHAR"}]}`)
	handler.OnMetadata(headerJSON)

	if len(handler.orderedColumns) != 2 {
		t.Errorf("Expected 2 columns, got %d", len(handler.orderedColumns))
	}

	rowJSON := json.RawMessage(`{"ID": 42, "NAME": "TestUser"}`)
	handler.OnRow(rowJSON)

	footerJSON := json.RawMessage(`{"rowCount": 1, "executionTime": 5}`)
	handler.OnFooter(footerJSON)

	if handler.orderedColumns != nil {
		t.Errorf("Expected orderedColumns to be nil after footer")
	}

	// Verify file
	data, err := os.ReadFile(tmpFile)
	if err != nil {
		t.Fatalf("Failed to read output file: %v", err)
	}

	content := string(data)
	if len(content) >= 3 && content[:3] == "\xEF\xBB\xBF" {
		content = content[3:]
	}

	reader := csv.NewReader(strings.NewReader(content))
	records, err := reader.ReadAll()
	if err != nil {
		t.Fatalf("Failed to parse CSV: %v", err)
	}

	if len(records) != 2 {
		t.Fatalf("Expected 2 records (header + 1 row), got %d", len(records))
	}

	if records[1][0] != "42" || records[1][1] != "TestUser" {
		t.Errorf("Unexpected row data: %v", records[1])
	}
}

func TestExportHandler_ErrorResetsState(t *testing.T) {
	tmpFile := t.TempDir() + "/error_test.csv"

	exporter, err := NewCsvExporter(tmpFile)
	if err != nil {
		t.Fatalf("Failed to create CsvExporter: %v", err)
	}

	handler := NewExportHandler(exporter)

	// Set some state
	headerJSON := json.RawMessage(`{"columns": [{"name": "COL1", "type": "INT"}]}`)
	handler.OnMetadata(headerJSON)

	// Simulate error
	handler.OnError(&domain.IPCError{Code: "TEST_ERR", Message: "simulated"})

	if handler.orderedColumns != nil {
		t.Errorf("Expected orderedColumns to be nil after error")
	}
}

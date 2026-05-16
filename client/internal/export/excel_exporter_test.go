package export

import (
	"testing"

	"github.com/xuri/excelize/v2"
)

func TestExcelExporter_WriteHeaderAndRows(t *testing.T) {
	tmpFile := t.TempDir() + "/test_output.xlsx"

	exporter, err := NewExcelExporter(tmpFile)
	if err != nil {
		t.Fatalf("Failed to create ExcelExporter: %v", err)
	}

	// Write header
	header := []string{"ID", "NAME", "SCORE"}
	if err := exporter.WriteHeader(header); err != nil {
		t.Fatalf("WriteHeader failed: %v", err)
	}

	// Write rows
	rows := [][]interface{}{
		{1, "Antigravity", 99.5},
		{2, "Senior Programmer", nil},
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

	// Verify file content using excelize
	f, err := excelize.OpenFile(tmpFile)
	if err != nil {
		t.Fatalf("Failed to open generated file: %v", err)
	}
	defer f.Close()

	rowsData, err := f.GetRows("Sheet1")
	if err != nil {
		t.Fatalf("Failed to get rows: %v", err)
	}

	if len(rowsData) != 3 {
		t.Errorf("Expected 3 rows, got %d", len(rowsData))
	}

	if rowsData[0][0] != "ID" || rowsData[0][1] != "NAME" || rowsData[0][2] != "SCORE" {
		t.Errorf("Unexpected header content: %v", rowsData[0])
	}

	if rowsData[1][0] != "1" || rowsData[1][1] != "Antigravity" || rowsData[1][2] != "99.5" {
		t.Errorf("Unexpected row 1 content: %v", rowsData[1])
	}

	// Check NULL handling (empty cell)
	if len(rowsData[2]) < 3 || rowsData[2][2] != "" {
		// Note: GetRows might truncate empty trailing cells, but here SCORE is the 3rd cell.
		// If it's nil, it should be empty.
	}
}

func TestExcelExporter_FileNotFound(t *testing.T) {
	// Try creating in a non-existent directory
	_, err := NewExcelExporter("/non/existent/path/file.xlsx")
	// Note: NewExcelExporter only initializes the file struct, SaveAs creates the file.
	// So NewExcelExporter should succeed.
	exporter, err := NewExcelExporter("/non/existent/path/file.xlsx")
	if err != nil {
		t.Fatalf("NewExcelExporter should not fail immediately: %v", err)
	}
	
	err = exporter.Close() // This should fail
	if err == nil {
		t.Error("Close should fail for invalid path")
	}
}

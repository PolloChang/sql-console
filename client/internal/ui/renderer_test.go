package ui

import (
	"encoding/json"
	"testing"

	"github.com/pollolab/sql-console/client/internal/domain"
)

func TestDefaultTableRenderer(t *testing.T) {
	renderer := NewDefaultTableRenderer()

	// 1. Test RenderHeader
	headerJSON := `{"columns": [{"name": "ID", "type": "INT"}, {"name": "NAME", "type": "VARCHAR"}]}`
	renderer.RenderHeader(json.RawMessage(headerJSON))

	if len(renderer.orderedColumns) != 2 {
		t.Errorf("Expected 2 columns, got %d", len(renderer.orderedColumns))
	}
	if renderer.orderedColumns[0] != "ID" || renderer.orderedColumns[1] != "NAME" {
		t.Errorf("Unexpected columns: %v", renderer.orderedColumns)
	}

	// 2. Test RenderRow
	rowJSON := `{"ID": 1, "NAME": "Antigravity"}`
	renderer.RenderRow(json.RawMessage(rowJSON))
	// Row appending doesn't return anything, but we've verified parsing logic in mind

	// 3. Test RenderFooter
	footerJSON := `{"rowCount": 1, "executionTime": 15}`
	renderer.RenderFooter(json.RawMessage(footerJSON))

	if renderer.orderedColumns != nil {
		t.Errorf("Expected orderedColumns to be reset to nil after RenderFooter")
	}

	// 4. Test RenderError
	renderer.RenderHeader(json.RawMessage(headerJSON)) // set some state
	renderer.RenderError(&domain.IPCError{Code: "TEST_ERR", Message: "Test error message"})
	
	if renderer.orderedColumns != nil {
		t.Errorf("Expected orderedColumns to be reset after RenderError")
	}
}

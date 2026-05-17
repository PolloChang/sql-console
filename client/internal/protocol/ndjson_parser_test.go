package protocol

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type mockHandler struct {
	metadataCalled int
	rowCalled      int
	footerCalled   int
	errorCalled    int
	lastError      *domain.IPCError
}

func (m *mockHandler) OnMetadata(data json.RawMessage) { m.metadataCalled++ }
func (m *mockHandler) OnRow(data json.RawMessage)      { m.rowCalled++ }
func (m *mockHandler) OnFooter(data json.RawMessage)   { m.footerCalled++ }
func (m *mockHandler) OnError(err *domain.IPCError) {
	m.errorCalled++
	m.lastError = err
}

func TestNdjsonParser_HandleResponses(t *testing.T) {
	input := `{"version":"1.0","requestId":"req-1","type":"header","payload":{"columns":[{"name":"ID","type":"INT"}]}}
{"version":"1.0","requestId":"req-1","type":"row","payload":{"ID":1}}
{"version":"1.0","requestId":"req-1","type":"row","payload":{"ID":2}}
{"version":"1.0","requestId":"req-1","type":"footer","payload":{"rowCount":2,"executionTime":10}}
`
	reader := strings.NewReader(input)
	parser := NewNdjsonParser(reader)
	handler := &mockHandler{}

	err := parser.HandleResponses(handler)
	if err != nil {
		t.Fatalf("HandleResponses failed: %v", err)
	}

	if handler.metadataCalled != 1 {
		t.Errorf("Expected OnMetadata called 1 time, got %d", handler.metadataCalled)
	}
	if handler.rowCalled != 2 {
		t.Errorf("Expected 2 row calls, got %d", handler.rowCalled)
	}
	if handler.footerCalled != 1 {
		t.Errorf("Expected OnFooter called 1 time, got %d", handler.footerCalled)
	}
}

func TestNdjsonParser_ErrorResponse(t *testing.T) {
	input := `{"version":"1.0","requestId":"req-1","type":"error","payload":{"code":"SQL_ERR","message":"syntax error"}}
`
	reader := strings.NewReader(input)
	parser := NewNdjsonParser(reader)
	handler := &mockHandler{}

	err := parser.HandleResponses(handler)
	if err == nil {
		t.Fatalf("Expected error, got nil")
	}

	if handler.errorCalled != 1 {
		t.Errorf("Expected OnError called 1 time, got %d", handler.errorCalled)
	}
	if handler.lastError == nil || handler.lastError.Code != "SQL_ERR" {
		t.Errorf("Unexpected error code: %v", handler.lastError)
	}
}

func TestNdjsonParser_SuccessType(t *testing.T) {
	// "success" type should also route to OnMetadata
	input := `{"version":"1.0","requestId":"req-1","type":"success","payload":{"message":"connected"}}
`
	reader := strings.NewReader(input)
	parser := NewNdjsonParser(reader)
	handler := &mockHandler{}

	err := parser.HandleResponses(handler)
	if err != nil {
		t.Fatalf("HandleResponses failed: %v", err)
	}

	if handler.metadataCalled != 1 {
		t.Errorf("Expected OnMetadata called for success type, got %d", handler.metadataCalled)
	}
}

func TestNdjsonParser_MultiRequest(t *testing.T) {
	// Simulate two requests on a single persistent stream
	input := `{"version":"1.0","requestId":"req-1","type":"success","payload":{"msg":"auth-ok"}}
{"version":"1.0","requestId":"req-2","type":"header","payload":{"columns":[]}}
{"version":"1.0","requestId":"req-2","type":"footer","payload":{"rowCount":0}}
`
	reader := strings.NewReader(input)
	parser := NewNdjsonParser(reader)

	// First Request: connect (returns on success)
	h1 := &mockHandler{}
	if err := parser.HandleResponses(h1); err != nil {
		t.Fatalf("First HandleResponses failed: %v", err)
	}
	if h1.metadataCalled != 1 {
		t.Errorf("h1 metadataCalled expected 1, got %d", h1.metadataCalled)
	}

	// Second Request: query (returns on footer)
	h2 := &mockHandler{}
	if err := parser.HandleResponses(h2); err != nil {
		t.Fatalf("Second HandleResponses failed: %v", err)
	}
	if h2.metadataCalled != 1 || h2.footerCalled != 1 {
		t.Errorf("h2 metadataCalled/footerCalled mismatch: %d/%d", h2.metadataCalled, h2.footerCalled)
	}
}

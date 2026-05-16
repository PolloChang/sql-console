package export

import (
	"encoding/json"

	"github.com/pollolab/sql-console/client/internal/domain"
)

// TeeHandler implements domain.ResponseHandler by fanning out each event
// to multiple downstream handlers. This enables simultaneous console display
// and file export without modifying either handler.
type TeeHandler struct {
	handlers []domain.ResponseHandler
}

// NewTeeHandler creates a TeeHandler that delegates to all given handlers.
func NewTeeHandler(handlers ...domain.ResponseHandler) *TeeHandler {
	return &TeeHandler{
		handlers: handlers,
	}
}

func (t *TeeHandler) OnMetadata(data json.RawMessage) {
	for _, h := range t.handlers {
		h.OnMetadata(data)
	}
}

func (t *TeeHandler) OnRow(data json.RawMessage) {
	for _, h := range t.handlers {
		h.OnRow(data)
	}
}

func (t *TeeHandler) OnFooter(data json.RawMessage) {
	for _, h := range t.handlers {
		h.OnFooter(data)
	}
}

func (t *TeeHandler) OnError(err *domain.IPCError) {
	for _, h := range t.handlers {
		h.OnError(err)
	}
}

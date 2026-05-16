package ui

import (
	"encoding/json"

	"github.com/pollolab/sql-console/client/internal/domain"
)

// ConsoleHandler handles IPC responses and coordinates rendering.
type ConsoleHandler struct {
	renderer  TableRenderer
	lastSqlId string
}

func NewConsoleHandler(renderer TableRenderer) *ConsoleHandler {
	return &ConsoleHandler{
		renderer: renderer,
	}
}

func (h *ConsoleHandler) OnMetadata(data json.RawMessage) {
	var meta struct {
		SqlId string `json:"sqlId"`
	}
	json.Unmarshal(data, &meta)
	if meta.SqlId != "" {
		h.lastSqlId = meta.SqlId
	}
	h.renderer.RenderHeader(data)
}

func (h *ConsoleHandler) GetLastSqlId() string {
	return h.lastSqlId
}

func (h *ConsoleHandler) OnRow(data json.RawMessage) {
	h.renderer.RenderRow(data)
}

func (h *ConsoleHandler) OnFooter(data json.RawMessage) {
	h.renderer.RenderFooter(data)
}

func (h *ConsoleHandler) OnError(err *domain.IPCError) {
	h.renderer.RenderError(err)
}

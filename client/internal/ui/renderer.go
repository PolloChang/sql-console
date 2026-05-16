package ui

import (
	"encoding/json"
	"fmt"
	"os"

	"github.com/olekukonko/tablewriter"
	"github.com/pollolab/sql-console/client/internal/domain"
)

// TableRenderer defines the interface for rendering query results.
type TableRenderer interface {
	RenderHeader(data json.RawMessage)
	RenderRow(data json.RawMessage)
	RenderFooter(data json.RawMessage)
	RenderError(err *domain.IPCError)
	RenderMessage(msg string)
	SetHideTable(hide bool)
}

// DefaultTableRenderer implements TableRenderer using tablewriter.
type DefaultTableRenderer struct {
	table          *tablewriter.Table
	orderedColumns []string
	lastMetadata   *metadataPayload
	hideTable      bool
}

func NewDefaultTableRenderer() *DefaultTableRenderer {
	r := &DefaultTableRenderer{}
	r.reset()
	return r
}

func (r *DefaultTableRenderer) reset() {
	r.table = tablewriter.NewWriter(os.Stdout)
	r.orderedColumns = nil
	r.lastMetadata = nil
}

type columnMetadata struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

type metadataPayload struct {
	SqlId       string           `json:"sqlId"`
	Transaction string           `json:"transaction"`
	Columns     []columnMetadata `json:"columns"`
}

func (r *DefaultTableRenderer) SetHideTable(hide bool) {
	r.hideTable = hide
}

func (r *DefaultTableRenderer) RenderHeader(data json.RawMessage) {
	var payload metadataPayload
	if err := json.Unmarshal(data, &payload); err != nil {
		fmt.Printf("Error parsing header: %v\n", err)
		return
	}

	r.lastMetadata = &payload

	if r.hideTable {
		return
	}

	r.orderedColumns = make([]string, len(payload.Columns))
	anyCols := make([]any, len(payload.Columns))
	for i, col := range payload.Columns {
		r.orderedColumns[i] = col.Name
		anyCols[i] = col.Name
	}
	r.table.Header(anyCols...)
}

func (r *DefaultTableRenderer) RenderRow(data json.RawMessage) {
	if r.hideTable {
		return
	}

	var rowMap map[string]interface{}
	if err := json.Unmarshal(data, &rowMap); err != nil {
		fmt.Printf("Error parsing row: %v\n", err)
		return
	}

	if r.orderedColumns == nil {
		fmt.Println("Error: row received before header")
		return
	}

	row := make([]interface{}, len(r.orderedColumns))
	for i, colName := range r.orderedColumns {
		val, ok := rowMap[colName]
		if !ok || val == nil {
			row[i] = "NULL"
		} else {
			row[i] = val
		}
	}
	r.table.Append(row...)
}

func (r *DefaultTableRenderer) RenderFooter(data json.RawMessage) {
	var stats domain.ExecutionStats
	if err := json.Unmarshal(data, &stats); err != nil {
		fmt.Printf("Error parsing footer: %v\n", err)
		return
	}

	// 1. Print Header Info (SqlId, Transaction, Pagination)
	if r.lastMetadata != nil && r.lastMetadata.SqlId != "" {
		fmt.Printf("sql_id: %s , transaction: %s\n", r.lastMetadata.SqlId, r.lastMetadata.Transaction)

		// Only print pagination info if it's a paginated result (TotalRows > 0 and PageSize > 0)
		if stats.TotalRows > 0 && stats.PageSize > 0 {
			fmt.Printf("result fetch size: %d\n", stats.PageSize)

			totalPages := 1
			if stats.PageSize > 0 {
				totalPages = (stats.TotalRows + stats.PageSize - 1) / stats.PageSize
			}
			fmt.Printf("page: %d/%d, total rows: %d\n", stats.Page, totalPages, stats.TotalRows)
		}
	}

	// 2. Render Table
	if !r.hideTable {
		r.table.Render()
	}

	// 3. Print Final Summary
	fmt.Printf("\n(%d rows affected, %dms)\n", stats.RowCount, stats.ExecutionTime)

	r.reset()
}

func (r *DefaultTableRenderer) RenderError(err *domain.IPCError) {
	if err != nil {
		fmt.Printf("\n[ERROR] %s: %s\n", err.Code, err.Message)
	} else {
		fmt.Println("\n[ERROR] Unknown error")
	}
	r.reset()
}

func (r *DefaultTableRenderer) RenderMessage(msg string) {
	fmt.Printf("\n[INFO] %s\n", msg)
}

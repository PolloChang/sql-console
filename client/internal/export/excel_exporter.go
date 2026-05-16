package export

import (
	"fmt"

	"github.com/xuri/excelize/v2"
)

// ExcelExporter implements Exporter by writing results to an Excel (.xlsx) file.
type ExcelExporter struct {
	filePath string
	f        *excelize.File
	sheet    string
	currRow  int
}

// NewExcelExporter creates an ExcelExporter that writes to the specified file path.
func NewExcelExporter(filePath string) (*ExcelExporter, error) {
	f := excelize.NewFile()
	sheet := "Sheet1"
	// Ensure the sheet exists or rename default
	f.SetSheetName("Sheet1", sheet)

	return &ExcelExporter{
		filePath: filePath,
		f:        f,
		sheet:    sheet,
		currRow:  1,
	}, nil
}

func (e *ExcelExporter) WriteHeader(columns []string) error {
	for i, col := range columns {
		cell, err := excelize.CoordinatesToCellName(i+1, e.currRow)
		if err != nil {
			return err
		}
		if err := e.f.SetCellValue(e.sheet, cell, col); err != nil {
			return err
		}
	}

	// Apply a basic style to the header
	style, err := e.f.NewStyle(&excelize.Style{
		Font: &excelize.Font{Bold: true},
		Fill: excelize.Fill{Type: "pattern", Color: []string{"#DDDDDD"}, Pattern: 1},
	})
	if err == nil {
		cell1, _ := excelize.CoordinatesToCellName(1, e.currRow)
		cell2, _ := excelize.CoordinatesToCellName(len(columns), e.currRow)
		e.f.SetCellStyle(e.sheet, cell1, cell2, style)
	}

	e.currRow++
	return nil
}

func (e *ExcelExporter) WriteRow(values []interface{}) error {
	for i, val := range values {
		cell, err := excelize.CoordinatesToCellName(i+1, e.currRow)
		if err != nil {
			return err
		}
		if val == nil {
			if err := e.f.SetCellValue(e.sheet, cell, ""); err != nil {
				return err
			}
		} else {
			if err := e.f.SetCellValue(e.sheet, cell, val); err != nil {
				return err
			}
		}
	}
	e.currRow++
	return nil
}

func (e *ExcelExporter) Close() error {
	if err := e.f.SaveAs(e.filePath); err != nil {
		return fmt.Errorf("failed to save excel file: %w", err)
	}
	return e.f.Close()
}

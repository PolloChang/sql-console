package export

import (
	"encoding/csv"
	"fmt"
	"os"
)

// CsvExporter implements Exporter by writing results to a CSV file.
type CsvExporter struct {
	file   *os.File
	writer *csv.Writer
}

// NewCsvExporter creates a CsvExporter that writes to the specified file path.
func NewCsvExporter(filePath string) (*CsvExporter, error) {
	file, err := os.Create(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to create CSV file: %w", err)
	}

	// Write UTF-8 BOM for Excel compatibility
	if _, err := file.Write([]byte{0xEF, 0xBB, 0xBF}); err != nil {
		file.Close()
		return nil, fmt.Errorf("failed to write BOM: %w", err)
	}

	return &CsvExporter{
		file:   file,
		writer: csv.NewWriter(file),
	}, nil
}

func (e *CsvExporter) WriteHeader(columns []string) error {
	return e.writer.Write(columns)
}

func (e *CsvExporter) WriteRow(values []interface{}) error {
	record := make([]string, len(values))
	for i, val := range values {
		if val == nil {
			record[i] = ""
		} else {
			record[i] = fmt.Sprintf("%v", val)
		}
	}
	return e.writer.Write(record)
}

func (e *CsvExporter) Close() error {
	e.writer.Flush()
	if err := e.writer.Error(); err != nil {
		e.file.Close()
		return fmt.Errorf("CSV flush error: %w", err)
	}
	return e.file.Close()
}

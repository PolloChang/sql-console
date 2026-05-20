package service

import (
	"bufio"
	"context"
	"encoding/csv"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/user"
	"strings"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type QueryService struct {
	profileSvc *ProfileService
	messenger  domain.Messenger
}

type connectHandler struct{}

func (h *connectHandler) OnMetadata(data json.RawMessage) {}
func (h *connectHandler) OnRow(data json.RawMessage)      {}
func (h *connectHandler) OnFooter(data json.RawMessage)   {}
func (h *connectHandler) OnError(err *domain.IPCError)    {}

func NewQueryService(profileSvc *ProfileService, messenger domain.Messenger) *QueryService {
	return &QueryService{
		profileSvc: profileSvc,
		messenger:  messenger,
	}
}

func (s *QueryService) getOSUser() string {
	u, err := user.Current()
	if err != nil {
		return "unknown"
	}
	return u.Username
}

// ExecuteSQL handles the 'connect' and 'query' flow.
func (s *QueryService) ExecuteSQL(ctx context.Context, profileName, sql string, handler domain.ResponseHandler, pageSize int) error {
	// 1. Load Profile
	profile, err := s.profileSvc.GetProfile(profileName)
	if err != nil {
		return fmt.Errorf("failed to load profile: %w", err)
	}

	// 2. Connect (Authenticate)
	connectReq := domain.Request{
		Version:   "1.0",
		Action:    "connect",
		RequestId: "req-auth",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"profile":  profile.Name,
			"url":      profile.URL,
			"username": profile.Username,
			"password": profile.Password,
		},
	}

	// For simple CLI, we just do one-off send/receive.
	// But the UDS messenger implementation currently parses until the stream ends.
	// In the Java Daemon, 'connect' sends a single success/error message and keeps the connection open.
	// However, my UdsMessenger.Send() currently blocks until the connection is closed or EOF.
	// We need to adjust UdsMessenger if we want to reuse the connection.
	// For now, let's keep it simple: one connection per action (Connect then Query).
	// Actually, the Java Daemon supports multiple requests per connection.
	
	// Refined logic:
	// We'll send 'connect' first.
	if err := s.messenger.Send(context.Background(), connectReq, &connectHandler{}); err != nil {
		return err
	}

	// 3. Send Query
	queryReq := domain.Request{
		Version:   "1.0",
		Action:    "query",
		RequestId: "req-query",
		OSUser:    s.getOSUser(),
		Payload: map[string]interface{}{
			"sql":      sql,
			"pageSize": pageSize,
		},
	}

	return s.messenger.Send(ctx, queryReq, handler)
}

// FetchPage retrieves a specific page of a previously executed query.
func (s *QueryService) FetchPage(ctx context.Context, profileName, sqlId string, page int, handler domain.ResponseHandler) error {
	// 1. Load Profile (Need to ensure session is authenticated)
	profile, err := s.profileSvc.GetProfile(profileName)
	if err != nil {
		return fmt.Errorf("failed to load profile: %w", err)
	}

	// 2. Connect (Authenticate)
	connectReq := domain.Request{
		Version:   "1.0",
		Action:    "connect",
		RequestId: "req-auth",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"profile":  profile.Name,
			"url":      profile.URL,
			"username": profile.Username,
			"password": profile.Password,
		},
	}

	if err := s.messenger.Send(context.Background(), connectReq, &connectHandler{}); err != nil {
		return err
	}

	// 3. Send Fetch Request
	fetchReq := domain.Request{
		Version:   "1.0",
		Action:    "fetch",
		RequestId: "req-fetch",
		OSUser:    s.getOSUser(),
		Payload: map[string]interface{}{
			"sqlId": sqlId,
			"page":  page,
		},
	}

	return s.messenger.Send(ctx, fetchReq, handler)
}

// SetTransaction changes the transaction mode (auto-commit or manual-commit).
func (s *QueryService) SetTransaction(ctx context.Context, profileName, mode string, handler domain.ResponseHandler) error {
	// 1. Load Profile
	profile, err := s.profileSvc.GetProfile(profileName)
	if err != nil {
		return fmt.Errorf("failed to load profile: %w", err)
	}

	// 2. Connect (Authenticate)
	connectReq := domain.Request{
		Version:   "1.0",
		Action:    "connect",
		RequestId: "req-auth",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"profile":  profile.Name,
			"url":      profile.URL,
			"username": profile.Username,
			"password": profile.Password,
		},
	}

	if err := s.messenger.Send(context.Background(), connectReq, &connectHandler{}); err != nil {
		return err
	}

	// 3. Send Set-Transaction Request
	txReq := domain.Request{
		Version:   "1.0",
		Action:    "set-transaction",
		RequestId: "req-set-tx",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"mode": mode,
		},
	}

	return s.messenger.Send(ctx, txReq, handler)
}

// ListTables retrieves a list of tables from the database.
func (s *QueryService) ListTables(ctx context.Context, profileName string, handler domain.ResponseHandler) error {
	// 1. Load Profile
	profile, err := s.profileSvc.GetProfile(profileName)
	if err != nil {
		return fmt.Errorf("failed to load profile: %w", err)
	}

	// 2. Connect (Authenticate)
	connectReq := domain.Request{
		Version:   "1.0",
		Action:    "connect",
		RequestId: "req-auth",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"profile":  profile.Name,
			"url":      profile.URL,
			"username": profile.Username,
			"password": profile.Password,
		},
	}

	if err := s.messenger.Send(context.Background(), connectReq, &connectHandler{}); err != nil {
		return err
	}

	// 3. Send List-Tables Request
	listReq := domain.Request{
		Version:   "1.0",
		Action:    "list-tables",
		RequestId: "req-list-tables",
		OSUser:    s.getOSUser(),
		Payload:   map[string]string{},
	}

	return s.messenger.Send(ctx, listReq, handler)
}

// ImportCSV reads a CSV file in batches and sends import requests to the daemon.
func (s *QueryService) ImportCSV(ctx context.Context, profileName, filePath, tableName string, columnMap map[string]string, handler domain.ResponseHandler) error {
	// 1. Load Profile
	profile, err := s.profileSvc.GetProfile(profileName)
	if err != nil {
		return fmt.Errorf("failed to load profile: %w", err)
	}

	// 2. Connect (Authenticate)
	connectReq := domain.Request{
		Version:   "1.0",
		Action:    "connect",
		RequestId: "req-auth",
		OSUser:    s.getOSUser(),
		Payload: map[string]string{
			"profile":  profile.Name,
			"url":      profile.URL,
			"username": profile.Username,
			"password": profile.Password,
		},
	}

	if err := s.messenger.Send(context.Background(), connectReq, &connectHandler{}); err != nil {
		return err
	}

	// 3. Open CSV file
	f, err := os.Open(filePath)
	if err != nil {
		return fmt.Errorf("failed to open CSV file %s: %w", filePath, err)
	}
	defer f.Close()

	// Handle optional UTF-8 BOM
	bufReader := bufio.NewReader(f)
	bom, err := bufReader.Peek(3)
	if err == nil && len(bom) == 3 && bom[0] == 0xEF && bom[1] == 0xBB && bom[2] == 0xBF {
		bufReader.Discard(3)
	}

	csvReader := csv.NewReader(bufReader)
	csvReader.LazyQuotes = true

	// Read header
	headers, err := csvReader.Read()
	if err != nil {
		return fmt.Errorf("failed to read CSV headers: %w", err)
	}

	for i, h := range headers {
		cleaned := strings.TrimSpace(h)
		if mapped, ok := columnMap[cleaned]; ok {
			headers[i] = mapped
		} else {
			headers[i] = cleaned
		}
	}

	// Read rows in batches
	batchSize := 1000
	var batch [][]interface{}

	batchNo := 1
	totalImported := 0

	for {
		row, err := csvReader.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			return fmt.Errorf("error reading CSV row: %w", err)
		}

		var objRow []interface{}
		for _, val := range row {
			objRow = append(objRow, strings.TrimSpace(val))
		}
		batch = append(batch, objRow)

		if len(batch) >= batchSize {
			req := domain.Request{
				Version:   "1.0",
				Action:    "import",
				RequestId: fmt.Sprintf("req-import-%d", batchNo),
				OSUser:    s.getOSUser(),
				Payload: map[string]interface{}{
					"table":   tableName,
					"columns": headers,
					"rows":    batch,
				},
			}

			if err := s.messenger.Send(ctx, req, handler); err != nil {
				return fmt.Errorf("import batch %d failed: %w", batchNo, err)
			}

			totalImported += len(batch)
			batch = nil
			batchNo++
		}
	}

	// Send remaining batch
	if len(batch) > 0 {
		req := domain.Request{
			Version:   "1.0",
			Action:    "import",
			RequestId: fmt.Sprintf("req-import-%d", batchNo),
			OSUser:    s.getOSUser(),
			Payload: map[string]interface{}{
				"table":   tableName,
				"columns": headers,
				"rows":    batch,
			},
		}

		if err := s.messenger.Send(ctx, req, handler); err != nil {
			return fmt.Errorf("import final batch failed: %w", err)
		}
		totalImported += len(batch)
	}

	fmt.Printf("\n[INFO] Successfully imported %d rows into table %s.\n", totalImported, tableName)
	return nil
}

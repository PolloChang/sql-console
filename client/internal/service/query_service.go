package service

import (
	"context"
	"encoding/json"
	"fmt"
	"os/user"

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

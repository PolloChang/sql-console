package service

import (
	"context"
	"testing"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type mockMessenger struct {
	lastRequest domain.Request
}

func (m *mockMessenger) Send(ctx context.Context, req domain.Request, handler domain.ResponseHandler) error {
	m.lastRequest = req
	return nil
}

func (m *mockMessenger) Close() error { return nil }

type mockEncrypter struct{}

func (e *mockEncrypter) Encrypt(plainText string) (string, error) { return plainText, nil }
func (e *mockEncrypter) Decrypt(cipherText string) (string, error) { return cipherText, nil }

type mockRepo struct{}

func (r *mockRepo) Load() (map[string]domain.JdbcProfile, error) {
	return map[string]domain.JdbcProfile{
		"test": {Name: "test", URL: "url", Username: "user", Password: "pass"},
	}, nil
}
func (r *mockRepo) Save(profiles map[string]domain.JdbcProfile) error { return nil }

func TestQueryService_ExecuteSQL_PageSize(t *testing.T) {
	messenger := &mockMessenger{}
	repo := &mockRepo{}
	crypto := &mockEncrypter{}
	profileSvc := NewProfileService(repo, crypto)
	svc := NewQueryService(profileSvc, messenger)

	err := svc.ExecuteSQL(context.Background(), "test", "SELECT 1", nil, 50)
	if err != nil {
		t.Fatalf("ExecuteSQL failed: %v", err)
	}

	// The last request should be the 'query' action
	if messenger.lastRequest.Action != "query" {
		t.Errorf("Expected action 'query', got '%s'", messenger.lastRequest.Action)
	}

	payload, ok := messenger.lastRequest.Payload.(map[string]interface{})
	if !ok {
		t.Fatalf("Payload is not a map[string]interface{}")
	}

	if payload["pageSize"] != 50 {
		t.Errorf("Expected pageSize 50, got %v", payload["pageSize"])
	}
}

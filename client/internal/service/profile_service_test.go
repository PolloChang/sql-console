package service

import (
	"testing"

	"github.com/pollolab/sql-console/client/internal/domain"
)

// MockEncrypter implements domain.Encrypter
type MockEncrypter struct{}

func (m *MockEncrypter) Encrypt(plainText string) (string, error) {
	return "enc_" + plainText, nil
}

func (m *MockEncrypter) Decrypt(cipherText string) (string, error) {
	return cipherText[4:], nil // remove "enc_"
}

// MockRepository implements domain.ProfileRepository
type MockRepository struct {
	data map[string]domain.JdbcProfile
}

func (m *MockRepository) Load() (map[string]domain.JdbcProfile, error) {
	if m.data == nil {
		m.data = make(map[string]domain.JdbcProfile)
	}
	return m.data, nil
}

func (m *MockRepository) Save(profiles map[string]domain.JdbcProfile) error {
	m.data = profiles
	return nil
}

func TestProfileService_AddAndGet(t *testing.T) {
	repo := &MockRepository{}
	crypto := &MockEncrypter{}
	svc := NewProfileService(repo, crypto)

	err := svc.AddProfile("test", "url", "user", "pass")
	if err != nil {
		t.Fatalf("Failed to add profile: %v", err)
	}

	// Verify encryption was called via mock behavior
	profiles, _ := repo.Load()
	if profiles["test"].Password != "enc_pass" {
		t.Errorf("Expected encrypted password 'enc_pass', got '%s'", profiles["test"].Password)
	}

	// Verify decryption via GetProfile
	p, err := svc.GetProfile("test")
	if err != nil {
		t.Fatalf("Failed to get profile: %v", err)
	}
	if p.Password != "pass" {
		t.Errorf("Expected decrypted password 'pass', got '%s'", p.Password)
	}
}

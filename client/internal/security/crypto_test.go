package security

import (
	"os"
	"path/filepath"
	"testing"
)

func TestEncryptionDecryption(t *testing.T) {
	// Create a dummy SSH key for testing
	home, _ := os.UserHomeDir()
	dummyKeyPath := filepath.Join(home, ".ssh", "id_rsa_test")
	err := os.WriteFile(dummyKeyPath, []byte("dummy-ssh-key-content"), 0600)
	if err != nil {
		t.Fatalf("Failed to create dummy key: %v", err)
	}
	defer os.Remove(dummyKeyPath)

	crypto, err := NewCrypto(dummyKeyPath)
	if err != nil {
		t.Fatalf("NewCrypto failed: %v", err)
	}

	originalText := "secret-password-123"
	encrypted, err := crypto.Encrypt(originalText)
	if err != nil {
		t.Fatalf("Encrypt failed: %v", err)
	}

	if encrypted == originalText {
		t.Error("Encrypted text should be different from original text")
	}

	decrypted, err := crypto.Decrypt(encrypted)
	if err != nil {
		t.Fatalf("Decrypt failed: %v", err)
	}

	if decrypted != originalText {
		t.Errorf("Decrypted text '%s' does not match original '%s'", decrypted, originalText)
	}
}

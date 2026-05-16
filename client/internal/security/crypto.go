package security

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"io"
	"os"
	"path/filepath"
)

// Crypto provides AES-GCM encryption/decryption.
type Crypto struct {
	key []byte
}

// NewCrypto creates a new Crypto instance using the provided SSH private key path as a seed.
func NewCrypto(sshKeyPath string) (*Crypto, error) {
	if sshKeyPath == "" {
		home, err := os.UserHomeDir()
		if err != nil {
			return nil, fmt.Errorf("could not determine home directory: %w", err)
		}
		sshKeyPath = filepath.Join(home, ".ssh", "id_rsa")
	}

	data, err := os.ReadFile(sshKeyPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read SSH key at %s: %w", sshKeyPath, err)
	}

	// Derive 32-byte key using SHA-256
	hash := sha256.Sum256(data)
	return &Crypto{key: hash[:]}, nil
}

// Encrypt encrypts plain text using AES-GCM.
func (c *Crypto) Encrypt(plainText string) (string, error) {
	block, err := aes.NewCipher(c.key)
	if err != nil {
		return "", err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}

	nonce := make([]byte, gcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return "", err
	}

	cipherText := gcm.Seal(nonce, nonce, []byte(plainText), nil)
	return base64.StdEncoding.EncodeToString(cipherText), nil
}

// Decrypt decrypts base64 encoded cipher text.
func (c *Crypto) Decrypt(encodedCipherText string) (string, error) {
	cipherText, err := base64.StdEncoding.DecodeString(encodedCipherText)
	if err != nil {
		return "", err
	}

	block, err := aes.NewCipher(c.key)
	if err != nil {
		return "", err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}

	nonceSize := gcm.NonceSize()
	if len(cipherText) < nonceSize {
		return "", fmt.Errorf("cipher text too short")
	}

	nonce, actualCipherText := cipherText[:nonceSize], cipherText[nonceSize:]
	plainText, err := gcm.Open(nil, nonce, actualCipherText, nil)
	if err != nil {
		return "", err
	}

	return string(plainText), nil
}

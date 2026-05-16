package domain

// JdbcProfile represents a database connection profile.
type JdbcProfile struct {
	Name     string `json:"name"`
	URL      string `json:"url"`
	Username string `json:"username"`
	Password string `json:"password"` // Encrypted in storage
}

// Encrypter defines the contract for credential encryption.
type Encrypter interface {
	Encrypt(plainText string) (string, error)
	Decrypt(cipherText string) (string, error)
}

// ProfileRepository defines the contract for profile persistence.
type ProfileRepository interface {
	Load() (map[string]JdbcProfile, error)
	Save(profiles map[string]JdbcProfile) error
}

package repository

import (
	"encoding/json"
	"os"
	"path/filepath"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type JsonProfileRepository struct {
	path string
}

func NewJsonProfileRepository() (*JsonProfileRepository, error) {
	home, err := os.UserHomeDir()
	if err != nil {
		return nil, err
	}
	configDir := filepath.Join(home, ".sql-console")
	if err := os.MkdirAll(configDir, 0700); err != nil {
		return nil, err
	}

	return &JsonProfileRepository{
		path: filepath.Join(configDir, "profiles.json"),
	}, nil
}

func (r *JsonProfileRepository) Load() (map[string]domain.JdbcProfile, error) {
	if _, err := os.Stat(r.path); os.IsNotExist(err) {
		return make(map[string]domain.JdbcProfile), nil
	}

	data, err := os.ReadFile(r.path)
	if err != nil {
		return nil, err
	}

	var profiles map[string]domain.JdbcProfile
	if err := json.Unmarshal(data, &profiles); err != nil {
		return nil, err
	}

	return profiles, nil
}

func (r *JsonProfileRepository) Save(profiles map[string]domain.JdbcProfile) error {
	data, err := json.MarshalIndent(profiles, "", "  ")
	if err != nil {
		return err
	}

	return os.WriteFile(r.path, data, 0600)
}

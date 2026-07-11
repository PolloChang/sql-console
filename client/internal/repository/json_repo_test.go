package repository

import (
	"os"
	"path/filepath"
	"testing"
)

func TestJsonProfileRepository_Load(t *testing.T) {
	tmpDir, err := os.MkdirTemp("", "sql-console-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tmpDir)

	profilePath := filepath.Join(tmpDir, "profiles.json")

	repo := &JsonProfileRepository{
		path: profilePath,
	}

	// Case 1: File does not exist
	profiles, err := repo.Load()
	if err != nil {
		t.Fatalf("Expected no error when file is missing, got: %v", err)
	}
	if len(profiles) != 0 {
		t.Errorf("Expected empty map, got size: %d", len(profiles))
	}

	// Case 2: Standard Map Format
	mapData := `{
		"db1": {
			"name": "db1",
			"url": "jdbc:postgresql://host1/db",
			"username": "user1",
			"password": "pass1"
		}
	}`
	if err := os.WriteFile(profilePath, []byte(mapData), 0600); err != nil {
		t.Fatalf("Failed to write mock map data: %v", err)
	}

	profiles, err = repo.Load()
	if err != nil {
		t.Fatalf("Expected no error for map format, got: %v", err)
	}
	if len(profiles) != 1 || profiles["db1"].Name != "db1" {
		t.Errorf("Unexpected profiles content: %v", profiles)
	}

	// Case 3: Legacy Array Format
	arrayData := `{
		"profiles": [
			{
				"name": "db2",
				"url": "jdbc:postgresql://host2/db",
				"username": "user2",
				"password": "pass2"
			}
		]
	}`
	if err := os.WriteFile(profilePath, []byte(arrayData), 0600); err != nil {
		t.Fatalf("Failed to write mock array data: %v", err)
	}

	profiles, err = repo.Load()
	if err != nil {
		t.Fatalf("Expected no error for array format, got: %v", err)
	}
	if len(profiles) != 1 || profiles["db2"].Name != "db2" {
		t.Errorf("Unexpected profiles content: %v", profiles)
	}
}

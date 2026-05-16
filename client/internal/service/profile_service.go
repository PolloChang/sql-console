package service

import (
	"fmt"
	"sort"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type ProfileService struct {
	repo    domain.ProfileRepository
	crypto  domain.Encrypter
}

func NewProfileService(repo domain.ProfileRepository, crypto domain.Encrypter) *ProfileService {
	return &ProfileService{
		repo:   repo,
		crypto: crypto,
	}
}

func (s *ProfileService) AddProfile(name, url, username, password string) error {
	profiles, err := s.repo.Load()
	if err != nil {
		return err
	}

	encryptedPassword, err := s.crypto.Encrypt(password)
	if err != nil {
		return fmt.Errorf("failed to encrypt password: %w", err)
	}

	profiles[name] = domain.JdbcProfile{
		Name:     name,
		URL:      url,
		Username: username,
		Password: encryptedPassword,
	}

	return s.repo.Save(profiles)
}

func (s *ProfileService) ListProfiles() ([]string, error) {
	profiles, err := s.repo.Load()
	if err != nil {
		return nil, err
	}

	names := make([]string, 0, len(profiles))
	for name := range profiles {
		names = append(names, name)
	}
	sort.Strings(names)
	return names, nil
}

func (s *ProfileService) DeleteProfile(name string) error {
	profiles, err := s.repo.Load()
	if err != nil {
		return err
	}

	if _, ok := profiles[name]; !ok {
		return fmt.Errorf("profile not found: %s", name)
	}

	delete(profiles, name)
	return s.repo.Save(profiles)
}

func (s *ProfileService) GetProfile(name string) (*domain.JdbcProfile, error) {
	profiles, err := s.repo.Load()
	if err != nil {
		return nil, err
	}

	profile, ok := profiles[name]
	if !ok {
		return nil, fmt.Errorf("profile not found: %s", name)
	}

	decryptedPassword, err := s.crypto.Decrypt(profile.Password)
	if err != nil {
		return nil, fmt.Errorf("failed to decrypt password: %w", err)
	}

	profile.Password = decryptedPassword
	return &profile, nil
}

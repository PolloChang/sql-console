package repl

import (
	"context"
	"fmt"
	"io"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"syscall"

	"github.com/ergochat/readline"
	"github.com/pollolab/sql-console/client/internal/service"
	"github.com/pollolab/sql-console/client/internal/ui"
)

const (
	continuePromptPad = "      -> "
)

// ReplSession manages the interactive REPL loop.
type ReplSession struct {
	profileName string
	querySvc    *service.QueryService
	profileSvc  *service.ProfileService
	handler     *ui.ConsoleHandler
	rl          *readline.Instance
	version     string
	lastSqlId   string
}

// NewReplSession creates a new REPL session.
func NewReplSession(
	profileName string,
	querySvc *service.QueryService,
	profileSvc *service.ProfileService,
	version string,
) (*ReplSession, error) {
	// Ensure history directory exists
	historyPath := getHistoryPath()
	if dir := filepath.Dir(historyPath); dir != "" {
		os.MkdirAll(dir, 0700)
	}

	// Build completer with available profiles
	profiles, _ := profileSvc.ListProfiles()
	completer := NewCompleter(profiles)

	prompt := buildPrompt(profileName)

	rl, err := readline.NewEx(&readline.Config{
		Prompt:            prompt,
		HistoryFile:       historyPath,
		AutoComplete:      completer,
		InterruptPrompt:   "^C",
		EOFPrompt:         "\\q",
		HistorySearchFold: true,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to initialize readline: %w", err)
	}

	renderer := ui.NewDefaultTableRenderer()
	handler := ui.NewConsoleHandler(renderer)

	return &ReplSession{
		profileName: profileName,
		querySvc:    querySvc,
		profileSvc:  profileSvc,
		handler:     handler,
		rl:          rl,
		version:     version,
	}, nil
}

// Start runs the REPL main loop.
func (s *ReplSession) Start() {
	defer s.rl.Close()

	// Print welcome banner
	fmt.Printf("sql-console v%s\n", s.version)
	if s.profileName != "" {
		fmt.Printf("Connected to: %s\n", s.profileName)
	} else {
		fmt.Println("No profile selected. Use \\c <profile> to connect.")
	}
	fmt.Println("Type \\q to quit, \\? for help.")
	fmt.Println()

	var buffer strings.Builder

	for {
		line, err := s.rl.Readline()
		if err != nil {
			if err == readline.ErrInterrupt {
				// Ctrl+C: clear the current buffer
				buffer.Reset()
				s.rl.SetPrompt(buildPrompt(s.profileName))
				continue
			}
			if err == io.EOF {
				fmt.Println("Bye!")
				return
			}
			fmt.Fprintf(os.Stderr, "Read error: %v\n", err)
			return
		}

		trimmed := strings.TrimSpace(line)

		// If buffer is empty, check for backslash commands
		if buffer.Len() == 0 && strings.HasPrefix(trimmed, `\`) {
			if s.handleBackslashCommand(trimmed) {
				return // \q was issued
			}
			continue
		}

		// Skip empty lines when buffer is empty
		if buffer.Len() == 0 && trimmed == "" {
			continue
		}

		// Accumulate into multi-line buffer
		if buffer.Len() > 0 {
			buffer.WriteString("\n")
		}
		buffer.WriteString(line)

		// Check if statement is complete (ends with ; or \g)
		currentSQL := strings.TrimSpace(buffer.String())
		if isStatementComplete(currentSQL) {
			// Remove trailing ; or \g for clean SQL
			sql := strings.TrimRight(currentSQL, ";")
			sql = strings.TrimSuffix(sql, `\g`)
			sql = strings.TrimSpace(sql)

			if sql != "" {
				s.executeSQL(sql)
			}
			buffer.Reset()
			s.rl.SetPrompt(buildPrompt(s.profileName))
		} else {
			// Switch to continuation prompt
			s.rl.SetPrompt(continuePromptPad)
		}
	}
}

// handleBackslashCommand processes REPL meta-commands. Returns true if \q.
func (s *ReplSession) handleBackslashCommand(cmd string) bool {
	parts := strings.Fields(cmd)
	if len(parts) == 0 {
		return false
	}

	switch parts[0] {
	case `\q`:
		fmt.Println("Bye!")
		return true

	case `\?`:
		s.printHelp()

	case `\c`:
		if len(parts) < 2 {
			fmt.Println("Usage: \\c <profile_name>")
			return false
		}
		s.profileName = parts[1]
		s.rl.SetPrompt(buildPrompt(s.profileName))
		fmt.Printf("Switched to profile: %s\n", s.profileName)

	case `\clear`:
		fmt.Print("\033[H\033[2J")

	case `\set`:
		if len(parts) < 3 || parts[1] != "tx" {
			fmt.Println("Usage: \\set tx <auto-commit|manual-commit>")
			return false
		}
		if err := s.querySvc.SetTransaction(context.Background(), s.profileName, parts[2], s.handler); err != nil {
			fmt.Printf("Error setting transaction: %v\n", err)
		}

	case `\p`:
		if len(parts) < 2 {
			fmt.Println("Usage: \\p <page_number>")
			return false
		}
		if s.lastSqlId == "" {
			fmt.Println("[ERROR] No previous query to paginate.")
			return false
		}
		var pageNum int
		fmt.Sscanf(parts[1], "%d", &pageNum)
		if pageNum < 1 {
			pageNum = 1
		}
		if err := s.querySvc.FetchPage(context.Background(), s.profileName, s.lastSqlId, pageNum, s.handler); err != nil {
			fmt.Printf("Error fetching page: %v\n", err)
		} else {
			s.lastSqlId = s.handler.GetLastSqlId()
		}

	case `\dt`:
		if s.profileName == "" {
			fmt.Println("[ERROR] Not connected. Use \\c <profile> to connect first.")
			return false
		}
		if err := s.querySvc.ListTables(context.Background(), s.profileName, s.handler); err != nil {
			fmt.Printf("Error listing tables: %v\n", err)
		}

	default:
		fmt.Printf("Unknown command: %s. Type \\? for help.\n", parts[0])
	}

	return false
}

func (s *ReplSession) executeSQL(sql string) {
	if s.profileName == "" {
		fmt.Println("[ERROR] Not connected. Use \\c <profile> to connect first.")
		return
	}

	// Create a cancellable context for this query
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Setup signal listener for Ctrl+C during execution
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	defer signal.Stop(sigChan)

	go func() {
		select {
		case <-sigChan:
			fmt.Println("\n[INTERRUPT] Cancelling query...")
			cancel()
		case <-ctx.Done():
			// Query finished or already cancelled
		}
	}()

	if err := s.querySvc.ExecuteSQL(ctx, s.profileName, sql, s.handler, 20); err != nil {
		if ctx.Err() == context.Canceled {
			fmt.Println("[INFO] Query execution was cancelled by user.")
		} else {
			fmt.Fprintf(os.Stderr, "Execution error: %v\n", err)
		}
	} else {
		// Update lastSqlId for pagination
		s.lastSqlId = s.handler.GetLastSqlId()
	}
}

func (s *ReplSession) printHelp() {
	fmt.Println("Available commands:")
	fmt.Println("  \\q           Quit")
	fmt.Println("  \\c <profile> Connect to / switch profile")
	fmt.Println("  \\set tx <mode> Set transaction mode (auto-commit|manual-commit)")
	fmt.Println("  \\p <n>       Go to page n of the last query")
	fmt.Println("  \\clear       Clear screen")
	fmt.Println("  \\dt          List tables")
	fmt.Println("  \\?           Show this help")
	fmt.Println()
	fmt.Println("SQL statements must end with ; or \\g to execute.")
	fmt.Println("Use Tab for keyword completion. Use ↑/↓ for history.")
}

// isStatementComplete checks if the accumulated SQL ends with ; or \g.
func isStatementComplete(sql string) bool {
	return strings.HasSuffix(sql, ";") || strings.HasSuffix(sql, `\g`)
}

func buildPrompt(profileName string) string {
	if profileName == "" {
		return "sql> "
	}
	return profileName + "> "
}

func getHistoryPath() string {
	home, err := os.UserHomeDir()
	if err != nil {
		return ""
	}
	return filepath.Join(home, ".sql-console", "history")
}

package repl

import (
	"strings"

	"github.com/ergochat/readline"
)

// sqlKeywords contains common SQL keywords for tab completion.
var sqlKeywords = []string{
	"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
	"FROM", "WHERE", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN",
	"LIKE", "IS", "NULL", "AS", "ON", "JOIN", "LEFT", "RIGHT", "INNER",
	"OUTER", "FULL", "CROSS", "GROUP", "BY", "ORDER", "ASC", "DESC",
	"HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT", "INTO",
	"VALUES", "SET", "TABLE", "INDEX", "VIEW", "BEGIN", "COMMIT",
	"ROLLBACK", "SAVEPOINT", "GRANT", "REVOKE", "TRUNCATE",
	"COUNT", "SUM", "AVG", "MIN", "MAX", "CASE", "WHEN", "THEN",
	"ELSE", "END", "CAST", "COALESCE", "PRIMARY", "KEY", "FOREIGN",
	"REFERENCES", "DEFAULT", "CONSTRAINT", "CHECK", "UNIQUE",
	"CASCADE", "EXPLAIN", "ANALYZE", "WITH", "RECURSIVE",
}

// NewCompleter creates a readline completer for SQL keywords and REPL commands.
func NewCompleter(profileNames []string) *readline.PrefixCompleter {
	// Build profile sub-items for \c command
	profileItems := make([]*readline.PrefixCompleter, len(profileNames))
	for i, name := range profileNames {
		profileItems[i] = readline.PcItem(name)
	}

	items := make([]*readline.PrefixCompleter, 0, len(sqlKeywords)*2+5)

	// SQL keywords (both upper and lower case)
	for _, kw := range sqlKeywords {
		items = append(items, readline.PcItem(kw))
		items = append(items, readline.PcItem(strings.ToLower(kw)))
	}

	// Backslash commands
	items = append(items, readline.PcItem(`\q`))
	items = append(items, readline.PcItem(`\?`))
	items = append(items, readline.PcItem(`\clear`))
	items = append(items, readline.PcItem(`\dt`))
	items = append(items, readline.PcItem(`\c`, profileItems...))

	return readline.NewPrefixCompleter(items...)
}

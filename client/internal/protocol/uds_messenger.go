package protocol

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"sync"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type UdsMessenger struct {
	socketPath string
	conn       net.Conn
	parser     *NdjsonParser
	mu         sync.Mutex // protects conn writes
}

func NewUdsMessenger(socketPath string) *UdsMessenger {
	return &UdsMessenger{socketPath: socketPath}
}

func (m *UdsMessenger) connect() error {
	if m.conn != nil {
		return nil
	}
	conn, err := net.Dial("unix", m.socketPath)
	if err != nil {
		return fmt.Errorf("failed to connect to UDS at %s: %w", m.socketPath, err)
	}
	m.conn = conn
	m.parser = NewNdjsonParser(conn)
	return nil
}

func (m *UdsMessenger) Send(ctx context.Context, req domain.Request, handler domain.ResponseHandler) error {
	if err := m.connect(); err != nil {
		return err
	}

	// 1. Send Request
	data, err := json.Marshal(req)
	if err != nil {
		return err
	}

	m.mu.Lock()
	_, err = m.conn.Write(append(data, '\n'))
	m.mu.Unlock()
	if err != nil {
		return err
	}

	// 2. Setup cancellation watcher
	done := make(chan struct{})
	defer close(done)

	go func() {
		select {
		case <-ctx.Done():
			// Send Cancel Request to Daemon
			cancelReq := domain.NewCancelRequest(req.RequestId)
			cancelData, _ := json.Marshal(cancelReq)
			
			m.mu.Lock()
			m.conn.Write(append(cancelData, '\n'))
			m.mu.Unlock()
			
			fmt.Println("\n[INFO] Cancellation request sent to daemon...")
		case <-done:
			// Normal completion
		}
	}()

	// 3. Parse Responses (Streaming) using the persistent parser
	return m.parser.HandleResponses(handler)
}

func (m *UdsMessenger) Close() error {
	if m.conn != nil {
		err := m.conn.Close()
		m.conn = nil
		return err
	}
	return nil
}

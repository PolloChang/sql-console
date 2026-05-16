package domain

import (
	"context"
	"encoding/json"
	"fmt"
)

// Request represents a message sent to the Java Daemon.
type Request struct {
	Version   string      `json:"version"`
	Action    string      `json:"action"`
	RequestId string      `json:"requestId"`
	OSUser    string      `json:"osUser"`
	Payload   interface{} `json:"payload"`
}

// Response represents a message received from the Java Daemon.
type Response struct {
	Version   string          `json:"version"`
	RequestId string          `json:"requestId"`
	Type      string          `json:"type"`
	Payload   json.RawMessage `json:"payload"`
}

// IPCError represents a structured error returned by the Daemon.
type IPCError struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func (e *IPCError) Error() string {
	return fmt.Sprintf("[%s] %s", e.Code, e.Message)
}

// ParseError attempts to parse the payload as an IPCError.
func (r *Response) ParseError() (*IPCError, error) {
	if r.Type != "error" {
		return nil, fmt.Errorf("response type is not error: %s", r.Type)
	}
	var errObj IPCError
	if err := json.Unmarshal(r.Payload, &errObj); err != nil {
		// Fallback for simple string payloads or unexpected structures
		return &IPCError{
			Code:    "ERR-PARSE",
			Message: string(r.Payload),
		}, nil
	}
	return &errObj, nil
}

// NewCancelRequest creates a request to cancel a specific running query.
func NewCancelRequest(requestId string) Request {
	return Request{
		Version:   "1.0",
		Action:    "cancel",
		RequestId: requestId,
	}
}

// ResponseHandler defines callbacks for NDJSON response events.
type ResponseHandler interface {
	OnMetadata(data json.RawMessage)
	OnRow(data json.RawMessage)
	OnFooter(data json.RawMessage)
	OnError(err *IPCError)
}

// Messenger defines the contract for sending requests and receiving responses.
type Messenger interface {
	Send(ctx context.Context, req Request, handler ResponseHandler) error
	Close() error
}

type ExecutionStats struct {
	RowCount      int `json:"rowCount"`
	UpdateCount   int `json:"updateCount"`
	ExecutionTime int `json:"executionTimeMs"`
	TotalRows     int `json:"totalRows"`
	Page          int `json:"page"`
	PageSize      int `json:"pageSize"`
}

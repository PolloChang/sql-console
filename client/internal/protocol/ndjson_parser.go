package protocol

import (
	"bufio"
	"encoding/json"
	"io"

	"github.com/pollolab/sql-console/client/internal/domain"
)

type NdjsonParser struct {
	reader *bufio.Reader
}

func NewNdjsonParser(r io.Reader) *NdjsonParser {
	return &NdjsonParser{
		reader: bufio.NewReader(r),
	}
}

func (p *NdjsonParser) HandleResponses(handler domain.ResponseHandler) error {
	for {
		line, err := p.reader.ReadBytes('\n')
		if err != nil {
			if err == io.EOF {
				return nil
			}
			return err
		}

		var resp domain.Response
		if err := json.Unmarshal(line, &resp); err != nil {
			return err
		}

		switch resp.Type {
		case "success":
			handler.OnMetadata(resp.Payload)
			return nil
		case "header":
			handler.OnMetadata(resp.Payload)
		case "row":
			handler.OnRow(resp.Payload)
		case "footer":
			handler.OnFooter(resp.Payload)
			return nil
		case "error":
			ipcErr, _ := resp.ParseError()
			handler.OnError(ipcErr)
			return ipcErr
		}
	}
}

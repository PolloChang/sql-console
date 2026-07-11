//go:build windows

package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	"golang.org/x/sys/windows/svc"
	"golang.org/x/sys/windows/svc/mgr"
)

const svcName = "SqlConsoleDaemon"

func main() {
	inService, err := svc.IsAnInteractiveSession()
	if err != nil {
		fmt.Printf("Failed to determine if session is interactive: %v\n", err)
		os.Exit(1)
	}

	if !inService {
		// Running under Windows Service Manager (SCM)
		svc.Run(svcName, &daemonService{})
		return
	}

	// Interactive command line usage
	if len(os.Args) < 2 {
		fmt.Println("Usage: sql-daemon-service.exe <install|uninstall|start|stop>")
		os.Exit(1)
	}

	cmd := os.Args[1]
	switch cmd {
	case "install":
		err = installService(svcName, "SQL Console Daemon Service", "Provides UDS backend database connectivity for SQL Console CLI.")
	case "uninstall":
		err = removeService(svcName)
	case "start":
		err = startService(svcName)
	case "stop":
		err = controlService(svcName, svc.Stop, svc.Stopped)
	default:
		fmt.Printf("Unknown command: %s\n", cmd)
		os.Exit(1)
	}

	if err != nil {
		fmt.Printf("Error [%s]: %v\n", cmd, err)
		os.Exit(1)
	}
	fmt.Printf("Service command '%s' completed successfully.\n", cmd)
}

type daemonService struct {
	cmd *exec.Cmd
}

func (m *daemonService) Execute(args []string, r <-chan svc.ChangeRequest, s chan<- svc.Status) (bool, uint32) {
	s <- svc.Status{State: svc.StartPending}

	exePath, _ := os.Executable()
	baseDir := filepath.Dir(exePath)
	matches, _ := filepath.Glob(filepath.Join(baseDir, "sql-console-daemon-*.jar"))
	var jarPath string
	if len(matches) > 0 {
		jarPath = matches[0]
	} else {
		jarPath = filepath.Join(baseDir, "sql-console-daemon-0.3.1.jar")
	}

	m.cmd = exec.Command("java", "-jar", jarPath)
	m.cmd.Dir = baseDir
	// Note: in Windows service, stdout/stderr are discarded unless redirected to log file
	logFile, _ := os.OpenFile(filepath.Join(baseDir, "service-wrapper.log"), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0666)
	if logFile != nil {
		m.cmd.Stdout = logFile
		m.cmd.Stderr = logFile
		defer logFile.Close()
	}

	if err := m.cmd.Start(); err != nil {
		if logFile != nil {
			logFile.WriteString(fmt.Sprintf("%s: Failed to start java process: %v\n", time.Now().Format(time.RFC3339), err))
		}
		s <- svc.Status{State: svc.Stopped}
		return false, 1
	}

	s <- svc.Status{State: svc.Running, Accepts: svc.AcceptStop | svc.AcceptShutdown}

	// Goroutine to monitor process exit
	done := make(chan error, 1)
	go func() {
		done <- m.cmd.Wait()
	}()

	for {
		select {
		case c := <-r:
			switch c.Cmd {
			case svc.Stop, svc.Shutdown:
				s <- svc.Status{State: svc.StopPending}
				if m.cmd != nil && m.cmd.Process != nil {
					m.cmd.Process.Kill()
				}
				return false, 0
			}
		case err := <-done:
			if logFile != nil {
				logFile.WriteString(fmt.Sprintf("%s: Java process exited: %v\n", time.Now().Format(time.RFC3339), err))
			}
			return false, 2
		}
	}
}

func installService(name, desc, longDesc string) error {
	exePath, err := os.Executable()
	if err != nil {
		return err
	}
	m, err := mgr.Connect()
	if err != nil {
		return fmt.Errorf("connect to SCM failed (must run as Administrator): %v", err)
	}
	defer m.Disconnect()

	s, err := m.OpenService(name)
	if err == nil {
		s.Close()
		return fmt.Errorf("service %s already exists", name)
	}

	s, err = m.CreateService(name, exePath, mgr.Config{
		DisplayName: desc,
		Description: longDesc,
		StartType:   mgr.StartAutomatic,
	})
	if err != nil {
		return fmt.Errorf("create service failed: %v", err)
	}
	defer s.Close()
	return nil
}

func removeService(name string) error {
	m, err := mgr.Connect()
	if err != nil {
		return fmt.Errorf("connect to SCM failed (must run as Administrator): %v", err)
	}
	defer m.Disconnect()

	s, err := m.OpenService(name)
	if err != nil {
		return fmt.Errorf("service %s is not installed", name)
	}
	defer s.Close()

	err = s.Delete()
	if err != nil {
		return fmt.Errorf("delete service failed: %v", err)
	}
	return nil
}

func startService(name string) error {
	m, err := mgr.Connect()
	if err != nil {
		return fmt.Errorf("connect to SCM failed: %v", err)
	}
	defer m.Disconnect()

	s, err := m.OpenService(name)
	if err != nil {
		return fmt.Errorf("open service failed: %v", err)
	}
	defer s.Close()

	err = s.Start()
	if err != nil {
		return fmt.Errorf("start service failed: %v", err)
	}
	return nil
}

func controlService(name string, c svc.Cmd, to svc.State) error {
	m, err := mgr.Connect()
	if err != nil {
		return fmt.Errorf("connect to SCM failed: %v", err)
	}
	defer m.Disconnect()

	s, err := m.OpenService(name)
	if err != nil {
		return fmt.Errorf("open service failed: %v", err)
	}
	defer s.Close()

	status, err := s.Control(c)
	if err != nil {
		return fmt.Errorf("send control %d failed: %v", c, err)
	}
	timeout := time.Now().Add(10 * time.Second)
	for status.State != to {
		if time.Now().After(timeout) {
			return fmt.Errorf("timeout waiting for service to transition to state %d", to)
		}
		time.Sleep(300 * time.Millisecond)
		status, err = s.Query()
		if err != nil {
			return fmt.Errorf("query service status failed: %v", err)
		}
	}
	return nil
}

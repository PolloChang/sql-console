package main

import (
	"archive/zip"
	"bytes"
	_ "embed"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
)

var installerVersion = "0.3.1"
//go:embed payload.zip
var payloadArchive []byte

func main() {
	fmt.Println("=============================================")
	fmt.Printf("    SQL Console Windows Installer v%s     \n", installerVersion)
	fmt.Println("=============================================")

	targetDir := "sql-console"
	if len(os.Args) > 1 {
		targetDir = os.Args[1]
	} else {
		fmt.Print("Enter installation directory [default: sql-console]: ")
		var input string
		fmt.Scanln(&input)
		if input != "" {
			targetDir = input
		}
	}

	absDir, err := filepath.Abs(targetDir)
	if err != nil {
		fmt.Printf("Error resolving path: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("\nInstalling SQL Console to: %s\n", absDir)
	if err := os.MkdirAll(absDir, 0755); err != nil {
		fmt.Printf("Error creating directory: %v\n", err)
		os.Exit(1)
	}

	r, err := zip.NewReader(bytes.NewReader(payloadArchive), int64(len(payloadArchive)))
	if err != nil {
		fmt.Printf("Error opening payload: %v\n", err)
		os.Exit(1)
	}

	for _, f := range r.File {
		fpath := filepath.Join(absDir, f.Name)
		if f.FileInfo().IsDir() {
			os.MkdirAll(fpath, os.ModePerm)
			continue
		}

		if err := os.MkdirAll(filepath.Dir(fpath), os.ModePerm); err != nil {
			fmt.Printf("Error creating dir: %v\n", err)
			continue
		}

		outFile, err := os.OpenFile(fpath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			fmt.Printf("Error extracting %s: %v\n", f.Name, err)
			continue
		}

		rc, err := f.Open()
		if err != nil {
			outFile.Close()
			continue
		}

		_, err = io.Copy(outFile, rc)
		outFile.Close()
		rc.Close()
		if err != nil {
			fmt.Printf("Error writing %s: %v\n", f.Name, err)
		} else {
			fmt.Printf(" - Extracted: %s\n", f.Name)
		}
	}

	fmt.Println("\nConfiguring Windows Environment...")
	setupWindowsEnvironment(absDir)

	fmt.Println("\nInstallation Complete! 🎉")
	fmt.Println("To get started:")
	fmt.Println("1. [Interactive Mode]: Open Start Menu and click 'SQL Console Daemon' to start the backend service.")
	fmt.Println("2. [Background Service]: To install as an automatic background Windows Service, run as Administrator:")
	fmt.Printf("   %s install && %s start\n", filepath.Join(absDir, "sql-daemon-service.exe"), filepath.Join(absDir, "sql-daemon-service.exe"))
	fmt.Println("3. Open a new Command Prompt or PowerShell and type 'sql -version'.")
	fmt.Println("\nPress Enter to exit...")
	fmt.Scanln()
}

func setupWindowsEnvironment(absDir string) {
	psScript := fmt.Sprintf(`
$absDir = "%s"
$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -notlike "*$absDir*") {
    $newPath = $userPath
    if ($newPath -ne "" -and ($newPath.Substring($newPath.Length - 1) -ne ";")) {
        $newPath += ";"
    }
    $newPath += $absDir
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
    Write-Host " - Added $absDir to User PATH"
} else {
    Write-Host " - $absDir is already in User PATH"
}

$WshShell = New-Object -comObject WScript.Shell
$startMenu = [Environment]::GetFolderPath("StartMenu")
$shortcutDir = "$startMenu\Programs"
$shortcutPath = "$shortcutDir\SQL Console Daemon.lnk"
$Shortcut = $WshShell.CreateShortcut($shortcutPath)
$Shortcut.TargetPath = "$absDir\start-daemon.bat"
$Shortcut.WorkingDirectory = "$absDir"
$Shortcut.IconLocation = "$env:SystemRoot\System32\shell32.dll,25"
$Shortcut.Description = "Launch SQL Console Backend Service"
$Shortcut.Save()
Write-Host " - Created Start Menu shortcut: SQL Console Daemon"
`, absDir)

	cmd := exec.Command("powershell", "-NoProfile", "-NonInteractive", "-Command", psScript)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Run()
}


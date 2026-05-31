# CHANGELOG

## [v0.3.0]-2026-05-31

* Feature: Support Oracle Database special commands (`show parameter` and `desc`/`describe`).
* Security: Implement whitelist validation input regex to defend against SQL Injection in client commands.
* SOLID: Refactor `ClientCommandProcessor` to a pure stateless translator pattern and clean up execution engine logic.

## [v0.2.1]-2026-05-19

* fix: select from oracle error
* add AI Agent Skill for sql-console: [sql-operator](https://github.com/PolloChang/sql-operator)

## [v0.2.0]-2026-05-17

* Add Windows Support 

1. Install Java 21+ on your Windows environment.
2. Run the standalone installer `sql-console-windows-installer_0.2.0_amd64.exe`:

```cmd
=============================================
    SQL Console Windows Installer v0.2.0
=============================================
Enter installation directory [default: sql-console]: C:\var\sql-console

Installing SQL Console to: C:\var\sql-console
 - Extracted: sql.exe
 - Extracted: README-Windows.txt
 - Extracted: start-daemon.bat
 - Extracted: sql-console-daemon-0.1.0.jar
 - Extracted: sql-daemon-service.exe

Configuring Windows Environment...
 - C:\var\sql-console is already in User PATH
 - Created Start Menu shortcut: SQL Console Daemon

Installation Complete! ??
To get started:
1. [Interactive Mode]: Open Start Menu and click 'SQL Console Daemon' to start the backend service.
2. [Background Service]: To install as an automatic background Windows Service, run as Administrator:
   C:\var\sql-console\sql-daemon-service.exe install && C:\var\sql-console\sql-daemon-service.exe start
3. Open a new Command Prompt or PowerShell and type 'sql -version'.

Press Enter to exit...
```

* Install Service

Enter admin mode, run `sql-daemon-service.exe install`, and then type `sql-daemon-service.exe start`.

```cmd
sql-daemon-service.exe install
```

* Stop Service

```cmd
sql-daemon-service.exe stop
```

* uninstall

Enter admin mode, run `sql-daemon-service.exe uninstall`.

```cmd
sql-daemon-service.exe uninstall
```


## [v0.1.0]-2026-05-16

### Features

* Support Postgresql default
* Support Debain Base default

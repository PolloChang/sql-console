#!/bin/bash
set -e

# SQL Console Master Setup Script
VERSION=$(grep "version =" daemon-service/app/build.gradle | cut -d"'" -f2)

echo "--- SQL Console Setup (v${VERSION}) ---"

# 1. Build Java Daemon
echo "[1/2] Building Java Daemon..."
cd daemon-service
./gradlew clean app:jar
cd ..

# 2. Build Go Client
echo "[2/2] Building Go Client..."
cd client
/usr/local/go/bin/go build -ldflags "-X main.version=${VERSION}" -o sql cmd/sql/main.go
cd ..

echo "--------------------------------------"
echo "Build Successful!"
echo ""
echo "To install the daemon as a system service (Linux):"
echo "1. Copy daemon-service/app/build/libs/*.jar to /opt/sql-console/daemon-service.jar"
echo "2. Copy daemon-service/libs/sql-console-daemon.service to /etc/systemd/system/"
echo "3. Copy daemon-service/libs/sql-console-rsyslog.conf to /etc/rsyslog.d/40-sql-console.conf"
echo "4. Copy daemon-service/libs/sql-console-logrotate to /etc/logrotate.d/sql-console"
echo "5. sudo systemctl daemon-reload"
echo "6. sudo systemctl restart rsyslog"
echo "7. sudo systemctl enable --now sql-console-daemon"
echo ""
echo "To use the CLI client:"
echo "  alias sql='$(pwd)/client/sql'"
echo ""
echo "Try running: sql profile list"
echo "--------------------------------------"

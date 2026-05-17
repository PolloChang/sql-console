#!/bin/bash
set -e

PROJECT_ROOT=$(pwd)
VERSION=$(grep "version =" daemon-service/app/build.gradle | cut -d"'" -f2)
PACKAGE_NAME="sql-console"
BUILD_DIR="package-config/deb-build"
RELEASE_DIR="release"

echo "Building $PACKAGE_NAME version $VERSION..."

# 1. Build artifacts
./SETUP.sh

# 2. Prepare build directory
mkdir -p $BUILD_DIR/DEBIAN
mkdir -p $BUILD_DIR/usr/bin
mkdir -p $BUILD_DIR/usr/lib/sql-console/libs
mkdir -p $BUILD_DIR/etc/systemd/system
mkdir -p $BUILD_DIR/etc/rsyslog.d
mkdir -p $BUILD_DIR/etc/logrotate.d
mkdir -p $BUILD_DIR/var/log/sql-console

# 3. Copy artifacts
cp client/sql $BUILD_DIR/usr/lib/sql-console/sql-client
cp daemon-service/app/build/libs/sql-console-daemon-${VERSION}.jar $BUILD_DIR/usr/lib/sql-console/sql-console-daemon.jar
cp daemon-service/libs/sql-console-daemon.service $BUILD_DIR/etc/systemd/system/
cp daemon-service/libs/sql-console-rsyslog.conf $BUILD_DIR/etc/rsyslog.d/40-sql-console.conf
cp daemon-service/libs/sql-console-logrotate $BUILD_DIR/etc/logrotate.d/sql-console

# 4. Copy Man Page
mkdir -p $BUILD_DIR/usr/share/man/man1
cp daemon-service/libs/sql.1 $BUILD_DIR/usr/share/man/man1/
gzip -f $BUILD_DIR/usr/share/man/man1/sql.1

# 5. Update control file version
sed -i "s/Version: .*/Version: $VERSION/" $BUILD_DIR/DEBIAN/control

# 5. Build package
mkdir -p $RELEASE_DIR
dpkg-deb --root-owner-group --build $BUILD_DIR $RELEASE_DIR/${PACKAGE_NAME}_${VERSION}_amd64.deb

# 6. Build Windows Standalone Installer
echo "Building Windows Standalone Installer..."
WIN_PAYLOAD="package-config/win-installer/payload"
mkdir -p $WIN_PAYLOAD

# Build Windows client executable
echo " - Building Windows client (sql.exe)..."
cd client
GOOS=windows GOARCH=amd64 /usr/local/go/bin/go build -o ../$WIN_PAYLOAD/sql.exe cmd/sql/main.go

# Build Windows service wrapper
echo " - Building Windows service wrapper (sql-daemon-service.exe)..."
GOOS=windows GOARCH=amd64 /usr/local/go/bin/go build -o ../$WIN_PAYLOAD/sql-daemon-service.exe cmd/win-service/main.go
cd ..

# Copy Java Daemon JAR
cp daemon-service/app/build/libs/sql-console-daemon-${VERSION}.jar $WIN_PAYLOAD/

# Create start-daemon.bat
cat << 'EOF' > $WIN_PAYLOAD/start-daemon.bat
@echo off
title SQL Console Daemon Backend
echo Starting SQL Console Daemon...
for %%f in (sql-console-daemon-*.jar) do (
    java -jar "%%f"
    goto :end
)
:end
pause
EOF

# Create README-Windows.txt
cat << 'EOF' > $WIN_PAYLOAD/README-Windows.txt
=========================================
      SQL Console - Windows Guide
=========================================

1. Interactive Mode (Standalone):
   Double-click the "SQL Console Daemon" shortcut in your Start Menu (or run start-daemon.bat).
   Then open Command Prompt or PowerShell and run `sql`.

2. Windows Background Service:
   Open Command Prompt or PowerShell as Administrator and run:
   sql-daemon-service.exe install
   sql-daemon-service.exe start

3. Managing Connection Profiles:
   Add profile: sql profile add my_db jdbc:postgresql://host:5432/db user pass
   List profiles: sql profile list
   Connect: sql -p my_db
EOF

# Create payload.zip
echo " - Packaging Windows payload archive..."
cd $WIN_PAYLOAD
zip -q -r ../payload.zip ./*
cd ../../..

# Build Windows installer executable
echo " - Building Windows installer executable..."
WIN_RELEASE_EXE="${RELEASE_DIR}/${PACKAGE_NAME}-windows-installer_${VERSION}_amd64.exe"
cd package-config/win-installer
GOOS=windows GOARCH=amd64 /usr/local/go/bin/go build -ldflags "-X main.installerVersion=${VERSION}" -o ../../$WIN_RELEASE_EXE main.go
# Clean up temporary payload directory and zip
rm -rf payload payload.zip
cd ../..

echo "--------------------------------------"
echo " Packages created successfully!"
echo " Debian Package : $RELEASE_DIR/${PACKAGE_NAME}_${VERSION}_amd64.deb"
echo " Windows Installer: $WIN_RELEASE_EXE"
echo "--------------------------------------"

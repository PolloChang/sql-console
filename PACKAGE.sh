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

echo "--------------------------------------"
echo " Package created successfully!"
echo " Location: $RELEASE_DIR/${PACKAGE_NAME}_${VERSION}_amd64.deb"
echo "--------------------------------------"

---
name: SETUP
description: >
  sql-console 開發與執行環境初始化指南。
---

# 系統安裝與設定指南 (SETUP)

## 1. 環境需求 (Prerequisites)

### 核心編譯環境
- **Java**: 21+ (建議使用 Temurin 21)
- **Go**: 1.20+ (建議使用 1.26+)
- **OS**: Linux (支援 Debian 12+, Ubuntu 22.04+)

### 資料庫依賴 (libs/)
- 驅動程式需手動放置於 `daemon-service/libs/`：
    - `ojdbc8.jar` (Oracle)
    - `postgresql-*.jar` (PostgreSQL - 已內建於 Gradle 配置中可自動下載)
    - `mysql-connector-java-*.jar` (MySQL)

## 2. 快速啟動 (Quick Start)

使用專案根目錄的 `SETUP.sh` 腳本自動完成編譯：
```bash
./SETUP.sh
```

## 3. 部署 Java Daemon (Production)
1. 將編譯出的 JAR 檔 (`sql-console-daemon.jar`) 複製到 `/opt/sql-console/`
2. 使用 `daemon-service/libs/sql-console-daemon.service` 配置 Systemd 服務。
3. 執行 `sudo systemctl enable --now sql-console-daemon`。

## 4. 部署 CLI Client (Production)
1. 將編譯出的 `sql` 執行檔複製到 `/opt/sql-console/bin/`
2. 建立軟連結至 `/usr/local/bin/` 以便全域執行：
   ```bash
   sudo ln -s /opt/sql-console/bin/sql /usr/local/bin/sql
   ```

## 5. CLI 基本操作

### 配置連線
```bash
# 新增 Profile
sql profile add my-db jdbc:postgresql://localhost:5432/mydb username password

# 列出 Profile
sql profile list
```

### 互動式 REPL 模式
直接執行 `sql -p <profile>` 進入互動模式：
```sql
my-db> SELECT * FROM users;
my-db> \dt            -- 瀏覽資料表
my-db> \p 2           -- 翻頁
my-db> \set tx manual -- 切換事務模式
my-db> \q            -- 退出
```

### 結果匯出
```bash
# 匯出至 Excel
sql -p my-db -o report.xlsx "SELECT * FROM orders"

# 匯出至 CSV
sql -p my-db -o report.csv "SELECT * FROM orders"
```

## 6. 建立 Debian 套件

使用專案根目錄的 `PACKAGE.sh` 腳本自動完成編譯與打包：
```bash
./PACKAGE.sh
```


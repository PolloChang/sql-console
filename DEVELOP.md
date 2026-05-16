---
name: DEVELOP
description: >
  sql-console 架構說明與開發指南。
---

# 開發者指南 (DEVELOP)

## 1. 架構概述
本專案採用 **Go Client + Java Daemon** 的雙語架構，透過 **Unix Domain Socket (UDS)** 進行通訊。

- **client/**: 使用 Go 1.20+ 實作。負責 CLI 解析、NDJSON 渲染與文件匯出 (Excel/CSV)。
- **daemon-service/**: 使用 Java 21 + Gradle 實作。負責 JDBC 連線池管理、SQL 執行與結果集序列化。

## 2. 通訊協定 (IPC)
採用 **NDJSON (Newline Delimited JSON)** 格式。
詳見 [ADR-001: Go-Java 雙語架構與 IPC 協定](adr/20260513-architecture-and-ipc.md)。

## 3. 開發規範
- **Java**: 嚴格遵守 Java 21 語法 (Records, Sealed Classes)，使用 SLF4J 進行日誌紀錄。
- **Go**: 使用標準 `flag` 或 `pflag` 進行參數解析，避免過度依賴外部框架。
- **Package**: `work.pollochang.app.sqlconsole`

## 4. 稽核日誌
稽核日誌必須輸出至 `logs/audit.log`，且對敏感資訊 (PASSWORD, IDENTIFIED BY) 進行遮罩。
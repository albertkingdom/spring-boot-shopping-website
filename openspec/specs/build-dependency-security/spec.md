# Capability: 前端工具鏈依賴安全

## Purpose

規範前端工具鏈面對已知壓縮請求安全漏洞時的依賴安裝與驗收要求，確保開發服務能處理正常請求，且異常壓縮輸入不會造成程序終止，並保持既有前端測試與建置相容。

## Requirements

### Requirement: 壓縮請求不得終止前端工具鏈程序

前端工具鏈 SHALL 使用已修補 GHSA-27r3-jxmh-p828 的 body-parser 版本；乾淨安裝 SHALL 可重現，且處理帶有尾端資料的壓縮請求時不得因未捕捉例外終止程序。

#### Scenario: 乾淨安裝修補版本

- WHEN 從版本庫 lockfile 執行 npm ci
- THEN 所有 body-parser 安裝節點均為已修補的 1.20.9，既有前端測試與 production build 成功

#### Scenario: 正常與異常壓縮輸入

- GIVEN 本機使用預設 body-parser JSON middleware，僅綁定 loopback
- WHEN 送出正常 gzip JSON 與附帶非 gzip 尾端資料的 gzip JSON
- THEN 正常請求回應 200；異常請求可以成功處理或回應 400，但程序不得終止，後續正常請求仍回應 200

# Solana Wallet Scout

過去に上昇したSolanaトークンから初期購入ウォレットを抽出し、再現性・活動状況・botリスクで候補を採点するための検証用サービスです。売買は行いません。

## MVPの流れ

1. 上昇銘柄のMintアドレスと上昇開始時刻を登録する
2. Helius `getTransactionsForAddress` で対象期間の取引を取得する
3. `getTransaction` の残高差分から初期購入ウォレットを抽出する
4. 各ウォレットの直近30日履歴を集計する
5. 0〜100点で採点し、bot・取引所・開発者候補を別枠にする
6. CSVで人間が確認できる候補一覧を出す

## 現在実装済み

- Spring Boot/PostgreSQLの土台
- Flywayスキーマ
- 説明可能なWallet Score計算
- botリスクによる減点
- 採点ユニットテスト
- Helius `getTransactionsForAddress` クライアント
- トークン残高の前後差分による初期購入者抽出
- 初期購入者の重複排除と購入時刻順ランキング
- `POST /api/scans/preview` プレビューAPI

## 初期購入者をプレビューする

```powershell
$body = @{
  tokenMint = "対象Mintアドレス"
  from = "2026-01-01T00:00:00Z"
  to = "2026-01-01T03:00:00Z"
  candidateLimit = 20
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/scans/preview `
  -ContentType application/json -Body $body
```

現在のプレビューAPIは、Mintに関係する取引のうち対象トークン残高が初めて増加したownerを候補にします。流動性プール、開発者、取引所、単純送金の除外は次の段階で追加します。

## 起動準備

`.env.example` を `.env` にコピーし、`HELIUS_API_KEY` を設定します。APIキーをGitへコミットしないでください。

```powershell
mvn spring-boot:run
```

通常起動ではインメモリH2を使うため、Dockerは不要です。保存データはアプリ終了時に消えます。

PostgreSQLへ永続保存する場合だけ、Dockerを起動して `postgres` プロファイルを指定します。

```powershell
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## 採点の注意

初期値は検証用です。実際の成績を保証する基準ではありません。ROIは取得時点の価格データだけでは正確に復元できないため、将来の価格スナップショットまたは別の価格データソースを追加して検証します。

## 参照する公式仕様

- Helius `getTransactionsForAddress`（履歴・バックフィル）
- Solana `getTransaction`（トークン残高の前後差分）

旧Enhanced Transactions APIは保守モードのため、新規の中心機能には使用しません。

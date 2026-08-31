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
- System Program所有でないアドレスの除外
- 総供給量10%以上の初期配布候補の除外
- 同一秒の購入に開発者・LP・スナイパーのリスク表示
- ウォレットの過去1〜90日アクティビティ分析
- 取引回数・活動日数・銘柄数・保有時間・短時間売買率の算出
- botリスクと暫定Activity Scoreの算出
- 初期購入候補を順番に分析するバックグラウンドランキング

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

プレビューAPIは、Mintに関係する取引のうち対象トークン残高が初めて増加したownerを候補にします。通常ウォレットではないアドレスと総供給量10%以上の初期配布候補は `excluded` に分け、判定理由を返します。同一秒の購入はリスク表示しますが、誤判定を避けるため自動除外しません。

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

## ウォレットの30日分析

```powershell
Invoke-RestMethod "http://localhost:8080/api/wallets/ウォレットアドレス/analysis?days=30"
```

勝率と利益銘柄数は過去時点の価格データが必要です。現在は誤った利益判定を避けるため、`profitabilityStatus` を `NOT_CALCULATED_REQUIRES_HISTORICAL_PRICES` として返します。

## 初期購入ウォレットを一括ランキング

`POST /api/rankings` にMint、ローンチ期間、候補数、履歴日数を送信します。レスポンスの `id` を使い、`GET /api/rankings/{id}` で進捗とランキングを確認します。処理はHeliusの無料枠に配慮して1ウォレットずつ実行します。アプリを終了するとジョブ履歴は消えます。

## 採点の注意

初期値は検証用です。実際の成績を保証する基準ではありません。ROIは取得時点の価格データだけでは正確に復元できないため、将来の価格スナップショットまたは別の価格データソースを追加して検証します。

## 追跡ウォレット

以下は、過去の上昇銘柄の初期購入者候補から抽出し、今後の購入傾向を継続観察するウォレットです。掲載は投資推奨や将来の収益性を保証するものではありません。

| ウォレット | 発見元 | Activity Score | botリスク | 直近30日の概要 | 追跡開始日 |
| --- | --- | ---: | ---: | --- | --- |
| `H5DQ7AoUMcpuvZHhDUabHDRJ6CWtAxBQEBnnT2rRnDmw` | OnlyMarms初期購入候補 | 93.80 | 12.00 | 427取引、22活動日、73銘柄、保有時間中央値367.52分 | 2026-09-01 |
| `EZYagrvcgDeoR1SKhyQqix9L2SGUPudpiZ3L3CJDpvTu` | OnlyMarms初期購入候補 | 78.11 | 13.33 | 566取引、22活動日、83銘柄、保有時間中央値223.62分 | 2026-09-01 |

### 追跡時の注意

- Activity Score 60以上、botリスク35以下、活動日数5日以上、取引銘柄3種類以上、保有時間中央値10分以上を暫定基準にします。
- Heliusのトークン残高増加は、DEX購入だけでなく送金やエアドロップを含む可能性があります。
- 初期配布・開発者・LP・取引所・スナイパー候補を追加確認し、疑わしい場合は追跡対象から外します。
- 購入を確定するには、同一取引内のSOL・USDC・USDT支出とDEXプログラムを照合します。

## 参照する公式仕様

- Helius `getTransactionsForAddress`（履歴・バックフィル）
- Solana `getTransaction`（トークン残高の前後差分）

旧Enhanced Transactions APIは保守モードのため、新規の中心機能には使用しません。

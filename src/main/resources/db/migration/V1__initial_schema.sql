create table scan_job (
  id uuid primary key,
  token_mint varchar(64) not null,
  status varchar(24) not null,
  requested_at timestamp with time zone not null,
  completed_at timestamp with time zone,
  error_message text
);

create table wallet_candidate (
  id bigserial primary key,
  scan_job_id uuid not null references scan_job(id) on delete cascade,
  wallet_address varchar(64) not null,
  first_buy_at timestamp with time zone,
  first_buy_amount numeric(38,12),
  trades_30d integer not null,
  distinct_tokens_30d integer not null,
  profitable_tokens_30d integer,
  median_holding_minutes numeric(18,2),
  bot_risk numeric(5,2) not null,
  score numeric(5,2) not null,
  reasons text not null,
  unique(scan_job_id, wallet_address)
);

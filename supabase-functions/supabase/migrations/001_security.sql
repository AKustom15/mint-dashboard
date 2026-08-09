-- ═══════════════════════════════════════════════════════════════════════════
-- Fase 2 — Tablas de seguridad
-- Pegar en: Supabase Dashboard → SQL Editor → Run
--
-- Ninguna de estas tablas es accesible desde el cliente: no tienen políticas
-- RLS, así que solo la service_role key (las edge functions) puede tocarlas.
-- ═══════════════════════════════════════════════════════════════════════════

-- ── Nonces de un solo uso, emitidos por el servidor ────────────────────────
-- Sin esto, un token de Play Integrity capturado una vez sirve para siempre.
create table if not exists public.integrity_nonces (
  nonce      text primary key,
  created_at timestamptz not null default now(),
  used_at    timestamptz
);

create index if not exists integrity_nonces_created_idx
  on public.integrity_nonces (created_at);

alter table public.integrity_nonces enable row level security;

-- ── Registro de solicitudes aceptadas ──────────────────────────────────────
-- Trazabilidad: qué instalación pidió qué, y con qué veredicto pasó.
-- Sirve para cruzar con los emails que te llegan.
create table if not exists public.icon_request_log (
  id           bigserial primary key,
  request_id   text unique not null,
  install_id   text not null,
  packages     text[] not null,
  verdict      text not null,
  cert_ok      boolean not null default false,
  created_at   timestamptz not null default now()
);

create index if not exists icon_request_log_install_idx
  on public.icon_request_log (install_id, created_at desc);

alter table public.icon_request_log enable row level security;

-- ── Titularidad (se usará al validar compras premium en servidor) ──────────
create table if not exists public.entitlements (
  install_id      text primary key,
  purchase_token  text unique,
  product_id      text,
  google_order_id text unique,
  verdict         text not null,
  cert_digest     text,
  created_at      timestamptz not null default now(),
  last_seen_at    timestamptz not null default now(),
  revoked_at      timestamptz
);

alter table public.entitlements enable row level security;

-- ── Limpieza automática de nonces caducados ────────────────────────────────
-- Requiere la extensión pg_cron. Si no está disponible en tu plan, puedes
-- omitir esto: la tabla crecería unos pocos MB al año, nada preocupante.
--
--   create extension if not exists pg_cron;
--
--   select cron.schedule(
--     'purge-nonces', '*/15 * * * *',
--     $$delete from public.integrity_nonces
--       where created_at < now() - interval '1 hour'$$
--   );

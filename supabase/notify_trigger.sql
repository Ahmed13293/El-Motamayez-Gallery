-- Postgres trigger that fires the notify-new-order Edge Function on every new order.
-- Run once in the Supabase SQL Editor (Dashboard → SQL Editor → New Query).
-- Requires the pg_net extension (enabled by default on Supabase).
--
-- Replace <SUPABASE_URL> and <SUPABASE_ANON_KEY> with your project values:
--   URL  : https://ekcpmpkudcyuqcihzzqw.supabase.co
--   Key  : found in Project Settings → API → anon / public

CREATE EXTENSION IF NOT EXISTS pg_net;

CREATE OR REPLACE FUNCTION notify_new_order()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  PERFORM net.http_post(
    url     := 'https://ekcpmpkudcyuqcihzzqw.supabase.co/functions/v1/notify-new-order',
    body    := jsonb_build_object('record', row_to_json(NEW)::jsonb),
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer <SUPABASE_ANON_KEY>'
    )
  );
  RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER on_new_order
AFTER INSERT ON orders
FOR EACH ROW EXECUTE FUNCTION notify_new_order();

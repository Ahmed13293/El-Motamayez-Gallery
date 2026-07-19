-- Run this in the Supabase SQL Editor to enable push notifications

CREATE TABLE IF NOT EXISTS push_tokens (
  id         uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  token      text        NOT NULL UNIQUE,
  platform   text        NOT NULL CHECK (platform IN ('android', 'web')),
  created_at timestamptz DEFAULT now()
);

-- No RLS — internal app, anon key can insert/read tokens
ALTER TABLE push_tokens DISABLE ROW LEVEL SECURITY;

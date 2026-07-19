-- Run in Supabase SQL Editor
-- Orders table needs the same anon grants as receipts

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON public.orders TO anon;

-- Policy (if not already set)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'orders' AND policyname = 'allow_all_orders'
  ) THEN
    EXECUTE 'CREATE POLICY "allow_all_orders" ON public.orders FOR ALL TO anon USING (true) WITH CHECK (true)';
  END IF;
END $$;

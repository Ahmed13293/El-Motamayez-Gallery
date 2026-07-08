-- Run in Supabase SQL editor
ALTER TABLE receipts ADD COLUMN IF NOT EXISTS is_paid BOOLEAN NOT NULL DEFAULT true;

-- Grant permissions
GRANT UPDATE ON public.receipts TO anon;
GRANT INSERT ON public.receipts TO anon;
GRANT SELECT ON public.receipts TO anon;

-- Policy (if not already set)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'receipts' AND policyname = 'allow_all_receipts'
  ) THEN
    EXECUTE 'CREATE POLICY "allow_all_receipts" ON public.receipts FOR ALL TO anon USING (true) WITH CHECK (true)';
  END IF;
END $$;

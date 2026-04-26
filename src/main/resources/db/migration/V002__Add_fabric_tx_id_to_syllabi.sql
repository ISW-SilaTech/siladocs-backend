-- Add fabricTxId column to syllabi table if it doesn't exist
-- Check if column exists first before adding
-- Note: Column may not exist initially if table is created after migration runs
ALTER TABLE IF EXISTS syllabi ADD COLUMN IF NOT EXISTS fabric_tx_id VARCHAR(255);

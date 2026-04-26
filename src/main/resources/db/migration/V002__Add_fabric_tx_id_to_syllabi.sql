-- Add fabricTxId column to syllabi table
ALTER TABLE syllabi ADD COLUMN IF NOT EXISTS fabric_tx_id VARCHAR(255);

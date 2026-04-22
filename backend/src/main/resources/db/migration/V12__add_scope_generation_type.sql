-- Migration V12: Add SCOPE_GENERATION to generation_type CHECK constraint
-- This allows AI scope generation events to be tracked in ai_generations table

-- Step 1: Drop old CHECK constraint
ALTER TABLE ai_generations DROP CONSTRAINT IF EXISTS ck_ai_generations_type;

-- Step 2: Recreate CHECK constraint with new value
ALTER TABLE ai_generations ADD CONSTRAINT ck_ai_generations_type
    CHECK (generation_type IN ('FOLLOW_UP_QUESTION', 'GAP_ANALYSIS', 'COMPLETION_SUMMARY', 'SCOPE_GENERATION'));

-- Step 3: Update column comment for documentation
COMMENT ON COLUMN ai_generations.generation_type IS 'Types: FOLLOW_UP_QUESTION, GAP_ANALYSIS, COMPLETION_SUMMARY, SCOPE_GENERATION';

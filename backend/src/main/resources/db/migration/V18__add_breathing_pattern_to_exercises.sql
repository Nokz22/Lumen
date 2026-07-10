-- Guided breathing pattern, in seconds per phase. Only meaningful for BREATHING
-- category exercises; every other category stays NULL until guided sessions
-- extend beyond breathing.
ALTER TABLE exercises ADD COLUMN inhale_seconds INTEGER;
ALTER TABLE exercises ADD COLUMN hold_after_inhale_seconds INTEGER;
ALTER TABLE exercises ADD COLUMN exhale_seconds INTEGER;
ALTER TABLE exercises ADD COLUMN hold_after_exhale_seconds INTEGER;

UPDATE exercises
SET inhale_seconds = 4, hold_after_inhale_seconds = 7, exhale_seconds = 8, hold_after_exhale_seconds = 0
WHERE id = '33333333-3333-3333-3333-333333333301'; -- 4-7-8 Breathing

UPDATE exercises
SET inhale_seconds = 4, hold_after_inhale_seconds = 4, exhale_seconds = 4, hold_after_exhale_seconds = 4
WHERE id = '33333333-3333-3333-3333-333333333302'; -- Box Breathing

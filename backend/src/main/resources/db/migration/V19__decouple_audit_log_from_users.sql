-- Erasure (GDPR Article 17) deletes the users row, but the audit trail has to survive it:
-- Article 5(2) accountability means being able to show that an erasure happened at all.
-- The two foreign keys made those goals mutually exclusive — the trail could only survive
-- if the user did.
--
-- Dropping them keeps every entry while leaving nothing behind that identifies a person:
-- once the users row and every other table referencing it are gone, actor_user_id and
-- subject_user_id are opaque UUIDs that resolve to nobody. Pseudonymous data becomes
-- anonymous data precisely when the key that re-identifies it no longer exists.
--
-- See ADR-0011.
ALTER TABLE audit_log_entries DROP CONSTRAINT audit_log_entries_actor_user_id_fkey;
ALTER TABLE audit_log_entries DROP CONSTRAINT audit_log_entries_subject_user_id_fkey;

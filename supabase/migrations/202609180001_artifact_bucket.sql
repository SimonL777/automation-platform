-- Apply only to this service's dedicated Supabase project.
-- Private objects are mediated by the authenticated Java API, never public URLs.
INSERT INTO storage.buckets(id,name,public)
VALUES ('automation-platform-artifacts','automation-platform-artifacts',false)
ON CONFLICT(id) DO NOTHING;

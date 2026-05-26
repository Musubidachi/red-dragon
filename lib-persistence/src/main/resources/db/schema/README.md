# db/schema/ (deprecated — moved to db/drafts/)

This folder previously held pre-migration schema drafts. They have been moved
to `../drafts/` to make their non-runtime status explicit (Flyway never scans
this folder, but the name "schema" misled readers into thinking it was
authoritative).

If you are looking for the runtime DDL, see `../migration/`. If you are
looking for the historical drafts, see `../drafts/`.

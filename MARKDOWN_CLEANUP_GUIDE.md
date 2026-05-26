# Markdown Cleanup Guide

This guide is for future documentation-only cleanup passes. It exists to keep
weaker-model edits from rewriting project history or inventing implementation
status.

## Rules

1. Read the relevant code and module docs before changing status language.
2. Preserve the distinction between implemented, open, fixed, and design-only
   work. Do not describe aspirational work as shipped.
3. Prefer the newest module-level review or README over stale historical audit
   text, but do not erase useful rationale without summarizing it.
4. Put current open work first. Move fixed findings into a compact historical
   section.
5. Keep repo-wide docs concise. Link to module review files for detail instead
   of duplicating long audit narratives.
6. Fix obvious encoding artifacts when touched, including mojibake forms of em
   dashes, arrows, section symbols, and replacement characters. Prefer plain
   ASCII punctuation.
7. Do not modify Java code, migrations, POM files, generated output, or runtime
   configuration during a markdown cleanup unless the user explicitly expands
   the task.
8. Avoid "done" language unless the code or current docs prove it. Use
   "planned", "deferred", "design-only", or "not wired" when that is the
   current state.
9. Keep links relative and verify that linked files exist.
10. When old audit details contradict a newer fix-status section, keep the newer
    status and compress the old detail into historical notes.

## Document Roles

`README.md`

* Current project overview.
* Current module role and implemented surface.
* Remaining work before each module reaches its intended role.
* Links to deeper module docs.

`REVIEW.md`

* Current repo-wide work-remains summary.
* Open work grouped by module.
* Links to detailed module reviews.
* Historical fixed findings only when they explain remaining risk.

`ISSUES.md`

* Actionable issue index.
* Grouped by priority/severity and module.
* Each issue should have a stable title, status, impact, and source link.
* Use this as the checklist; use `REVIEW.md` for narrative context.

Module `REVIEW.md`

* Lead with `Current Open Work`.
* Include a short implemented-surface summary.
* Compress fixed audit history into `Historical Fixes`.
* Keep deferred design work separate from implementation gaps.

## Suggested Checks

Run these after a cleanup pass:

```powershell
rg "<mojibake-or-replacement-character-pattern>" -g "*.md"
rg "^#|TODO|Open|Fixed" -g "*.md"
rg "\]\(" -g "*.md"
```

For markdown-only edits, Java compilation is usually unnecessary. If the edit
claims code behavior changed, run the relevant tests or compile the reactor.

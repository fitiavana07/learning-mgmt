# Backup / restore proposal

Modeled on the backup/restore of `accounting-android-19`. Restore is a full
replace (clear, then insert), not a partial import.

## Reference: how accounting does it

- `BackupRepository`: `export()` returns one pretty-printed JSON string.
  `restore(json)` returns a sealed `RestoreResult`: `Success`,
  `SchemaMismatch(backupVersion, currentVersion)` or `Error(message)`.
- JSON header: `schemaVersion` (the Room DB version) and `exportedAt`, then one
  array per table.
- Restore is strict:
  - Parse and validate everything first, so a bad file touches nothing.
  - Then, in one DB transaction, delete children before parents and insert
    parents before children.
  - A different schema version is rejected outright (no cross-version import).
- UI:
  - Top app bar menu with Backup and Restore.
  - Backup uses `CreateDocument("application/json")` with a timestamped
    filename.
  - Restore uses `OpenDocument`, then a confirm dialog ("This will erase all
    current data... cannot be undone").
  - IO runs on a background thread, menu items are disabled during the
    operation, and a Toast reports the outcome.

## Design for learning-mgmt

### Scope and format

- **Contents:** everything in the DB: curricula, phases, topics and progress
  (phase status, topic status, done count). Progress is always included, since
  this is a backup and not a template.
- **Selected curriculum:** include it only if it is persisted in Room. If it
  lives in DataStore or preferences, decide whether to carry it. Proposed:
  carry it, with a fallback to the first curriculum if the id is missing after
  restore.
- **Header:** `schemaVersion` (the `AppDatabase` version) and `exportedAt`.
- **Version check:** reject on mismatch, same as accounting.
  - Cost: a backup from an older app version cannot be restored after an
    update.
  - Accepted for now; accepting older versions would mean replaying migrations
    on JSON.
- **Ids:** keep the exported UUIDs (a full replace cannot collide).
- **Ordering:** keep each row's stored order field so ordering round-trips
  exactly.

### Restore semantics

1. Parse and validate the whole file first. On any error nothing changes and
   the user sees a message.
2. In one transaction, delete topics, then phases, then curricula. Then insert
   in the reverse order.
3. After success, make sure the UI lands somewhere valid: restored (or
   fallback) selection, Home and drawer re-render from Room flows. With zero
   curricula, the app returns to its empty state.

Optional safety check: validate the progress invariants before wiping:

- a completed phase has all its topics done;
- at most one in-progress phase;
- done count is within the total.

This is stricter than accounting, but protects against hand-edited files.

### UI/UX

- **Location:** the Manage curricula top-bar overflow menu, with "Back up..."
  and "Restore...". This matches the "structure edits only here" convention.
  Alternative: the drawer footer, if more discoverability is wanted.
- **Backup flow:**
  1. The system "save as" picker opens with
     `learning-mgmt-backup_2026-10-06_183000.json`.
  2. A snackbar confirms "Backup saved".
- **Restore flow:**
  1. The system file picker opens.
  2. A confirm dialog shows a file summary (e.g. "3 curricula, 14 phases,
     52 topics, exported 2026-10-06") next to the current data count. This
     lowers the risk of restoring the wrong file.
  3. The dialog says "This will erase all current data... cannot be undone".
     The confirm button is red (destructive); Cancel is neutral.
  4. A snackbar reports "Restore complete", or an error in plain language.
- **During the operation:** disable the menu items and show a small progress
  indicator.
- **Errors to message:** invalid JSON, missing schema version, schema mismatch
  ("made with a different app version, schema v3 vs v5"), malformed or
  inconsistent data, unreadable file.
- **Testing:** the confirm dialog has no text field, so Robolectric can cover
  it. The system pickers cannot be tested and stay on the manual checklist.

## Open decisions

1. **Selected curriculum:** stored in Room or elsewhere, and should it be
   restored?
2. **Schema mismatch:** is strict rejection acceptable (a backup must be
   restored on the same DB version)?
3. **Entry point:** Manage curricula overflow menu (proposed) or the drawer?
4. **Pre-restore snapshot:** save an automatic snapshot to app-private storage
   so a mistaken restore can be undone? Accounting does not have this;
   proposed: skip.
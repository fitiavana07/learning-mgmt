# Peer-to-peer sync

Several devices of one person keep the same data in step over the local Wi-Fi, with no server and
no account. Devices find each other automatically (as LocalSend does), and each one behaves as if
it synced to a server: local changes are pushed, the others' changes are pulled, and conflicts are
settled by a fixed merge rule.

The document has two parts. **Part I is the specification**: it describes the protocol and the
algorithms with no reference to any language, library or class, so it can be implemented on any
platform. **Part II maps it onto this codebase**.

---

# Part I: Specification

## 1. Goals, assumptions, non-goals

- **Main use case:** tick progress on device A, open device B, and find the progress already
  there, without redoing it.
- **One user, a few devices.** Data is small (hundreds of rows), so whole-state exchange is cheap.
- **Same local network.** No internet, no relay, no accounts.
- **Foreground only.** Sync runs while the app is on screen. Nothing runs in the background.
- **Convergence over cleverness.** Any two devices that exchange state end up identical, whatever
  the order of exchanges. Concurrent edits to the same record keep the later one; the other is lost.
- **Non-goals:** syncing between different people, merging two edits of one record field by field,
  syncing while the app is closed, protection against an attacker who knows the passphrase.

## 2. Concepts

| Term | Meaning |
|---|---|
| **Device id** | Random UUID generated on first launch, never changed, never shared between installations. |
| **Device name** | Friendly label derived from the device id (section 3). |
| **Passphrase** | Secret typed once on every device of the user. It defines the **group** and the encryption key. |
| **Group** | The devices that share a passphrase. Only they see each other. |
| **Peer** | Another device of the group, currently reachable. |
| **Record** | One synced row. Five kinds: curriculum, phase, topic, phase status, topic progress. Identified by `(kind, id)`. Progress records use their parent's id (a phase status uses the phase id, a topic progress uses the topic id). |
| **Stamp** | The version of a record: a pair `(time, deviceId)` ordered by `time`, then `deviceId`. |
| **Tombstone** | A record marked deleted. It keeps its stamp so the deletion propagates. |
| **Snapshot** | A device's whole state: all its rows, plus the stamp of every record and every tombstone. |

Device-local state is **never synced**: the selected curriculum, the passphrase, the device id and
the auto-sync switch.

## 3. Identity and naming

- The device id is generated once and persisted in device-local settings.
- The name is `Adjective Noun`, chosen deterministically from the device id:

```
hash       = 32-bit hash of the device id string
adjective  = adjectives[ hash mod 64 ]
noun       = nouns[ (hash >>> 16) mod 64 ]
```

  Two fixed lists of 64 words give 4096 names. The same id always gives the same name.
- When two visible peers have the same name, the list shows `Name · XXXX`, where `XXXX` is the
  first four characters of the device id in upper case. The suffix disappears when the namesake
  leaves.

## 4. Security

**Threat model:** a casual or curious person on the same Wi-Fi who is not in the group. The
protocol keeps them from reading or changing the data, and from sending data that is applied.

- **Minimum passphrase length: 8 characters** (after trimming).
- **Key:** `PBKDF2-HMAC-SHA256(passphrase, fixed salt, 100 000 iterations)`, 256 bits. The
  derivation is slow on purpose: derive once per passphrase and keep the result.
- **Messages:** every session message is encrypted and authenticated with AES-256-GCM, a fresh
  random 12-byte nonce each time, 128-bit tag. The wire form is `nonce || ciphertext || tag`.
  A message that fails authentication is never used.
- **Group tag:** `hex( first 8 bytes of HMAC-SHA256(key, "learning-mgmt-group") )`, 16 characters.
  It is broadcast in discovery packets so that only members of the group list each other. It does
  not reveal the key.
- **Replays are harmless:** applying a snapshot is idempotent and older stamps always lose, so a
  replayed message cannot revert state.
- **Known limits:** (1) anyone can capture the group tag and test passphrase guesses offline, which
  is why the minimum length is 8 and the derivation is slow; (2) there is no forward secrecy;
  (3) unauthenticated parties can open connections, which is bounded (section 11).

## 5. Discovery

Discovery finds who is around. It carries no data, only enough to open a connection.

- **Transport:** UDP datagrams on a fixed port (41889), sent by broadcast to the broadcast address
  of every IPv4 network the device is on, and to the generic broadcast address.
- **Packet** (JSON, at most 1024 bytes, anything else is ignored):

```
{ "app": "learning-mgmt-sync", "protocol": 1,
  "type": "announce" | "query",
  "deviceId": "...", "name": "...",
  "port": <TCP port of this device's session server, 1..65535>,
  "group": "<group tag>" }
```

- **Announce** means "I am here". It is sent when discovery starts and then every **5 seconds**.
- **Query** means "who is here?". It is sent on **refresh**. Every group member that receives a
  query answers **directly** (unicast to the sender) with an announce, without waiting for its
  next interval. A query also carries the asker's own details, so the receivers list the asker.
- **Registry:** each received valid packet of our group and protocol version, from another
  device, creates or refreshes an entry `deviceId -> (name, host, port, lastSeen)`. Our own packets,
  other groups and other protocol versions are ignored.
- **Expiry:** an entry not refreshed for **15 seconds** is removed. Expiry is checked every second.
- **Refresh** clears the registry, sends a query and lets the answers repopulate it.
- The list shown to the user is ordered by label, then device id, and only changes when its content
  does (not on every heartbeat).
- On some platforms the radio drops incoming broadcasts unless a multicast lock is held while
  discovery runs.

## 6. Data model

Every record has a **meta**: `(stamp, deleted)`. The store holds, for each record, its row (unless
deleted) and its meta.

- **Invariant:** every row has a live meta; a deleted record has a tombstone and no row. A row that
  has no meta yet (data from before sync existed) is treated as having the stamp `(0, "")`, older
  than any real write.
- Tombstones are **kept forever** (a few bytes each).
- Deleting a record also tombstones everything below it that exists (curriculum: its phases,
  topics, phase statuses and topic progress rows; phase: its topics and their progress and its own
  status; topic: its progress). This is done before the rows disappear.
- Ordered lists (curricula, phases, topics) carry an integer `number`. See section 8.

## 7. The stamp clock

Phone clocks drift, so a stamp never trusts the wall clock alone.

```
next():      highest = max( wallClock(), highest + 1 )
             return (highest, thisDeviceId)
observe(t):  highest = max( highest, t )
```

- Every local write (including a decrement of a counter) gets a fresh stamp from `next()`. Stamps
  of one device are strictly increasing, so two writes never share a stamp.
- On start, `highest` is initialised from the highest stamp time already stored.
- Applying a peer's state first calls `observe` with the highest stamp time it contains, so
  everything written afterwards is newer than what was just received.

## 8. Ordering

Lists use an integer `number` and **tolerate duplicates and gaps**, which concurrent inserts on
two devices produce. Sync never repairs numbers; that keeps the merge trivial (a number is just a
field).

- **Order** is `(number, id)`: the id breaks ties, so every device orders identically.
- **Display:** the user sees the **place** in that order (1, 2, 3...), never the stored number.
- **Append:** a new item gets `max(number) + 1` (1 if empty), so it never collides with a gap.
- **Reorder / delete:** renumber the whole list contiguously `1..N` in the current order; only the
  rows whose number changed are rewritten (and so restamped).
- Moves use the places the user sees (`from`, `to` are positions in the ordered list).

## 9. Merge

Per record, last write wins:

```
merge(local, remote):                    // maps  recordKey -> meta
  for each key in remote:
     if key not in local or remote[key].stamp > local[key].stamp:
         local[key] = remote[key]        // and take the remote row, or delete the local one if it is a tombstone
  return local, set of keys taken
```

- A tombstone competes like any other version: a newer edit beats an older deletion and vice versa.
- Equal stamps are the same write and change nothing.
- **Properties** (a conforming implementation must satisfy them): merge is **commutative,
  associative and idempotent** on metas. Therefore devices converge regardless of exchange order,
  and A -> B -> C propagates A's changes to C through B.
- Progress records merge independently of structure, so a progress tick is never blocked by, or
  lost to, an older copy of the curriculum.

## 10. Repair

Rows merged one by one can break rules that hold across rows (two devices each started a
different phase; a topic was added to a completed phase; a parent was deleted elsewhere). After
every merge a deterministic **repair** runs on the merged rows, in this order:

```
1. drop rows whose parent is gone, parents first
     (phase without curriculum, topic without phase, status without phase, progress without topic)
2. clamp every counter to 0..total          (negative -> 0; no total -> only the lower bound)
3. in each phase keep at most one topic in progress: the lowest by (number, id); others -> not started
4. a completed phase with a topic that is not completed -> in progress
5. in each curriculum keep at most one phase in progress: the lowest by (number, id); others -> not started
```

- The repair is **idempotent** and a function of the merged rows only, so devices repair alike.
- Every row it **modified or removed** is then stamped as a fresh local write (a removal becomes a
  tombstone). Without this, two devices could hold different values under the same stamp forever.
- After repair the data must satisfy all consistency rules (no duplicate ids, no dangling parent,
  counters in range, at most one phase / topic in progress, completed phases fully completed). If
  not, the whole merge is rolled back and the peer's state is rejected.

## 11. Session protocol

A **session** brings two devices to the same state in **one round trip**. Whole-state exchange
means no change log, no per-peer cursors, and nothing to recover after a failed session.

**Transport.** Each device listens on a TCP port chosen by the system and announces it in discovery.
The initiator connects (3 s connect timeout, 15 s read timeout, so a silent peer is given up on).
Messages are **frames**: a 4-byte big-endian length, then that many bytes. A frame larger than
16 MiB, or with a negative length, is refused before reading it. A listener serves at most **4
connections at once** and drops the others.

**Messages** (JSON, then encrypted as in section 4, then framed). Each carries
`"protocol": 1`, which is read before anything else; another value is reported as incompatible.

```
request  : { protocol, type:"request",  deviceId, name, snapshot }   // initiator -> responder
response : { protocol, type:"response", deviceId, name, snapshot }   // responder -> initiator
failure  : { protocol, type:"failure",  message }                    // responder -> initiator
```

**Exchange:**

```
initiator                                   responder
  snapshot_A = read local state
  send request(snapshot_A)           --->   receive, decrypt, decode
                                            check: same database/schema version, stamps consistent
                                            apply(snapshot_A)        // merge + repair, one transaction
                                            send response(snapshot_B_after_merge)   (or failure)
  receive, decrypt, decode           <---
  apply(snapshot_B_after_merge)
```

Because the responder answers with its state **after** merging, the initiator ends up with the
merge of both and normally has nothing further to change.

**Applying a snapshot** is all-or-nothing, in one transaction:

```
reject if the snapshot's schema version differs, or a row has no live stamp, or a live stamp has no row
local   = current rows + metas
merged  = merge(local metas, snapshot metas)
observe(highest stamp time in the snapshot)
rows    = local rows, replacing/deleting those whose record was taken from the snapshot
repaired = repair(rows)                            // section 10
verify repaired rows are consistent                // else roll back
store the taken metas; stamp what repair changed as new local writes
write only the rows that differ, deleting children before parents and inserting parents first
```

**Failures**, reported to the user in plain terms:

| Failure | Cause |
|---|---|
| No answer | The peer hung up without replying: usually another passphrase. |
| Wrong passphrase | A message did not authenticate. |
| Incompatible | Another protocol version. |
| Refused | The peer could not use our state (for example another database version). |
| Invalid | A message that is not valid. |
| Network | Connection failed or timed out. |

A responder that cannot decrypt a request **replies nothing and hangs up**: without the passphrase
there is no one to answer.

## 12. When syncing happens

Sync is active while the app is in the foreground and a passphrase is set. A switch,
**auto-sync, on by default**, controls the automatic triggers; the manual ones always work.

| Trigger | Behaviour |
|---|---|
| Peer appears | A newly listed peer is synced with if **this device's id is smaller** than the peer's (so exactly one of two devices starts), **or** this device has local changes the peer has not been synced up to (see below). |
| Local change | About **3 seconds after the last change** (debounced), sync with every peer currently listed. |
| Manual | Tapping a peer syncs with it; "Sync all" syncs with every listed peer. |
| Refresh | Forgets and re-discovers peers; peers that answer appear again, and the "peer appears" rule applies to them. |

- **Unsynced changes:** the device counts its local changes, and remembers for each peer the count
  at the start of the last successful session with it. If the count is higher, the peer is behind.
  This covers a change made before the peer was discovered, which the smaller-id rule alone would
  miss when this device has the larger id.
- **No loops:** applying a peer's rows is not a local change and triggers nothing. A repair is a
  local write, so it is pushed once; the peers' state then already matches and nothing more follows.
- **One session per peer at a time** from this device. Sessions with different peers, and incoming
  sessions, may overlap; each is a transaction, and merging commutes.
- **Passphrase changes** restart discovery and the listener in the new group. Without a passphrase
  nothing is opened.
- **Network cannot open** (no Wi-Fi, port in use): the problem is reported and a refresh retries.
- Leaving the foreground stops discovery and the listener and clears the peer list.

## 13. Edge cases

- **Independent first-time data:** two devices that each created a curriculum before ever syncing
  have different ids, so after syncing both curricula exist. This is safe; the user deletes one.
- **Restore from a backup:** it is a bulk local write. Every restored record gets a fresh stamp and
  every record it replaced is tombstoned, otherwise the next sync would bring the old data back.
- **Edit vs delete:** the later stamp wins. A later edit of a record deleted elsewhere brings the
  record back (without the descendants whose own tombstones are newer).
- **Concurrent progress on one topic:** the later write wins; the other value is lost. A decrement is
  a normal write, so a maximum would not be correct either.
- **A child created under a parent deleted elsewhere** is dropped by repair and tombstoned.
- **Progress for a deleted topic** is dropped likewise.
- **A device left offline for a long time** needs nothing special: tombstones are never purged.
- **Clocks:** a device with a wrong clock cannot win forever; it is out-stamped after syncing once
  (section 7).
- **Another database version** on one device: sessions are refused with the versions shown, until
  both apps are updated.

## 14. Properties a correct implementation satisfies

1. Merge of metas is commutative, associative and idempotent.
2. After a session both devices have identical records and identical metas.
3. Applying the same snapshot twice changes nothing.
4. A rejected snapshot changes nothing, not even stamps.
5. A tick of progress on A followed by a session is visible on B, and a later write on B returns to A.
6. Delete propagates with everything below it; a newer edit resurrects only what it touched.
7. Repair is idempotent and produces data that satisfies all consistency rules.
8. Stamps of one device are strictly increasing, and every stamp written after an apply is greater
   than every stamp that was received.
9. Receiving rows never triggers an automatic sync by itself; repairs do, once.

---

# Part II: Implementation in this codebase

Package `dev.fitiavana.learning_mgmt`. Sync lives in `features/sync/`; its UI in
`ui/managecurricula/sync/`. Section numbers refer to Part I.

## Where each part lives

| Spec | Code |
|---|---|
| 2 Stamp, tombstone, record | `Stamp`, `RecordKey`, `Meta`, `SyncKind` (`Stamp.kt`, `SyncMerge.kt`) |
| 3 Names | `DeviceName` (`of`, `shortId`); labels in `PeerRegistry` |
| 4 Security | `SyncCrypto` (key, `encrypt`, `decrypt`, `groupTag`) |
| 5 Discovery | `DiscoveryPacket`, `PeerRegistry`, `Discovery`, `DiscoveryChannel`, `UdpDiscoveryChannel`, `LockedDiscoveryChannel` |
| 6 Data model | `SyncMetaRow` / `SyncMetaDao` (table `sync_meta`), `ChangeTracker` |
| 7 Clock | `StampClock` |
| 8 Ordering | `NumberedOrdering` (ties by id, `nextNumber` = max + 1); reads ranked in the repositories |
| 9 Merge | `SyncMerge.merge` (pure) |
| 10 Repair | `SyncRepair.repair` (pure), checked with `BackupRules` |
| 11 Session | `SyncSession`, `SyncMessageJson` / `SyncMessage`, `FrameIO`, `SyncServer`, `SyncClient`, `SyncRepository.apply` / `snapshot`, `SyncSnapshot`, `SyncJson` |
| 12 Triggers | `SyncCoordinator` (implements `SyncControls`), `ForegroundObserver` |
| UI | `SyncScreen`, `SyncRoute`, `SyncViewModel` |

## Storage

- **Schema version 3** (`DB_VERSION`). `MIGRATION_2_3` (in `db/Migrations.kt`) adds
  `curriculum.number`, seeded from the previous insertion order (`rowid`), creates `sync_meta`
  (`kind`, `id`, `stampTime`, `stampDevice`, `deleted`, primary key `(kind, id)`), and gives every
  existing row a zero stamp. The schema is exported to `app/schemas/.../3.json`, and
  `MigrationTest` covers `2 -> 3`. Backups from version 2 are refused by the existing strict
  version check.
- **Feature tables are unchanged** apart from `curriculum.number`. Deleted rows are hard-deleted;
  only `sync_meta` remembers them as tombstones.
- Every DAO that lists curricula, phases or topics orders by `number, id`.
- **Device-local settings** (`SyncSettingsStore`, a preferences file named `sync`): device id,
  passphrase, auto-sync (default true). `AppContainer` reads the device id once at start, because
  the first write needs it. The file is excluded from cloud backup and device transfer in
  `res/xml/data_extraction_rules.xml` and `backup_rules.xml`; a restored id would be shared by two
  phones and the passphrase would leave the device.

## Write path

`ChangeTracker` (`touch`, `deleted`, `deletedCurriculum` / `deletedPhase` / `deletedTopic`,
`replaceAll`, `observe`, and the `changes` flow) is the only place that stamps writes. Each
repository calls it **inside its own transaction**: `CurriculumRepository`, `PhaseRepository`,
`TopicRepository` (create, update, delete, move; only rows whose number changed are touched),
`ProgressRepository` (phase status and topic progress) and `BackupRepository.restore`
(`replaceAll`). The delete helpers read the descendants **before** the row is deleted. The clock is
seeded lazily from the highest stored stamp on the first write.

Reads are numbered by place: `PhaseRepository.observe`, `TopicRepository.observe` and the
`ProgressRepository` observers renumber with `PhaseOrdering` / `TopicOrdering`, so no screen shows
a stored number. `ProgressRepository` rule checks use the same places.

## Apply path

`SyncRepository.apply` runs in one `withTransaction`: `read` (rows through `BackupDao`, metas through
`SyncMetaDao`, unstamped rows given the zero stamp), `SyncMerge.merge`, `SyncRepair.repair`,
`BackupRules.violation`, then writes through `SyncDao` (`@Upsert`, so a row is updated in place and
no foreign key cascade fires; deletes are chunked by 500 because older SQLite limits variables).
It bypasses `ProgressRepository`'s start-in-order checks, as `BackupRepository.restore` does.
Repair results are stamped through `ChangeTracker`, which announces them on `changes`; rows taken
from the peer are written to `sync_meta` directly and announce nothing.

The payload is the backup format plus stamps: `SyncJson` nests `BackupJson` output and adds
`meta`. The strict decoding helpers are shared in `features/StrictJson.kt`. `SyncSnapshot.violation`
checks stamp/row consistency only; cross-row rules are left to the repair, because a device can
legitimately hold such a state (a topic added to a completed phase).

## Network

- `SyncServer` listens on port 0 and hands each connection to a handler; at most
  `SyncServer.MAX_CONNECTIONS` (4) are served at once. `SyncClient.connect` has the 3 s / 15 s
  timeouts. `FrameIO` limits frames to 16 MiB.
- `UdpDiscoveryChannel` binds `DISCOVERY_PORT` with address reuse and broadcast on. `Discovery`
  announces every 5 s and expires every 1 s. `PeerRegistry` has a 15 s TTL.
- **No new library**: plain sockets, `javax.crypto`, and `org.json` (already used by backup).

## Coordinator

`SyncCoordinator.start()` / `stop()` follow the app's foreground (`ForegroundObserver` in
`MainActivity`). It collects the passphrase and, per value, runs a network (crypto, registry,
session, server, discovery) that a new passphrase or a refresh after a failure replaces. A
`localVersion` counter and a per-peer `syncedVersion` implement "unsynced changes". Its
`SyncState` (running, hasPassphrase, autoSync, deviceName, peers with a status, lastSyncedAt,
networkError) is what the screen shows. Status per peer: idle, syncing, synced (time, changed),
failed (reason).

## Android integration

- **Manifest permissions:** `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and
  `ACCESS_LOCAL_NETWORK`. The project targets SDK 37, where local network access is blocked by
  default and needs the runtime permission; `SyncRoute` asks for it on the sync screen (and again
  after the user returns from settings), and retries the network once granted. Below API 37 it is
  implicit.
- **Multicast lock:** `AppContainer` creates one and `LockedDiscoveryChannel` holds it for the
  lifetime of the channel. Without it Android drops incoming broadcasts.
- **Entry point:** overflow menu of *Manage curricula* -> "Sync with other devices"
  (`Routes.SYNC` in `AppNavHost`).

## UI

`SyncScreen` is stateless: this device's name, the local-network prompt (when blocked), the
passphrase form or "Passphrase is set" with *Change* and a red, confirmed *Remove passphrase*,
the auto-sync switch, and the devices list with per-peer status, *Sync all*, a refresh action, a
network-error block or an empty state. The passphrase is typed in the screen, not a dialog.
Failures are explained in plain text next to the peer.

## Tests

All run on the JVM (Robolectric where Android is needed); none needs a device.

- Pure: `SyncMergeTest`, `StampClockTest`, `DeviceNameTest`, `SyncRepairTest`, `SyncSnapshotTest`,
  `PeerRegistryTest`, `FrameIOTest`, `SyncCryptoTest`.
- Persistence: `MigrationTest` (`2 -> 3`), `SyncMetaDaoTest`, `NumberTiesTest`, `StampedWritesTest`,
  `ChangeTrackerTest`, backup tests for restore stamping.
- Merge: `SyncRepositoryTest` runs two in-memory databases through snapshots (adds, edits, deletes,
  reorders, progress ticks and decrements, concurrent edits, repairs, rejection, clock, no
  announce on receive).
- Protocol: `SyncMessageJsonTest`, `SyncSessionTest` (real loopback sockets, wrong passphrase,
  other version, garbage, oversize, silence), `SyncTransportTest` (including the connection cap),
  `DiscoveryPacketTest`, `DiscoveryTest` (virtual time), `UdpDiscoveryChannelTest`.
- Coordinator: `SyncCoordinatorTest` runs two devices with a fake network and real loopback sessions,
  including the "change made before the peer was seen" case.
- UI: `SyncViewModelTest`, `SyncScreenTest`, `ManageCurriculaScreenTest`, `AppNavHostTest`.
- Platform glue: `ManifestTest` (permissions, backup exclusions), `LockedDiscoveryChannelTest`,
  `ForegroundObserverTest`.

## Manual checks on real devices

Not coverable on the JVM: broadcast delivery on real Wi-Fi, the multicast lock, the Android 17
local-network permission flow, and behaviour with two phones.

1. Install on two phones on the same Wi-Fi; set the same passphrase; both list each other with a name.
2. Refresh clears and re-lists the peers.
3. Tick progress on one phone, open the other: it is already there.
4. Use "-" on one device, then continue on the other.
5. Create, rename, reorder and delete curricula, phases and topics on each; they follow.
6. Edit the same item on both while apart, then reconnect: the later edit wins on both.
7. Wrong passphrase on one: the devices do not see each other.
8. Auto-sync off: nothing moves until a peer is tapped or "Sync all" is used.
9. Airplane mode or no Wi-Fi: the screen reports the problem; refresh recovers.
10. Android 17: the first visit to the sync screen asks for local network access; denying it shows the prompt again.
11. Restore a backup on one phone, then sync: the other follows the restored data.
12. Leave the app: the phone stops being listed on the other.

## Known limitations and open decisions

- A concurrent edit to the same record loses one side (accepted, section 13).
- No encryption of the data on the device; the passphrase is stored in app-private storage.
- Backup files stay version-bound: a v2 backup cannot be restored into v3.
- The discovery port and the word lists are constants; a clash on the port shows as a network error.
- Tombstones grow without bound. Purging them would need a rule for devices offline for a long time.
- Not implemented: syncing in the background, a "sync only structure" mode, per-field merging.

package dev.fitiavana.learning_mgmt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.StampClock

/** In-memory database for Robolectric tests. */
fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** A tracker stamping as device [deviceId] with the given wall clock. */
fun testTracker(db: AppDatabase, deviceId: String = "test", now: () -> Long = { 1_000L }): ChangeTracker =
    ChangeTracker(db.syncMetaDao(), StampClock(deviceId, now))

/** Predictable ids: "<prefix>1", "<prefix>2", ... */
fun sequentialIds(prefix: String = "id"): IdGenerator {
    var next = 0
    return { "$prefix${++next}" }
}

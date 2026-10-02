package dev.fitiavana.learning_mgmt.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** In-memory database for Robolectric tests. */
fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Predictable ids: "<prefix>1", "<prefix>2", ... */
fun sequentialIds(prefix: String = "id"): IdGenerator {
    var next = 0
    return { "$prefix${++next}" }
}

package dev.fitiavana.learning_mgmt.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs on the JVM with Robolectric against the schemas exported to `app/schemas`. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrating1To2KeepsExistingDataAndAddsTopicTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO curriculum (id, name) VALUES ('c1', 'Spanish')")
            execSQL("INSERT INTO phase (id, curriculumId, number, name, description) VALUES ('p1', 'c1', 1, 'Basics', 'md')")
            execSQL("INSERT INTO phase_status (phaseId, status) VALUES ('p1', 'IN_PROGRESS')")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT name, description FROM phase WHERE id = 'p1'").use {
            it.moveToFirst()
            assertEquals("Basics", it.getString(0))
            assertEquals("md", it.getString(1))
        }
        db.query("SELECT status FROM phase_status WHERE phaseId = 'p1'").use {
            it.moveToFirst()
            assertEquals("IN_PROGRESS", it.getString(0))
        }
        db.execSQL("INSERT INTO topic (id, phaseId, number, name, total, unit) VALUES ('t1', 'p1', 1, 'Chapter', 40, 'pages')")
        db.execSQL("INSERT INTO topic_progress (topicId, status, done) VALUES ('t1', 'IN_PROGRESS', 12)")
        db.query("SELECT done FROM topic_progress WHERE topicId = 't1'").use {
            it.moveToFirst()
            assertEquals(12, it.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}

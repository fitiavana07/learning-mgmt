package dev.fitiavana.learning_mgmt.features.curricula

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import dev.fitiavana.learning_mgmt.features.sync.ChangeTracker
import dev.fitiavana.learning_mgmt.features.sync.SyncKind
import kotlinx.coroutines.flow.Flow

class CurriculumRepository(
    private val db: RoomDatabase,
    private val dao: CurriculumDao,
    private val tracker: ChangeTracker,
    private val newId: IdGenerator = UuidGenerator,
) {
    fun observeAll(): Flow<List<Curriculum>> = dao.observeAll()

    suspend fun create(name: String): String =
        db.withTransaction {
            val id = newId()
            dao.insert(Curriculum(id, validName(name), dao.highestNumber() + 1))
            tracker.touch(SyncKind.CURRICULUM, id)
            id
        }

    suspend fun rename(id: String, name: String) {
        val valid = validName(name)
        db.withTransaction {
            if (dao.rename(id, valid) > 0) tracker.touch(SyncKind.CURRICULUM, id)
        }
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            tracker.deletedCurriculum(id)
            dao.delete(id)
        }
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Curriculum name must not be blank" } }
}

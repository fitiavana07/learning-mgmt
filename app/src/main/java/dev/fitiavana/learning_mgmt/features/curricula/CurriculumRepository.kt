package dev.fitiavana.learning_mgmt.features.curricula

import dev.fitiavana.learning_mgmt.db.IdGenerator
import dev.fitiavana.learning_mgmt.db.UuidGenerator
import kotlinx.coroutines.flow.Flow

class CurriculumRepository(
    private val dao: CurriculumDao,
    private val newId: IdGenerator = UuidGenerator,
) {
    fun observeAll(): Flow<List<Curriculum>> = dao.observeAll()

    suspend fun create(name: String): String {
        val id = newId()
        dao.insert(Curriculum(id, validName(name), dao.highestNumber() + 1))
        return id
    }

    suspend fun rename(id: String, name: String) = dao.rename(id, validName(name))

    suspend fun delete(id: String) = dao.delete(id)

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Curriculum name must not be blank" } }
}

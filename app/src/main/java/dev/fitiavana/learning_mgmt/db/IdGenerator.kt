package dev.fitiavana.learning_mgmt.db

import java.util.UUID

/** Every primary key is a UUID string generated in app code, never by the database. */
typealias IdGenerator = () -> String

val UuidGenerator: IdGenerator = { UUID.randomUUID().toString() }

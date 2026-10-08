package dev.fitiavana.learning_mgmt.features.sync

import dev.fitiavana.learning_mgmt.features.FormatException
import dev.fitiavana.learning_mgmt.features.int
import dev.fitiavana.learning_mgmt.features.string
import org.json.JSONException
import org.json.JSONObject

/**
 * What devices broadcast on the local network to find each other: who they are, where to connect
 * and which group (a hash of the passphrase, never the passphrase) they belong to. It is not
 * encrypted and carries no data, only enough to open a connection, which is.
 */
data class DiscoveryPacket(
    val type: Type,
    val info: PeerInfo,
    val port: Int,
    val group: String,
) {
    enum class Type {
        /** "I am here": sent regularly and in answer to a query. */
        ANNOUNCE,

        /** "Who is here?": makes everyone answer with an announcement right away. */
        QUERY,
    }

    fun encode(): ByteArray = JSONObject()
        .put("app", APP)
        .put("protocol", PROTOCOL_VERSION)
        .put("type", type.name.lowercase())
        .put("deviceId", info.deviceId)
        .put("name", info.name)
        .put("port", port)
        .put("group", group)
        .toString()
        .toByteArray()

    companion object {
        private const val APP = "learning-mgmt-sync"
        private const val MAX_SIZE = 1024

        /** The packet, or null for anything that is not a valid packet of this app and protocol version. */
        fun decode(bytes: ByteArray): DiscoveryPacket? {
            if (bytes.isEmpty() || bytes.size > MAX_SIZE) return null
            return try {
                val root = JSONObject(String(bytes))
                if (root.string("app") != APP || root.int("protocol") != PROTOCOL_VERSION) return null
                val typeName = root.string("type")
                val type = Type.entries.firstOrNull { it.name.lowercase() == typeName } ?: return null
                val port = root.int("port").takeIf { it in 1..65_535 } ?: return null
                DiscoveryPacket(type, PeerInfo(root.string("deviceId"), root.string("name")), port, root.string("group"))
            } catch (e: FormatException) {
                null
            } catch (e: JSONException) {
                null
            }
        }
    }
}

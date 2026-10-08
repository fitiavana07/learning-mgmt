package dev.fitiavana.learning_mgmt

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    /** Predictive back: the back gesture drags the slide transition and previews the previous screen. */
    @Test
    fun predictiveBackIsEnabled() {
        val application = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element

        assertEquals("true", application.getAttributeNS(android, "enableOnBackInvokedCallback"))
    }

    private fun excludedFiles(path: String, section: String? = null): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val scope = if (section == null) document.documentElement
        else document.getElementsByTagName(section).item(0) as org.w3c.dom.Element
        val excludes = scope.getElementsByTagName("exclude")
        return (0 until excludes.length).map { excludes.item(it) as org.w3c.dom.Element }
            .filter { it.getAttribute("domain") == "file" }
            .map { it.getAttribute("path") }
            .toSet()
    }

    /**
     * The sync settings hold the passphrase and this installation's id: neither may leave the
     * device in a cloud backup or a device transfer (a restored id would be shared by two phones).
     */
    @Test
    fun theSyncSettingsAreNeverBackedUpOrTransferred() {
        val settings = "datastore/sync.preferences_pb"

        assertEquals(setOf(settings), excludedFiles("src/main/res/xml/backup_rules.xml"))
        assertEquals(setOf(settings), excludedFiles("src/main/res/xml/data_extraction_rules.xml", "cloud-backup"))
        assertEquals(setOf(settings), excludedFiles("src/main/res/xml/data_extraction_rules.xml", "device-transfer"))
    }

    /** Sync talks to the local network and nothing else: no location, no storage, no background access. */
    @Test
    fun theOnlyPermissionsAreTheOnesLocalNetworkSyncNeeds() {
        val permissions = manifest.getElementsByTagName("uses-permission").let { nodes ->
            (0 until nodes.length).map { (nodes.item(it) as org.w3c.dom.Element).getAttributeNS(android, "name") }
        }

        assertEquals(
            setOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_WIFI_STATE",
                "android.permission.CHANGE_WIFI_MULTICAST_STATE",
                // Android 17 blocks traffic to the local network unless the user allows it at runtime.
                "android.permission.ACCESS_LOCAL_NETWORK",
            ),
            permissions.toSet(),
        )
    }
}

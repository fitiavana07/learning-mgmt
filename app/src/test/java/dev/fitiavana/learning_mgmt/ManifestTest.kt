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
            ),
            permissions.toSet(),
        )
    }
}

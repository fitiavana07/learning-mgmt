package dev.fitiavana.learning_mgmt

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    /** Predictive back: the back gesture drags the slide transition and previews the previous screen. */
    @Test
    fun predictiveBackIsEnabled() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element

        assertEquals("true", application.getAttributeNS(android, "enableOnBackInvokedCallback"))
    }
}

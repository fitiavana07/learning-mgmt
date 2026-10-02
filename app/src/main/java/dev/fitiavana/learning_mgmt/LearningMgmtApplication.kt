package dev.fitiavana.learning_mgmt

import android.app.Application

class LearningMgmtApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

package dev.fitiavana.learning_mgmt.features.selection

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** A real DataStore-backed store writing to a throwaway file. */
fun testSelectionStore(folder: File, scope: CoroutineScope): SelectedCurriculumStore =
    SelectedCurriculumStore(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder, "selection.preferences_pb") }),
    )

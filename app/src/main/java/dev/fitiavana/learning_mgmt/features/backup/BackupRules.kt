package dev.fitiavana.learning_mgmt.features.backup

import dev.fitiavana.learning_mgmt.features.progress.Status

/**
 * Consistency checks run on a decoded backup before anything is wiped, so a hand-edited file
 * cannot leave the app in a state its own rules would never produce. Mirrors `PhaseRules` and
 * `TopicRules`: one in-progress phase per curriculum, one in-progress topic per phase, a phase
 * completed only with all its topics completed, and quantified progress within its total.
 */
object BackupRules {
    /** The first problem found, in plain language, or null when the data is consistent. */
    fun violation(data: BackupData): String? {
        val curriculumIds = data.curricula.map { it.id }
        val phaseIds = data.phases.map { it.id }
        val topicIds = data.topics.map { it.id }
        val topicsById = data.topics.associateBy { it.id }

        duplicate("curricula", curriculumIds)?.let { return it }
        duplicate("phases", phaseIds)?.let { return it }
        duplicate("topics", topicIds)?.let { return it }
        duplicate("phase statuses", data.phaseStatuses.map { it.phaseId })?.let { return it }
        duplicate("topic progress rows", data.topicProgress.map { it.topicId })?.let { return it }

        data.phases.firstOrNull { it.curriculumId !in curriculumIds }
            ?.let { return "Phase '${it.name}' belongs to a curriculum that is not in the file" }
        data.topics.firstOrNull { it.phaseId !in phaseIds }
            ?.let { return "Topic '${it.name}' belongs to a phase that is not in the file" }
        data.phaseStatuses.firstOrNull { it.phaseId !in phaseIds }
            ?.let { return "A phase status refers to a phase that is not in the file" }
        data.topicProgress.firstOrNull { it.topicId !in topicIds }
            ?.let { return "A topic progress row refers to a topic that is not in the file" }

        val phaseStatus = data.phaseStatuses.associate { it.phaseId to it.status }
        val topicProgress = data.topicProgress.associateBy { it.topicId }

        data.topicProgress.forEach { progress ->
            val topic = topicsById.getValue(progress.topicId)
            val max = topic.total
            if (progress.done < 0 || (max != null && progress.done > max)) {
                return "Topic '${topic.name}' has progress ${progress.done} outside 0 to ${max ?: "its total"}"
            }
        }

        data.curricula.forEach { curriculum ->
            val phases = data.phases.filter { it.curriculumId == curriculum.id }
            if (phases.count { phaseStatus[it.id] == Status.IN_PROGRESS } > 1) {
                return "Curriculum '${curriculum.name}' has more than one phase in progress"
            }
        }

        data.phases.forEach { phase ->
            val topics = data.topics.filter { it.phaseId == phase.id }
            val statuses = topics.map { topicProgress[it.id]?.status ?: Status.NOT_STARTED }
            if (statuses.count { it == Status.IN_PROGRESS } > 1) {
                return "Phase '${phase.name}' has more than one topic in progress"
            }
            if (phaseStatus[phase.id] == Status.COMPLETED && statuses.any { it != Status.COMPLETED }) {
                return "Phase '${phase.name}' is completed but has topics that are not"
            }
        }
        return null
    }

    private fun duplicate(what: String, ids: List<String>): String? =
        if (ids.size != ids.toSet().size) "The file lists the same id twice in $what" else null
}

package com.example.model.developer

import java.util.UUID

data class DeveloperProfile(
    val name: String,
    val role: String,
    val contactUrl: String? = null
)

data class DeveloperStatement(
    val whyCreatedTh: String,
    val problemToSolveTh: String,
    val corePrinciplesTh: String,
    val futureVisionTh: String
)

data class DeveloperFeelings(
    val feelingAtStartTh: String,
    val challengesTh: String,
    val mistakesTh: String,
    val lessonsLearnedTh: String,
    val proudestMomentsTh: String,
    val unfinishedThingsTh: String,
    val hopesTh: String
)

data class DeveloperJournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val version: String,
    val eventTh: String,
    val currentlyDoingTh: String,
    val problemsFoundTh: String,
    val lessonsTh: String,
    val developerFeelingsTh: String,
    val isPinned: Boolean = false
)

data class TechnicalMilestone(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // e.g., Architecture, ELM327
    val titleTh: String,
    val descriptionTh: String,
    val achievedVersion: String,
    val date: String
)

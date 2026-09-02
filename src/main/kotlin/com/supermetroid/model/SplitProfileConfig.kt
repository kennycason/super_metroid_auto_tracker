package com.supermetroid.model

import kotlinx.serialization.Serializable

/** Persisted, editable representation of a custom split profile. */
@Serializable
data class StoredSplitProfile(
    val id: String,
    val familyId: String,
    val version: Int,
    val name: String,
    val splitIds: List<String>,
    /** Full trigger snapshot so archives do not depend on a future app catalog. */
    val splitDefinitions: List<Split> = emptyList(),
    val splitNameOverrides: Map<String, String> = emptyMap(),
    val updatedAtEpochMs: Long = 0L
)

/**
 * User-owned split profile configuration.
 *
 * Built-in profile structure remains source controlled; only its display-name
 * overrides live here. Previous custom revisions and deleted profiles remain in
 * [archivedProfiles] so historical runs can still resolve their profile IDs.
 */
@Serializable
data class SplitProfilesConfig(
    val schemaVersion: Int = 1,
    val customProfiles: List<StoredSplitProfile> = emptyList(),
    val archivedProfiles: List<StoredSplitProfile> = emptyList(),
    val builtInSplitNameOverrides: Map<String, Map<String, String>> = emptyMap()
)

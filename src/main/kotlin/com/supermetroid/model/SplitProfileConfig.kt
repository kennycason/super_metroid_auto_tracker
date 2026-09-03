package com.supermetroid.model

import kotlinx.serialization.Serializable

/** Metadata for an original upload and its display-optimized PNG preview. */
@Serializable
data class SplitImageAsset(
    /** Path relative to ~/.smtracker (or the configured data directory). */
    val originalPath: String,
    /** Path relative to ~/.smtracker (or the configured data directory). */
    val previewPath: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    /** Serves as a stable UI cache key for this immutable asset revision. */
    val updatedAtEpochMs: Long
)

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
    val splitImageOverrides: Map<String, SplitImageAsset> = emptyMap(),
    val updatedAtEpochMs: Long = 0L
)

/**
 * User-owned split profile configuration.
 *
 * Built-in profile structure remains source controlled; its display-name and
 * image overrides live here. Previous custom revisions and deleted profiles
 * remain in [archivedProfiles] so historical runs can still resolve them.
 */
@Serializable
data class SplitProfilesConfig(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val customProfiles: List<StoredSplitProfile> = emptyList(),
    val archivedProfiles: List<StoredSplitProfile> = emptyList(),
    val builtInSplitNameOverrides: Map<String, Map<String, String>> = emptyMap(),
    val builtInSplitImageOverrides: Map<String, Map<String, SplitImageAsset>> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

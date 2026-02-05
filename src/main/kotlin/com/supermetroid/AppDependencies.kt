package com.supermetroid

import com.supermetroid.autosplits.AutoSplitsEngine
import com.supermetroid.service.*
import com.supermetroid.storage.FileStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

/**
 * Central container for all application dependencies.
 * 
 * This class provides a single point of initialization for all services,
 * enabling easier testing and cleaner dependency management.
 * 
 * Usage:
 * - In production: Create via AppDependencies.create()
 * - In tests: Create with mock services via constructor
 */
data class AppDependencies(
    val fileStorageService: FileStorageService,
    val gameStateService: GameStateService,
    val autoSplitsEngine: AutoSplitsEngine,
    val themeService: ThemeService,
    val iconSizeService: IconSizeService,
    val splitIconSizeService: SplitIconSizeService,
    val splitDisplayModeService: SplitDisplayModeService,
    val splitProfileService: SplitProfileService,
    val iconConfigService: IconConfigService,
    val roomNameService: RoomNameService,
    val iconViewModeService: IconViewModeService,
    val uiVisibilityService: UIVisibilityService,
    val soundService: SoundService,
    val gameGenieService: GameGenieService,
    val mapRandoInfoFontSizeService: MapRandoInfoFontSizeService,
    val mapRandoDataService: MapRandoDataService,
    val mapRandoInfoConfigService: MapRandoInfoConfigService
) {
    companion object {
        /**
         * Creates the default production dependencies.
         * 
         * @param customDataDir Optional custom data directory path
         * @param scope CoroutineScope for async operations (defaults to GlobalScope)
         */
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        fun create(
            customDataDir: String? = null,
            scope: CoroutineScope = GlobalScope
        ): AppDependencies {
            val fileStorage = FileStorageService(customDataDir)
            val autoSplitsEngine = AutoSplitsEngine(fileStorage)
            
            return AppDependencies(
                fileStorageService = fileStorage,
                gameStateService = GameStateService(),
                autoSplitsEngine = autoSplitsEngine,
                themeService = ThemeService(fileStorage),
                iconSizeService = IconSizeService(fileStorage),
                splitIconSizeService = SplitIconSizeService(fileStorage),
                splitDisplayModeService = SplitDisplayModeService(fileStorage),
                splitProfileService = SplitProfileService(fileStorage, autoSplitsEngine),
                iconConfigService = IconConfigService(fileStorage),
                roomNameService = RoomNameService(fileStorage),
                iconViewModeService = IconViewModeService(fileStorage),
                uiVisibilityService = UIVisibilityService(fileStorage),
                soundService = SoundService(fileStorage),
                gameGenieService = GameGenieService(fileStorage),
                mapRandoInfoFontSizeService = MapRandoInfoFontSizeService(fileStorage, scope),
                mapRandoDataService = MapRandoDataService(),
                mapRandoInfoConfigService = MapRandoInfoConfigService()
            )
        }
    }
}


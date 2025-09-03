import kotlinx.coroutines.runBlocking
import com.supermetroid.storage.FileStorageService
import com.supermetroid.service.ThemeService
import com.supermetroid.service.AppTheme

fun main() = runBlocking {
    println("🧪 Testing theme persistence...")
    
    val fileStorage = FileStorageService()
    val themeService = ThemeService(fileStorage)
    
    // Initialize theme service
    themeService.initialize()
    val initialTheme = themeService.currentTheme.value
    println("📋 Initial theme: ${initialTheme.displayName}")
    
    // Change to a different theme
    val newTheme = AppTheme.NEON_BLUE
    themeService.setTheme(newTheme)
    println("🎨 Changed theme to: ${newTheme.displayName}")
    
    // Wait a moment for async save to complete
    kotlinx.coroutines.delay(100)
    
    // Create a new theme service to test loading
    val themeService2 = ThemeService(fileStorage)
    themeService2.initialize()
    val loadedTheme = themeService2.currentTheme.value
    println("📋 Loaded theme: ${loadedTheme.displayName}")
    
    if (loadedTheme == newTheme) {
        println("✅ Theme persistence test PASSED!")
    } else {
        println("❌ Theme persistence test FAILED!")
        println("   Expected: ${newTheme.displayName}")
        println("   Got: ${loadedTheme.displayName}")
    }
}

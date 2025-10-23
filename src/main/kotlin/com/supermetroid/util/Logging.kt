package com.supermetroid.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Lightweight logging mixin for kotlin-logging/logback.
 *
 * Usage in a class:
 *   class MyService : Logging {
 *     fun doWork() {
 *       logger.info { "hello" }
 *     }
 *   }
 *
 * For top-level files with only functions/composables, prefer a small private object:
 *   private object SpriteIconLog : Logging
 *   // then SpriteIconLog.logger.debug { "..." }
 *
 * Note: This mirrors the ergonomics of log4j-kotlin's Logging interface. The property getter
 * uses KotlinLogging.logger {} which internally caches by class name, so the overhead is minimal.
 */
interface Logging {
    val logger: KLogger
        get() = KotlinLogging.logger {}
}

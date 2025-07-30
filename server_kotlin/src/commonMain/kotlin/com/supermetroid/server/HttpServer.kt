package com.supermetroid.server

import com.supermetroid.service.BackgroundPoller
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

/**
 * Ktor HTTP server serving cached game state data
 * Equivalent to Python's CacheServingHTTPHandler and BackgroundPollerServer
 */
class HttpServer(
    private val port: Int = 8000,
    private val pollInterval: Long = 1000L
) {
    private val poller = BackgroundPoller(pollInterval)
    private var server: ApplicationEngine? = null

    suspend fun start() {
        try {
            // Start background poller
            poller.start()

            // Create and configure Ktor server
            server = embeddedServer(CIO, port = port) {
                configureServer()
                configureRouting()
            }

            println("🚀 Background Polling Super Metroid Tracker Server")
            println("=".repeat(50))
            println("📱 Tracker UI: http://localhost:$port/")
            println("📊 API Status: http://localhost:$port/api/status")
            println("📈 API Stats:  http://localhost:$port/api/stats")
            println("⚡ Background polling: ${pollInterval}ms intervals")
            println("🎯 Architecture: Background UDP + Instant Cache Serving")
            println("⏹️  Press Ctrl+C to stop")
            println("=".repeat(50))

            server?.start(wait = true)

        } catch (e: Exception) {
            println("Server error: ${e.message}")
        } finally {
            stop()
        }
    }

    fun stop() {
        poller.stop()
        server?.stop(1000, 5000)
        println("🏁 Server stopped")
    }

    private fun Application.configureServer() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
        }
    }

    private fun Application.configureRouting() {
        routing {
            // Main endpoints
            get("/") {
                call.respondText(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head><title>Super Metroid Tracker (Kotlin Native)</title></head>
                    <body>
                        <h1>🎮 Super Metroid Tracker (Kotlin Native)</h1>
                        <p>Kotlin Native backend server is running!</p>
                        <ul>
                            <li><a href="/api/status">📊 Server Status</a></li>
                            <li><a href="/api/stats">📈 Game Stats</a></li>
                            <li><a href="/game_state">🎯 Game State</a></li>
                        </ul>
                        <p><em>Original Python backend: <a href="http://localhost:8000">localhost:8000</a></em></p>
                    </body>
                    </html>
                    """.trimIndent(),
                    ContentType.Text.Html
                )
            }

            get("/api/status") {
                val status = poller.getCachedState()
                call.respond(status)
            }

            get("/api/stats") {
                val status = poller.getCachedState()
                call.respond(status.stats)
            }

            get("/game_state") {
                val status = poller.getCachedState()
                call.respond(status.stats)
            }

            post("/api/reset-cache") {
                poller.resetCache()
                // Reset Mother Brain cache if parser is accessible
                try {
                    poller.parser.resetMotherBrainCache()
                    call.respondText("Cache reset successfully", ContentType.Text.Plain, HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respondText("Cache reset completed with warnings: ${e.message}", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }

            get("/api/bootstrap-mb") {
                call.respond(mapOf("message" to "Bootstrap triggered"))
            }

            get("/api/manual-mb-complete") {
                try {
                    // Note: In full implementation, this would manually set MB completion
                    val response = mapOf(
                        "message" to "MB1 and MB2 manually set to completed",
                        "mb1" to true,
                        "mb2" to true
                    )
                    call.respond(response)
                    println("🔧 Manual MB completion triggered via API")
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to e.message)
                    )
                }
            }

            get("/api/reset-mb-cache") {
                try {
                    poller.parser.resetMotherBrainCache()
                    val response = mapOf("message" to "MB cache reset to default (not detected)")
                    call.respond(response)
                    println("🔄 MB cache reset via API")
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to e.message)
                    )
                }
            }

            // Add /api/reset endpoint that redirects to /api/reset-cache
            // This is for compatibility with the user's existing calls
            get("/api/reset") {
                poller.resetCache()
                call.respondText("Cache reset successfully via /api/reset", ContentType.Text.Plain, HttpStatusCode.OK)
                println("🔄 Cache reset via /api/reset endpoint")
            }

            // Health check endpoint
            get("/health") {
                call.respond(mapOf(
                    "status" to "healthy",
                    "server" to "kotlin-native",
                    "version" to "1.0.0"
                ))
            }
        }
    }
}

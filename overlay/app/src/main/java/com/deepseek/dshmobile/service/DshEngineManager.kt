package com.deepseek.dshmobile.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DshEngineManager(private val context: Context) {

    companion object {
        private const val TAG = "DshEngineManager"
        private const val PORT = 3080
        private const val HOST = "127.0.0.1"

        @Volatile
        var isRunning: Boolean = false

        @Volatile
        private var shared: DshEngineManager? = null

        /** Process-wide singleton bound to the application context. */
        fun get(context: Context): DshEngineManager {
            shared?.let { return it }
            return synchronized(this) {
                shared ?: DshEngineManager(context.applicationContext).also { shared = it }
            }
        }

        fun baseUrl(): String = "http://$HOST:$PORT"
    }

    private var process: Process? = null

    /**
     * 初始化并启动本地 dsh 引擎。
     * 从 assets/engine/ 解压 node + dsh CLI 到 filesDir，然后启动 `dsh web`。
     */
    suspend fun initialize(): Boolean {
        if (isRunning) return true

        val appDir = File(context.filesDir, "dsh_engine")
        if (!appDir.exists()) appDir.mkdirs()

        val nodeBin = File(appDir, "node")
        val dshCli = File(appDir, "dsh")

        if (!nodeBin.exists() || !dshCli.exists()) {
            Log.i(TAG, "Engine binaries missing, extracting from assets...")
            if (!extractFromAssets(appDir)) {
                Log.e(TAG, "Failed to extract engine binaries")
                return false
            }
        }
        nodeBin.setExecutable(true, false)
        dshCli.setExecutable(true, false)

        return try {
            val pb = ProcessBuilder(
                nodeBin.absolutePath,
                dshCli.absolutePath,
                "web",
                "--port", PORT.toString(),
                "--host", HOST
            )
                .redirectErrorStream(true)
                .directory(appDir)
                .start()

            process = pb
            Log.i(TAG, "Engine process started: PID=${pb.pid()}")

            withContext(Dispatchers.IO) { waitForReady() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start engine", e)
            false
        }
    }

    /** 从 assets/engine/ 解压引擎文件。 */
    private fun extractFromAssets(targetDir: File): Boolean {
        return try {
            targetDir.mkdirs()
            val files = context.assets.list("engine").orEmpty()
            if (files.isEmpty()) {
                Log.e(TAG, "assets/engine is empty - no engine payload bundled")
                return false
            }
            for (fileName in files) {
                val targetFile = File(targetDir, fileName)
                context.assets.open("engine/$fileName").use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                targetFile.setExecutable(true, false)
            }
            Log.i(TAG, "Engine extracted successfully: ${files.joinToString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract engine", e)
            false
        }
    }

    /** 等待 dsh web 服务就绪。 */
    private suspend fun waitForReady() {
        val timeoutMs = 30_000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (ping()) {
                Log.i(TAG, "Engine service is ready")
                isRunning = true
                return
            }
            delay(300)
        }

        throw RuntimeException("Engine startup timeout after ${timeoutMs}ms")
    }

    /** 停止引擎服务。 */
    fun stop() {
        process?.let {
            it.destroy()
            Log.i(TAG, "Engine process destroyed: PID=${it.pid()}")
        }
        process = null
        isRunning = false
    }

    /** 探测引擎 HTTP 端口是否可用。 */
    fun ping(): Boolean {
        return try {
            val conn = URL(baseUrl()).openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499
        } catch (_: Exception) {
            false
        }
    }

    /** 发送消息到引擎。 */
    suspend fun sendMessage(content: String, sessionId: String? = null): String =
        withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl()/api/chat").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.connectTimeout = 30_000
                conn.readTimeout = 120_000

                val payload = JSONObject().apply {
                    put("content", content)
                    put("sessionId", sessionId)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw RuntimeException("HTTP $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                throw e
            } finally {
                conn.disconnect()
            }
        }
}

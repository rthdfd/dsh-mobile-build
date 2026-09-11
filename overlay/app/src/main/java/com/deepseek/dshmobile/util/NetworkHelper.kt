package com.deepseek.dshmobile.util

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Suppress("unused")
class NetworkHelper(private val context: Context) {

    private val trustAllCertificates = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private fun createSSLSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCertificates), SecureRandom())
        return sslContext.socketFactory
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true } // 开发阶段允许自签名证书
            .sslSocketFactory(createSSLSocketFactory(), trustAllCertificates)
            .build()
    }

    fun postJson(url: String, body: String, callback: Callback) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).enqueue(callback)
    }

    fun get(url: String, callback: Callback) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).enqueue(callback)
    }

    fun websocket(url: String, listener: okhttp3.WebSocketListener) {
        val request = Request.Builder().url(url).build()
        client.newWebSocket(request, listener)
    }

    fun cancelAll() {
        client.dispatcher.cancelAll()
    }
}

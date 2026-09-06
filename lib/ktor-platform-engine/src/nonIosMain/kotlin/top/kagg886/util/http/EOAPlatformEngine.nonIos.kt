package top.kagg886.util.http

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.utils.io.KtorDsl
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@KtorDsl
actual fun HttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            ignoreSSL()
            dns(
                Dns.SYSTEM.interceptor { hostname, addresses ->
                    val addressDebugPrint = addresses.joinToString(",") { it.hostAddress }
                    Logger.withTag("OkHttp").v("query dns result: $hostname -> $addressDebugPrint")
                    addresses.sortedBy { it !is Inet4Address }
                }
            )
            eventListener(TraceEventListener())
        }
    }
    block()
}


fun OkHttpClient.Builder.ignoreSSL() {
    val sslContext = SSLContext.getInstance("SSL")
    val trust = object : X509TrustManager {
        override fun checkClientTrusted(
            p0: Array<out X509Certificate>?,
            p1: String?,
        ) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) =
            Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
    sslContext.init(
        null,
        arrayOf(
            trust,
        ),
        SecureRandom(),
    )

    sslSocketFactory(sslContext.socketFactory, trust)
    hostnameVerifier { _, _ -> true }
}

private object BypassSSLSocketFactory : SSLSocketFactory() {
    @Throws(IOException::class)
    override fun createSocket(
        paramSocket: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean
    ): Socket {
        val inetAddress = paramSocket!!.inetAddress
        val sslSocket =
            (getDefault().createSocket(inetAddress, port) as SSLSocket).apply {
                enabledProtocols = supportedProtocols
            }
        return sslSocket
    }

    override fun createSocket(paramString: String?, paramInt: Int): Socket? = null

    override fun createSocket(
        paramString: String?,
        paramInt1: Int,
        paramInetAddress: InetAddress?,
        paramInt2: Int,
    ): Socket? = null

    override fun createSocket(paramInetAddress: InetAddress?, paramInt: Int): Socket? = null

    override fun createSocket(
        paramInetAddress1: InetAddress?,
        paramInt1: Int,
        paramInetAddress2: InetAddress?,
        paramInt2: Int,
    ): Socket? = null

    override fun getDefaultCipherSuites(): Array<String> = arrayOf()

    override fun getSupportedCipherSuites(): Array<String> = arrayOf()
}

@Suppress("CustomX509TrustManager")
private object BypassTrustManager : X509TrustManager {
    @Suppress("TrustAllX509TrustManager")
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit

    @Suppress("TrustAllX509TrustManager")
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

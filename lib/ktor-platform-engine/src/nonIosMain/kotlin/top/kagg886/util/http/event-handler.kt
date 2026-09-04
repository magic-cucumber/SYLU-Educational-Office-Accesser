package top.kagg886.util.http

import co.touchlab.kermit.Logger
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Dispatcher
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

data class TraceEventListener(private val logger: Logger = Logger.withTag("OkHttp")) :
    EventListener() {

    private fun Call.id(): String =
        Integer.toHexString(System.identityHashCode(this))

    private fun Call.prefix(): String =
        "[${id()}] ${request().method} ${request().url}"

    override fun callStart(call: Call) {
        logger.v("${call.prefix()} callStart")
    }

    override fun dispatcherQueueStart(
        call: Call,
        dispatcher: Dispatcher,
    ) {
        logger.v(
            "${call.prefix()} dispatcherQueueStart " +
                    "running=${dispatcher.runningCallsCount()} " +
                    "queued=${dispatcher.queuedCallsCount()}"
        )
    }

    override fun dispatcherQueueEnd(
        call: Call,
        dispatcher: Dispatcher,
    ) {
        logger.v(
            "${call.prefix()} dispatcherQueueEnd " +
                    "running=${dispatcher.runningCallsCount()} " +
                    "queued=${dispatcher.queuedCallsCount()}"
        )
    }

    override fun proxySelectStart(
        call: Call,
        url: HttpUrl,
    ) {
        logger.v("${call.prefix()} proxySelectStart url=$url")
    }

    override fun proxySelectEnd(
        call: Call,
        url: HttpUrl,
        proxies: List<Proxy>,
    ) {
        logger.v("${call.prefix()} proxySelectEnd proxies=$proxies")
    }

    override fun dnsStart(
        call: Call,
        domainName: String,
    ) {
        logger.v("${call.prefix()} dnsStart domain=$domainName")
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<InetAddress>,
    ) {
        logger.v(
            "${call.prefix()} dnsEnd domain=$domainName " +
                    "addresses=${inetAddressList.joinToString { it.hostAddress ?: it.toString() }}"
        )
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
    ) {
        logger.v(
            "${call.prefix()} connectStart " +
                    "address=$inetSocketAddress proxy=$proxy"
        )
    }

    override fun secureConnectStart(call: Call) {
        logger.v("${call.prefix()} secureConnectStart")
    }

    override fun secureConnectEnd(
        call: Call,
        handshake: Handshake?,
    ) {
        logger.v(
            "${call.prefix()} secureConnectEnd " +
                    "tls=${handshake?.tlsVersion} " +
                    "cipher=${handshake?.cipherSuite}"
        )
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
    ) {
        logger.v(
            "${call.prefix()} connectEnd " +
                    "address=$inetSocketAddress proxy=$proxy protocol=$protocol"
        )
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException,
    ) {
        logger.v(
            "${call.prefix()} connectFailed " +
                    "address=$inetSocketAddress proxy=$proxy protocol=$protocol " +
                    "exception=${ioe::class.simpleName}: ${ioe.message}"
        )
    }

    override fun connectionAcquired(
        call: Call,
        connection: Connection,
    ) {
        logger.v(
            "${call.prefix()} connectionAcquired " +
                    "route=${connection.route()} " +
                    "protocol=${connection.protocol()} " +
                    "handshake=${connection.handshake()?.tlsVersion}"
        )
    }

    override fun connectionReleased(
        call: Call,
        connection: Connection,
    ) {
        logger.v(
            "${call.prefix()} connectionReleased " +
                    "route=${connection.route()}"
        )
    }

    override fun requestHeadersStart(call: Call) {
        logger.v("${call.prefix()} requestHeadersStart")
    }

    override fun requestHeadersEnd(
        call: Call,
        request: Request,
    ) {
        logger.v("${call.prefix()} requestHeadersEnd")
    }

    override fun requestBodyStart(call: Call) {
        logger.v("${call.prefix()} requestBodyStart")
    }

    override fun requestBodyEnd(
        call: Call,
        byteCount: Long,
    ) {
        logger.v("${call.prefix()} requestBodyEnd bytes=$byteCount")
    }

    override fun requestFailed(
        call: Call,
        ioe: IOException,
    ) {
        logger.v(
            "${call.prefix()} requestFailed " +
                    "${ioe::class.simpleName}: ${ioe.message}"
        )
    }

    override fun responseHeadersStart(call: Call) {
        logger.v("${call.prefix()} responseHeadersStart")
    }

    override fun responseHeadersEnd(
        call: Call,
        response: Response,
    ) {
        logger.v(
            "${call.prefix()} responseHeadersEnd " +
                    "code=${response.code} protocol=${response.protocol}"
        )
    }

    override fun responseBodyStart(call: Call) {
        logger.v("${call.prefix()} responseBodyStart")
    }

    override fun responseBodyEnd(
        call: Call,
        byteCount: Long,
    ) {
        logger.v("${call.prefix()} responseBodyEnd bytes=$byteCount")
    }

    override fun responseFailed(
        call: Call,
        ioe: IOException,
    ) {
        logger.v(
            "${call.prefix()} responseFailed " +
                    "${ioe::class.simpleName}: ${ioe.message}"
        )
    }

    override fun retryDecision(
        call: Call,
        exception: IOException,
        retry: Boolean,
    ) {
        logger.v(
            "${call.prefix()} retryDecision retry=$retry " +
                    "exception=${exception::class.simpleName}: ${exception.message}"
        )
    }

    override fun followUpDecision(
        call: Call,
        networkResponse: Response,
        nextRequest: Request?,
    ) {
        logger.v(
            "${call.prefix()} followUpDecision " +
                    "code=${networkResponse.code} " +
                    "next=${nextRequest?.url}"
        )
    }

    override fun cacheHit(
        call: Call,
        response: Response,
    ) {
        logger.v("${call.prefix()} cacheHit code=${response.code}")
    }

    override fun cacheMiss(call: Call) {
        logger.v("${call.prefix()} cacheMiss")
    }

    override fun cacheConditionalHit(
        call: Call,
        cachedResponse: Response,
    ) {
        logger.v(
            "${call.prefix()} cacheConditionalHit code=${cachedResponse.code}"
        )
    }

    override fun satisfactionFailure(
        call: Call,
        response: Response,
    ) {
        logger.v(
            "${call.prefix()} satisfactionFailure code=${response.code}"
        )
    }

    override fun canceled(call: Call) {
        logger.v("${call.prefix()} canceled")
    }

    override fun callEnd(call: Call) {
        logger.v("${call.prefix()} callEnd")
    }

    override fun callFailed(
        call: Call,
        ioe: IOException,
    ) {
        logger.v(
            "${call.prefix()} callFailed " +
                    "${ioe::class.simpleName}: ${ioe.message}"
        )
    }
}
package top.kagg886.util.http

import okhttp3.Dns
import java.net.InetAddress


fun Dns.interceptor(lookup: (String, List<InetAddress>) -> List<InetAddress>): Dns =
    Dns { hostname ->
        val result = lookup(hostname)
        return@Dns lookup(hostname, result)
    }
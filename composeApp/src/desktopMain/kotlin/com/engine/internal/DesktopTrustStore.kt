package com.engine.internal

import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Desktop stand-in for the Android trust-store export the engine wrapper calls. It hands the
 * engine the system CA bundle (the Flatpak runtime ships one), or else the Java runtime's own
 * trusted certificates as PEM.
 */
internal object AndroidTrustStore {
    private val systemBundles = listOf(
        "/etc/ssl/certs/ca-certificates.crt",
        "/etc/pki/tls/certs/ca-bundle.crt",
        "/etc/ssl/cert.pem",
    )

    fun export(dataDirectory: File): File {
        systemBundles.map(::File).firstOrNull { it.isFile && it.length() > 0 }?.let { return it }

        val trustDirectory = File(dataDirectory, "tls").apply { mkdirs() }
        val destination = File(trustDirectory, "jvm-ca.pem")
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        val certificates = factory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .flatMap { it.acceptedIssuers.asList() }
        check(certificates.isNotEmpty()) { "Java trust store contains no certificates" }
        val encoder = Base64.getMimeEncoder(64, "\n".toByteArray())
        destination.writeText(
            certificates.joinToString("") { certificate: X509Certificate ->
                "-----BEGIN CERTIFICATE-----\n${encoder.encodeToString(certificate.encoded)}\n-----END CERTIFICATE-----\n"
            },
        )
        return destination
    }
}

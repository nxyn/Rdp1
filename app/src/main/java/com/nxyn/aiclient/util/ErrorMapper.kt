package com.nxyn.aiclient.util

import com.nxyn.aiclient.domain.model.ConnectionResult
import com.nxyn.aiclient.domain.model.DebugInfo
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

object ErrorMapper {
    fun mapHttpError(status: Int, body: String? = null): ConnectionResult {
        val sanitizedBody = body?.take(500)
        return when (status) {
            400 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Bad request",
                possibleFix = "The provider rejected the request.${sanitizedBody?.let { " $it" } ?: ""}"
            )
            401 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Unauthorized",
                possibleFix = "Check your API key."
            )
            403 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Access denied",
                possibleFix = "The provider rejected this request."
            )
            404 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Endpoint or model not found",
                possibleFix = "Check the Base URL and Model ID."
            )
            408 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Request timed out",
                possibleFix = "Try again later."
            )
            429 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Rate limit exceeded",
                possibleFix = "Try again later."
            )
            in 500..599 -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Provider server error",
                possibleFix = "The provider server returned an error. Try again later."
            )
            else -> ConnectionResult(
                success = false,
                httpStatus = status,
                message = "Request failed (HTTP $status)",
                possibleFix = sanitizedBody
            )
        }
    }

    fun mapNetworkError(throwable: Throwable): ConnectionResult {
        return when (throwable) {
            is SSLHandshakeException, is SSLPeerUnverifiedException, is CertPathValidatorException -> ConnectionResult(
                success = false,
                message = "Secure connection failed",
                possibleFix = "The server's TLS certificate could not be verified."
            )
            is SocketTimeoutException -> ConnectionResult(
                success = false,
                message = "Request timed out",
                possibleFix = "The server did not respond in time."
            )
            is UnknownHostException, is ConnectException -> ConnectionResult(
                success = false,
                message = "Unable to reach server",
                possibleFix = "No HTTP response was received. Check the endpoint URL and network connection."
            )
            is IOException -> ConnectionResult(
                success = false,
                message = "Network connection failed",
                possibleFix = "The server did not return an HTTP response. Check URL, internet connection, server availability, TLS certificate, firewall, and local network access."
            )
            else -> ConnectionResult(
                success = false,
                message = throwable.message ?: "Unknown error",
                possibleFix = "An unexpected error occurred."
            )
        }
    }

    fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        val sensitive = setOf("authorization", "x-api-key", "api-key", "proxy-authorization")
        return headers.mapValues { (key, value) ->
            if (key.lowercase() in sensitive) "••••••••" else value
        }
    }

    fun buildDebugInfo(
        requestUrl: String,
        httpStatus: Int?,
        responseTimeMs: Long,
        headers: Map<String, String>,
        body: String?,
        secrets: List<String>
    ): DebugInfo {
        return DebugInfo(
            requestUrl = requestUrl,
            httpStatus = httpStatus,
            responseTimeMs = responseTimeMs,
            responseHeaders = sanitizeHeaders(headers),
            sanitizedBody = body?.let { UrlHelper.redactSecrets(it, secrets) }?.take(4000)
        )
    }
}

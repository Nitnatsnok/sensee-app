package app.sensee.core.network

import app.sensee.core.coroutines.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

public suspend inline fun <reified T> safeBody(crossinline request: suspend () -> HttpResponse): NetworkResult<T> =
    runCatchingCancellable { request().body<T>() }
        .fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { NetworkResult.Failure(it.toNetworkError()) },
        )

@PublishedApi
internal suspend fun Throwable.toNetworkError(): NetworkError =
    when (this) {
        is ClientRequestException -> toNetworkHttpError()
        is ServerResponseException -> toNetworkHttpError()
        is ResponseException -> toNetworkHttpError()
        is HttpRequestTimeoutException -> NetworkError.Timeout(message)
        is JsonConvertException,
        is SerializationException,
        -> NetworkError.Serialization(message)
        is IOException -> NetworkError.Connectivity(message)
        else -> NetworkError.Unknown(message)
    }

@PublishedApi
internal suspend fun ResponseException.toNetworkHttpError(): NetworkError.Http =
    NetworkError.Http(
        statusCode = response.status.value,
        responseBody = safeBodyAsTextOrNull(response),
        message = message,
    )

private suspend fun safeBodyAsTextOrNull(response: HttpResponse): String? =
    runCatchingCancellable {
        response.bodyAsText()
    }.getOrElse {
        null
    }

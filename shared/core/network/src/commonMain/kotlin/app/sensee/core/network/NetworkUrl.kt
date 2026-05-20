package app.sensee.core.network

public fun buildNetworkUrl(
    baseUrl: String,
    path: String,
): String {
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    val normalizedPath = path.trimStart('/')

    return if (normalizedPath.isEmpty()) {
        normalizedBaseUrl
    } else {
        "$normalizedBaseUrl/$normalizedPath"
    }
}

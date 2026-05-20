package app.sensee.core.network

public fun interface NetworkHeadersProvider {
    public fun headers(): Map<String, String>
}

public object EmptyNetworkHeadersProvider : NetworkHeadersProvider {
    override fun headers(): Map<String, String> = emptyMap()
}

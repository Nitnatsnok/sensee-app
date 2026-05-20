package app.sensee.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher

public class AppDispatchers(
    public val main: MainCoroutineDispatcher,
    public val default: CoroutineDispatcher,
    public val io: CoroutineDispatcher,
)

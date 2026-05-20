package app.sensee.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val appIoDispatcher: CoroutineDispatcher = Dispatchers.Default

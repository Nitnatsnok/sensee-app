package app.sensee.core.decompose.logic

import app.sensee.core.decompose.context.AppComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.serialization.KSerializer

public fun <L : Logic> AppComponentContext.getOrCreateLogic(
    key: LogicKey,
    factory: () -> L,
): L =
    instanceKeeper.getOrCreate(
        key = key.value,
        factory = factory,
    )

public fun <State : Any, L : StatefulLogic<State>> AppComponentContext.getOrCreateLogic(
    key: LogicKey,
    stateSerializer: KSerializer<State>,
    factory: (savedState: State?) -> L,
): L {
    val logic =
        instanceKeeper.getOrCreate(
            key = key.value,
        ) {
            val restoredState =
                stateKeeper.consume(
                    key = key.value,
                    strategy = stateSerializer,
                )

            factory(restoredState)
        }

    stateKeeper.register(
        key = key.value,
        strategy = stateSerializer,
    ) {
        logic.saveState()
    }

    return logic
}

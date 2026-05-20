package app.sensee.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigator
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable
public data class NodeConfig<T : ScreenConfig>(
    val id: Long = NavigationInstanceKey.STABLE,
    val destination: T,
)

public fun <T : ScreenConfig> nodeConfigSerializer(destinationSerializer: KSerializer<T>): KSerializer<NodeConfig<T>> =
    NodeConfigKSerializer(destinationSerializer)

private class NodeConfigKSerializer<T : ScreenConfig>(
    private val destinationSerializer: KSerializer<T>,
) : KSerializer<NodeConfig<T>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("app.sensee.core.decompose.navigation.NodeConfig") {
            element<Long>("id")
            element("destination", destinationSerializer.descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: NodeConfig<T>,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.id)
            encodeSerializableElement(descriptor, 1, destinationSerializer, value.destination)
        }
    }

    override fun deserialize(decoder: Decoder): NodeConfig<T> {
        var id = NavigationInstanceKey.STABLE
        var destination: T? = null

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeLongElement(descriptor, 0)
                    1 -> destination = decodeSerializableElement(descriptor, 1, destinationSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return NodeConfig(
            id = id,
            destination = destination ?: throw SerializationException("NodeConfig.destination is missing"),
        )
    }
}

public inline fun <C : ScreenConfig> StackNavigator<NodeConfig<C>>.replaceAll(
    vararg configurations: C,
    recreateIfSameConfig: Boolean = false,
    noinline matchKeySelector: (C) -> Any? = { it::class },
    crossinline onComplete: () -> Unit = { },
) {
    navigate(
        transformer = { currentStack ->
            currentStack.replaceAllNodes(
                configurations = configurations,
                recreateIfSameConfig = recreateIfSameConfig,
                matchKeySelector = matchKeySelector,
            )
        },
        onComplete = { _, _ ->
            onComplete()
        },
    )
}

public inline fun <C : ScreenConfig> StackNavigator<NodeConfig<C>>.pushToFront(
    configuration: C,
    noinline matchKeySelector: (C) -> Any? = { it::class },
    crossinline onComplete: () -> Unit = {},
) {
    navigate(
        transformer = { stack -> stack.pushNodeToFront(configuration, matchKeySelector) },
        onComplete = { _, _ -> onComplete() },
    )
}

public inline fun <C : ScreenConfig> StackNavigator<NodeConfig<C>>.bringToFront(
    configuration: C,
    recreateIfSameConfig: Boolean = false,
    noinline matchKeySelector: (C) -> Any? = { it::class },
    crossinline onComplete: () -> Unit = {},
) {
    navigate(
        transformer = { stack ->
            stack.bringNodeToFront(
                configuration = configuration,
                recreateIfSameConfig = recreateIfSameConfig,
                matchKeySelector = matchKeySelector,
            )
        },
        onComplete = { _, _ -> onComplete() },
    )
}

@PublishedApi
internal fun <C : ScreenConfig> List<NodeConfig<C>>.replaceAllNodes(
    configurations: Array<out C>,
    recreateIfSameConfig: Boolean,
    matchKeySelector: (C) -> Any?,
): List<NodeConfig<C>> {
    val remainingStack = toMutableList()
    val resultStack = mutableListOf<NodeConfig<C>>()
    val configurationIterator = configurations.iterator()

    while (configurationIterator.hasNext()) {
        val configuration = configurationIterator.next()
        val existingIndex =
            remainingStack.indexOfFirst {
                matchKeySelector(it.destination) == matchKeySelector(configuration)
            }

        if (existingIndex >= 0) {
            val nodeConfig = remainingStack.removeAt(existingIndex)
            resultStack.add(nodeConfig.updated(configuration, recreateIfSameConfig))
        } else {
            resultStack.add(configuration.toNodeConfig())
        }
    }

    return resultStack
}

@PublishedApi
internal fun <C : ScreenConfig> List<NodeConfig<C>>.pushNodeToFront(
    configuration: C,
    matchKeySelector: (C) -> Any?,
): List<NodeConfig<C>> {
    val nodeConfig = findNodeConfig(configuration, matchKeySelector)
    return nodeConfig?.let {
        this - it + it.copy(destination = configuration)
    } ?: (this + configuration.toNodeConfig())
}

@PublishedApi
internal fun <C : ScreenConfig> List<NodeConfig<C>>.bringNodeToFront(
    configuration: C,
    recreateIfSameConfig: Boolean,
    matchKeySelector: (C) -> Any?,
): List<NodeConfig<C>> {
    val nodeConfig = findNodeConfig(configuration, matchKeySelector)
    return if (nodeConfig == null) {
        this + configuration.toNodeConfig()
    } else {
        this - nodeConfig + nodeConfig.updated(configuration, recreateIfSameConfig)
    }
}

private fun <C : ScreenConfig> List<NodeConfig<C>>.findNodeConfig(
    configuration: C,
    matchKeySelector: (C) -> Any?,
): NodeConfig<C>? = find { matchKeySelector(it.destination) == matchKeySelector(configuration) }

private fun <C : ScreenConfig> NodeConfig<C>.updated(
    configuration: C,
    recreateIfSameConfig: Boolean,
): NodeConfig<C> =
    if (destination == configuration && recreateIfSameConfig) {
        copy(id = NavigationInstanceKey.next())
    } else {
        copy(destination = configuration)
    }

public fun <T : ScreenConfig> T.toNodeConfig(id: Long = NavigationInstanceKey.STABLE): NodeConfig<T> =
    NodeConfig(id, this)

package app.sensee.appShell.root

import app.sensee.appShell.primary.PrimaryShellComponent
import app.sensee.appShell.primary.PrimaryShellConfig
import app.sensee.appShell.primary.WebSectionRoute
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.TargetedScreenConfig
import app.sensee.core.decompose.navigation.pop
import app.sensee.core.platform.Platform
import app.sensee.core.platform.PlatformEnvironment
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.startup.domain.PreloadAppStartupUseCase
import app.sensee.feature.startup.presentation.api.StartupComponent
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AssistedInject
public class DefaultRootComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val deepLink: String?,
    private val startupComponentFactory: StartupComponent.Factory,
    private val primaryShellComponentFactory: PrimaryShellComponent.Factory,
    private val preloadAppStartup: PreloadAppStartupUseCase,
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
) : RootComponent,
    AppComponentContext by componentContext {
    override val platform: Platform = platformEnvironment.platform

    private val stackNavigation = StackNavigation<ScreenConfig>()
    private val componentScope = CoroutineScope(appDispatchers.main.immediate + SupervisorJob())

    override val stack: Value<ChildStack<ScreenConfig, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            // Persist the root stack: a process-death restore returns the user's
            // deep target instead of replaying the splash from scratch.
            serializer = screenConfigSerializer,
            initialConfiguration = StartupConfig,
            handleBackButton = true,
            navigation = this,
            childFactory = ::createChild,
        )

    // Root contributes no path segment and no back stack: it holds at most one
    // active host, and the splash (`StartupConfig`) is swapped via
    // `replaceCurrent`, so it never becomes its own browser-history entry and
    // never gets a URL. The active section's path comes from the child
    // `PrimaryShellComponent`, selected here.
    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = stackNavigation,
            stack = stack,
            serializer = screenConfigSerializer,
            enableHistory = false,
            pathMapper = { null },
            childSelector = { child -> child.instance as? WebNavigationOwner },
        )

    init {
        // Warm-restore safety net: when Decompose restores a non-`StartupConfig`
        // stack (deep-link / process-death return), the splash is skipped, so
        // the use case has to be kicked here too. Providers cache app-scope —
        // at most one fetch per session, splash + root share the same cache.
        componentScope.launch { preloadAppStartup() }
        lifecycle.doOnDestroy(componentScope::cancel)
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus {
        val rootBranch = target.toRootBranch() ?: return NavigationRequestStatus.Unhandled

        openRootBranch(
            branch = rootBranch,
            onComplete = onComplete,
        )
        return NavigationRequestStatus.Handled
    }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        stackNavigation.pop(onResult = onResult)
    }

    private fun createChild(
        config: ScreenConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            StartupConfig ->
                startupComponentFactory.create(
                    componentContext = componentContext,
                    onFinished = ::finishStartup,
                )

            else -> createHostChild(config, componentContext)
        }

    private fun finishStartup() {
        stackNavigation.replaceCurrent(
            configuration = PrimaryShellConfig(target = deepLinkLanding()),
        )
    }

    // Cold-start deep-link resolution. Warm reloads come from Decompose's own
    // history.state restore — only typed/shared URLs route through here.
    private fun deepLinkLanding(): ScreenConfig {
        val afterHost =
            deepLink
                ?.substringAfter("://", "")
                ?.substringAfter('/', "")
                ?: return HomeConfig.Home

        val path = afterHost.substringBefore('?').substringBefore('#')
        val query = afterHost.substringAfter('?', "").substringBefore('#')

        val segments =
            path
                .split('/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val parameters =
            query
                .split('&')
                .filter { it.isNotEmpty() }
                .associate { pair ->
                    pair.substringBefore('=') to pair.substringAfter('=', "")
                }

        return WebSectionRoute.landingForPath(segments, parameters)
    }

    private fun createHostChild(
        config: ScreenConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            is PrimaryShellConfig ->
                primaryShellComponentFactory.create(
                    componentContext = componentContext,
                    target = config.target,
                )

            else -> error("Unknown root screen config: $config")
        }

    private fun openRootBranch(
        branch: RootNavigationBranch,
        onComplete: (isSuccess: Boolean) -> Unit,
    ) {
        when (branch) {
            is RootNavigationBranch.PrimaryShell ->
                openTargetedHost(
                    hostConfig = branch.config,
                    isHostConfig = { config -> config.isPrimaryShellTarget() },
                    openOnActiveHost = { target, callback ->
                        (stack.value.active.instance as? PrimaryShellComponent)?.open(
                            target = target,
                            onComplete = callback,
                        ) ?: NavigationRequestStatus.Unhandled
                    },
                    onComplete = onComplete,
                )
        }
    }

    private fun openTargetedHost(
        hostConfig: TargetedScreenConfig<ScreenConfig>,
        isHostConfig: (ScreenConfig) -> Boolean,
        openOnActiveHost: (target: ScreenConfig, onComplete: (isSuccess: Boolean) -> Unit) -> NavigationRequestStatus,
        onComplete: (isSuccess: Boolean) -> Unit,
    ) {
        val hasHost = stack.value.items.any { isHostConfig(it.configuration) }

        if (!hasHost) {
            stackNavigation.bringToFront(hostConfig) {
                onComplete(true)
            }
            return
        }

        stackNavigation.navigate(
            transformer = { stack ->
                val existingIndex = stack.indexOfLast(isHostConfig)
                stack.toMutableList().apply {
                    add(removeAt(existingIndex))
                }
            },
            onComplete = { _, _ ->
                val target = hostConfig.target
                if (target == null) {
                    onComplete(false)
                    return@navigate
                }

                val isHandled =
                    openOnActiveHost(
                        target,
                        onComplete,
                    ) == NavigationRequestStatus.Handled
                if (!isHandled) {
                    onComplete(false)
                }
            },
        )
    }

    private fun ScreenConfig.toRootBranch(): RootNavigationBranch? =
        when (this) {
            is HomeConfig,
            is PracticeConfig,
            is LibraryConfig,
            is VocabularyEditorConfig,
            is ProfileConfig,
            -> RootNavigationBranch.PrimaryShell(PrimaryShellConfig(target = this))

            else -> null
        }

    private fun ScreenConfig.isPrimaryShellTarget(): Boolean = this is PrimaryShellConfig

    private sealed interface RootNavigationBranch {
        data class PrimaryShell(
            val config: PrimaryShellConfig,
        ) : RootNavigationBranch
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<RootComponent.Factory>(),
    )
    public fun interface Factory : RootComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            deepLink: String?,
        ): DefaultRootComponent
    }
}

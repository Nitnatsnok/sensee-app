package app.sensee.appShell.primary

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.NodeConfig
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.bringToFront
import app.sensee.core.decompose.navigation.nodeConfigSerializer
import app.sensee.core.decompose.navigation.pop
import app.sensee.core.decompose.navigation.replaceAll
import app.sensee.core.decompose.value.asFlow
import app.sensee.feature.home.presentation.api.HomeSectionComponent
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import app.sensee.feature.library.presentation.api.LibrarySectionComponent
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import app.sensee.feature.practice.presentation.api.PracticeSectionComponent
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyEditorSectionComponent
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@AssistedInject
public class DefaultPrimaryShellComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val target: ScreenConfig?,
    private val sectionComponentFactories: PrimarySectionComponentFactories,
    appDispatchers: AppDispatchers,
) : PrimaryShellComponent,
    AppComponentContext by componentContext {
    private val initialSectionConfig = target.toPrimarySectionConfig()
    private val stackNavigation = StackNavigation<NodeConfig<ScreenConfig>>()
    private val componentScope = CoroutineScope(appDispatchers.main.immediate + SupervisorJob())

    override val stack: Value<ChildStack<NodeConfig<ScreenConfig>, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            serializer = nodeConfigSerializer(screenConfigSerializer),
            initialConfiguration = NodeConfig(destination = initialSectionConfig),
            handleBackButton = true,
            navigation = this,
            childFactory = ::createChild,
        )

    // Bottom-tab semantics: `enableHistory = false` keeps only the active
    // section in browser history (Decompose's documented mode for tab
    // navigation), so the browser Back button doesn't cycle through tabs.
    // The path segment comes from the section under the NodeConfig wrapper;
    // the wrapper's volatile recreation `id` must never reach the URL.
    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = stackNavigation,
            stack = stack,
            serializer = nodeConfigSerializer(screenConfigSerializer),
            enableHistory = false,
            pathMapper = { child: Child.Created<NodeConfig<ScreenConfig>, AppComponent> ->
                (child.configuration.destination as? PrimarySectionConfig)
                    ?.let(WebSectionRoute::pathFor)
            },
            // Chain into the active section's own web navigation so a section
            // with deeper routing (Practice) contributes the rest of the path.
            // Sections without it (single-screen Home/Library/Profile) are not
            // WebNavigationOwner and just keep their section segment.
            childSelector = { child -> child.instance as? WebNavigationOwner },
        )

    override val selectedSection: StateFlow<PrimarySection>
        field = MutableStateFlow(initialSectionConfig.toPrimarySection())

    // Derived, not pushed: each section owns its own `BottomBarVisibilityOwner`; sections
    // without one keep the bar shown.
    override val showBottomBar: StateFlow<Boolean> =
        stack
            .asFlow()
            .flatMapLatest { childStack ->
                (childStack.active.instance as? BottomBarVisibilityOwner)?.showBottomBar
                    ?: flowOf(true)
            }.stateIn(componentScope, SharingStarted.Eagerly, initialValue = true)

    // Catches the system back on a single-item stack whose only item is not
    // Home (cold deep-link or LRU rotation), so the next back lands on Home
    // instead of exiting the app from a section the user never opened.
    private val homeRootBackCallback =
        BackCallback(isEnabled = initialSectionConfig !is PrimarySectionConfig.HomeSection) {
            replaceWithHomeRoot {}
        }

    init {
        backHandler.register(homeRootBackCallback)
        val stackSubscription =
            stack.subscribe { stack ->
                homeRootBackCallback.isEnabled =
                    primaryShellBackAction(stack.items.map { it.configuration.destination }) ==
                    PrimaryShellBackAction.ReplaceWithHomeRoot
                selectedSection.update {
                    stack.active.configuration.destination
                        .toPrimarySection()
                }
            }
        lifecycle.doOnDestroy(stackSubscription::cancel)
        lifecycle.doOnDestroy(componentScope::cancel)
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            is HomeConfig -> openSection(PrimarySectionConfig.HomeSection(target), onComplete)
            is PracticeConfig -> openSection(PrimarySectionConfig.PracticeSection(target), onComplete)
            is LibraryConfig -> openSection(PrimarySectionConfig.LibrarySection(target), onComplete)
            is VocabularyEditorConfig -> openSection(PrimarySectionConfig.VocabularyEditor(target), onComplete)
            is ProfileConfig -> openSection(PrimarySectionConfig.ProfileSection(target), onComplete)
            else -> NavigationRequestStatus.Unhandled
        }

    override fun selectSection(section: PrimarySection) {
        selectSectionConfig(
            sectionConfig =
                when (section) {
                    PrimarySection.Home -> PrimarySectionConfig.HomeSection()
                    PrimarySection.Practice -> PrimarySectionConfig.PracticeSection()
                    PrimarySection.Library -> PrimarySectionConfig.LibrarySection()
                    PrimarySection.VocabularyEditor -> PrimarySectionConfig.VocabularyEditor()
                    PrimarySection.Profile -> PrimarySectionConfig.ProfileSection()
                },
        )
    }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        when (primaryShellBackAction(stack.value.items.map { it.configuration.destination })) {
            PrimaryShellBackAction.Pop -> stackNavigation.pop(onResult = onResult)
            PrimaryShellBackAction.ReplaceWithHomeRoot ->
                replaceWithHomeRoot { onResult(NavigationRequestStatus.Handled) }
            PrimaryShellBackAction.Unhandled -> onResult(NavigationRequestStatus.Unhandled)
        }
    }

    private fun replaceWithHomeRoot(onComplete: () -> Unit) {
        stackNavigation.replaceAll(
            PrimarySectionConfig.HomeSection(),
            onComplete = onComplete,
        )
    }

    private fun createChild(
        config: NodeConfig<ScreenConfig>,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (val destination = config.destination) {
            is PrimarySectionConfig.HomeSection ->
                sectionComponentFactories.home.create(
                    componentContext = componentContext,
                    target = destination.target,
                )

            is PrimarySectionConfig.PracticeSection ->
                sectionComponentFactories.practice.create(
                    componentContext = componentContext,
                    target = destination.target,
                )

            is PrimarySectionConfig.LibrarySection ->
                sectionComponentFactories.library.create(
                    componentContext = componentContext,
                    target = destination.target,
                )

            is PrimarySectionConfig.VocabularyEditor ->
                sectionComponentFactories.vocabularyEditor.create(
                    componentContext = componentContext,
                    target = destination.target,
                )

            is PrimarySectionConfig.ProfileSection ->
                sectionComponentFactories.profile.create(
                    componentContext = componentContext,
                    target = destination.target,
                )

            else -> error("Unknown primary shell config: $destination")
        }

    private fun openSection(
        sectionConfig: PrimarySectionConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus {
        stackNavigation.bringToFront(
            configuration = sectionConfig,
            recreateIfSameConfig = true,
        ) {
            onComplete(true)
        }
        return NavigationRequestStatus.Handled
    }

    private fun selectSectionConfig(
        sectionConfig: PrimarySectionConfig,
        onComplete: (isSuccess: Boolean) -> Unit = {},
    ): NavigationRequestStatus {
        stackNavigation.bringToFront(configuration = sectionConfig) {
            onComplete(true)
        }
        return NavigationRequestStatus.Handled
    }

    private fun ScreenConfig.toPrimarySection(): PrimarySection =
        when (this) {
            is PrimarySectionConfig.HomeSection -> PrimarySection.Home
            is PrimarySectionConfig.PracticeSection -> PrimarySection.Practice
            is PrimarySectionConfig.LibrarySection -> PrimarySection.Library
            is PrimarySectionConfig.VocabularyEditor -> PrimarySection.VocabularyEditor
            is PrimarySectionConfig.ProfileSection -> PrimarySection.Profile
            else -> error("Unknown primary section config: $this")
        }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<PrimaryShellComponent.Factory>(),
    )
    public fun interface Factory : PrimaryShellComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: ScreenConfig?,
        ): DefaultPrimaryShellComponent
    }
}

@Inject
public class PrimarySectionComponentFactories(
    public val home: HomeSectionComponent.Factory,
    public val practice: PracticeSectionComponent.Factory,
    public val library: LibrarySectionComponent.Factory,
    public val vocabularyEditor: VocabularyEditorSectionComponent.Factory,
    public val profile: ProfileSectionComponent.Factory,
)

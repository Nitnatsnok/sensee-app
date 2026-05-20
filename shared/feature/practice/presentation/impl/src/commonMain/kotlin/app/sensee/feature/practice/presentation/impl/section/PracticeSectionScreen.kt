package app.sensee.feature.practice.presentation.impl.section

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.api.DeckPracticeComponent
import app.sensee.feature.practice.presentation.api.PracticeHomeComponent
import app.sensee.feature.practice.presentation.api.PracticeSectionComponent
import app.sensee.feature.practice.presentation.impl.deck.DeckPracticeScreen
import app.sensee.feature.practice.presentation.impl.detail.CardDetailScreen
import app.sensee.feature.practice.presentation.impl.home.PracticeHomeScreen
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
public fun PracticeSectionScreen(
    component: PracticeSectionComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        Children(
            stack = component.stack,
            modifier = modifier.navigationBarsPadding(),
        ) { child ->
            when (val instance = child.instance) {
                is PracticeHomeComponent ->
                    PracticeHomeScreen(
                        component = instance,
                        modifier = Modifier,
                        textProvider = textProvider,
                    )

                is DeckPracticeComponent ->
                    DeckPracticeScreen(
                        component = instance,
                        modifier = Modifier,
                        textProvider = textProvider,
                    )

                is CardDetailComponent ->
                    CardDetailScreen(
                        component = instance,
                        modifier = Modifier,
                        textProvider = textProvider,
                    )

                else -> error("Unknown practice child: ${instance::class}")
            }
        }
    }
}

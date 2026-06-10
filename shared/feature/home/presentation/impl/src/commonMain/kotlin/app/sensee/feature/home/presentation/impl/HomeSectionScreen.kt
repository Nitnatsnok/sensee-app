package app.sensee.feature.home.presentation.impl

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.home.presentation.api.HomeComponent
import app.sensee.feature.home.presentation.api.HomeSectionComponent
import app.sensee.feature.home.presentation.impl.text.rememberHomeTextProvider
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
public fun HomeSectionScreen(
    component: HomeSectionComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberHomeTextProvider(),
) {
    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        Children(
            stack = component.stack,
            modifier = modifier.navigationBarsPadding(),
        ) { child ->
            when (val instance = child.instance) {
                is HomeComponent ->
                    HomeScreen(
                        component = instance,
                        modifier = Modifier,
                        textProvider = textProvider,
                    )

                else -> error("Unknown home child: ${instance::class}")
            }
        }
    }
}

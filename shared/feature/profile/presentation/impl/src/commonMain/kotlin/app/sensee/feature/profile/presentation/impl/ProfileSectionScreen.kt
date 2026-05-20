package app.sensee.feature.profile.presentation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
public fun ProfileSectionScreen(
    component: ProfileSectionComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileHomeTextProvider(),
) {
    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        Children(
            stack = component.stack,
            modifier = modifier,
        ) { child ->
            when (val instance = child.instance) {
                is ProfileHomeComponent ->
                    ProfileHomeScreen(
                        component = instance,
                        textProvider = textProvider,
                    )

                else -> error("Unknown profile child: ${instance::class}")
            }
        }
    }
}

@Composable
internal fun rememberProfileHomeTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileHomeTextProvider.withFallback(parent) }
}

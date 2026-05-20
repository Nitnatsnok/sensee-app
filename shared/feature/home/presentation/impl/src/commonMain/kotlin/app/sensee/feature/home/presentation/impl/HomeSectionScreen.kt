package app.sensee.feature.home.presentation.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.feature.home.presentation.api.HomeComponent
import app.sensee.feature.home.presentation.api.HomeSectionComponent
import app.sensee.ui.designSystem.component.UnimplementedScreen
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
public fun HomeSectionScreen(
    component: HomeSectionComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
    ) { child ->
        when (child.instance) {
            is HomeComponent -> HomeScreen(modifier = Modifier)
            else -> error("Unknown home child: ${child.instance::class}")
        }
    }
}

@Composable
public fun HomeScreen(modifier: Modifier = Modifier) {
    UnimplementedScreen(
        title = "Главная",
        modifier = modifier,
    )
}

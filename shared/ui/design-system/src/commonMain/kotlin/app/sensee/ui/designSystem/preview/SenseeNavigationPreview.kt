package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.navigation.SenseeBottomNavigationBar
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationActionButton
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationItem
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationRail
import app.sensee.ui.designSystem.icons.Add24px
import app.sensee.ui.designSystem.icons.Home24px
import app.sensee.ui.designSystem.icons.LibraryBooks24px
import app.sensee.ui.designSystem.icons.Person24px
import app.sensee.ui.designSystem.icons.Psychology24px

@Preview
@Composable
private fun SenseeBottomNavigationBarPreview() =
    SenseePreview {
        SenseeBottomNavigationBar {
            SenseeNavigationItem(label = "Home", icon = Home24px, selected = true, onClick = {})
            SenseeNavigationItem(label = "Library", icon = LibraryBooks24px, selected = false, onClick = {})
            SenseeNavigationActionButton(icon = Add24px, onClick = {}, contentDescription = "Add")
            SenseeNavigationItem(label = "Practice", icon = Psychology24px, selected = false, onClick = {})
            SenseeNavigationItem(label = "Profile", icon = Person24px, selected = false, onClick = {})
        }
    }

@Preview
@Composable
private fun SenseeNavigationRailCollapsedPreview() =
    SenseePreview {
        SenseeNavigationRail(
            modifier = Modifier.height(480.dp),
            expanded = false,
            onExpandedChange = {},
            action = { SenseeNavigationActionButton(icon = Add24px, onClick = {}, contentDescription = "Add") },
        ) {
            Spacer(modifier = Modifier.weight(1f))
            SenseeNavigationItem(
                label = "Home",
                icon = Home24px,
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SenseeNavigationItem(
                label = "Practice",
                icon = Psychology24px,
                selected = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SenseeNavigationItem(
                label = "Library",
                icon = LibraryBooks24px,
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }

@Preview
@Composable
private fun SenseeNavigationRailExpandedPreview() =
    SenseePreview {
        SenseeNavigationRail(
            modifier = Modifier.height(480.dp),
            expanded = true,
            onExpandedChange = {},
            action = { SenseeNavigationActionButton(icon = Add24px, onClick = {}, contentDescription = "Добавить") },
        ) {
            Spacer(modifier = Modifier.weight(1f))
            SenseeNavigationItem(
                label = "Главная",
                icon = Home24px,
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SenseeNavigationItem(
                label = "Практика",
                icon = Psychology24px,
                selected = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SenseeNavigationItem(
                label = "Библиотека",
                icon = LibraryBooks24px,
                selected = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }

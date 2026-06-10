package app.sensee.feature.home.presentation.impl.text

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.QuantityTextKey
import app.sensee.core.presentation.text.QuantityTextTemplates
import app.sensee.core.presentation.text.TextKey

internal object HomeTextKeys {
    val Loading = TextKey("home.loading")
    val LoadError = TextKey("home.load_error")
    val ReviewTitle = TextKey("home.review.title")
    val ReviewDueCount = QuantityTextKey("home.review.due_count")
    val ReviewDueCountCapped = TextKey("home.review.due_count_capped")
    val ReviewNothingDue = TextKey("home.review.nothing_due")
    val ReviewGoal = QuantityTextKey("home.review.goal")
    val ReviewCta = TextKey("home.review.cta")
}

internal val DefaultHomeTextProvider =
    MapTextProvider(
        mapOf(
            HomeTextKeys.Loading to "Загрузка...",
            HomeTextKeys.LoadError to "Не удалось загрузить данные",
            HomeTextKeys.ReviewTitle to "Стоит повторить",
            HomeTextKeys.ReviewDueCountCapped to "{0}+ карточек к повторению",
            HomeTextKeys.ReviewNothingDue to "На сегодня всё повторено",
            HomeTextKeys.ReviewCta to "Повторить сегодня",
        ),
        quantities =
            mapOf(
                HomeTextKeys.ReviewDueCount to
                    QuantityTextTemplates(
                        one = "{0} карточка к повторению",
                        few = "{0} карточки к повторению",
                        many = "{0} карточек к повторению",
                    ),
                HomeTextKeys.ReviewGoal to
                    QuantityTextTemplates(
                        one = "Цель: {0} карточка в день",
                        few = "Цель: {0} карточки в день",
                        many = "Цель: {0} карточек в день",
                    ),
            ),
    )

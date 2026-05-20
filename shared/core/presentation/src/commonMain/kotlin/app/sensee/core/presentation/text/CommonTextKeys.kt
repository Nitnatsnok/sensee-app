package app.sensee.core.presentation.text

public object CommonTextKeys {
    public val Retry: TextKey = TextKey("common.retry")
    public val Close: TextKey = TextKey("common.close")
}

public val DefaultCommonTextProvider: TextProvider =
    MapTextProvider(
        mapOf(
            CommonTextKeys.Retry to "Повторить",
            CommonTextKeys.Close to "Закрыть",
        ),
    )

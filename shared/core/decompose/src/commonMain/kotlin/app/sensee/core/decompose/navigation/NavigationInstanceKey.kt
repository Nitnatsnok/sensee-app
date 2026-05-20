package app.sensee.core.decompose.navigation

public object NavigationInstanceKey {
    public const val STABLE: Long = 0L

    private var nextValue: Long = 0L

    public fun next(): Long {
        nextValue += 1
        return nextValue
    }
}

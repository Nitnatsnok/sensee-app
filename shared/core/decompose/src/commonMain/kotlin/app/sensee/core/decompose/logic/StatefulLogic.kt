package app.sensee.core.decompose.logic

public interface StatefulLogic<State : Any> : Logic {
    public fun saveState(): State
}

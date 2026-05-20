package app.sensee.srs.core.model

public data class SrsAlgorithmInfo(
    val name: String,
    val version: String,
) {
    init {
        require(name.isNotBlank()) {
            "Algorithm name must not be blank"
        }

        require(version.isNotBlank()) {
            "Algorithm version must not be blank"
        }
    }
}

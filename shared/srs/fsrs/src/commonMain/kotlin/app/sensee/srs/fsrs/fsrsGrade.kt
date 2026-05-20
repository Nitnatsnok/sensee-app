package app.sensee.srs.fsrs

import app.sensee.srs.core.model.ReviewRating

internal val ReviewRating.fsrsGrade: Int
    get() =
        when (this) {
            ReviewRating.Again -> 1
            ReviewRating.Hard -> 2
            ReviewRating.Good -> 3
            ReviewRating.Easy -> 4
        }

package app.sensee.feature.library.data

import app.sensee.feature.library.data.remote.DeckDto
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.Deck
import app.sensee.feature.library.domain.DeckId
import app.sensee.feature.library.domain.DeckWithCards
import app.sensee.lexicon.domain.deriveLemmaKey
import app.sensee.lexicon.domain.isConfirmable
import app.sensee.lexicon.serialization.toDomain

/**
 * Project a Service deck straight from its remote [DeckDto] into the read-only catalog
 * model WITHOUT touching the local store. This is the browse-before-adopt path: a user
 * previews a Service suggestion's cards, while adopting it (subscribe) — a separate,
 * explicit action — is the only thing that ingests.
 *
 * Mirrors the confirm-gate and dedup of [CatalogLocalDataSource.ingestDeck] for the
 * renderable card list: a sense lacking a translation or example is dropped rather
 * than previewed as a broken card. The deck meta still keeps the original Service
 * card count, matching the list/subscription count users see for the source set.
 *
 * Identity caveat: a preview card's [app.sensee.feature.library.domain.CardId] is the raw
 * catalog `source_ref` ([CardDto.id]), NOT the stable, namespaced `sense_id` that an adopt
 * would mint. The preview is therefore display-only — its ids must not be used to navigate
 * into a card/lemma detail (no such navigation exists from the read-only browse screen).
 * Dedup here is by `source_ref`; the store's content-level resolve-or-mint (which could
 * collapse two source_refs onto one sense) is not reproducible without the store, so an
 * adopt may keep marginally fewer cards than the preview in the rare identical-content case.
 */
internal fun DeckDto.toPreviewDeckWithCards(): DeckWithCards {
    val seen = mutableSetOf<CardId>()
    val previewCards =
        cards
            .mapNotNull { dto ->
                val sense = dto.sense.toDomain()
                if (!sense.isConfirmable()) {
                    null
                } else {
                    SenseCatalogProjection.cardFromSense(
                        sense = sense,
                        cardId = CardId(dto.id),
                        lemmaId = SenseCatalogProjection.lemmaId(deriveLemmaKey(sense)),
                    )
                }
            }.filter { seen.add(it.id) }
    return DeckWithCards(
        deck =
            Deck(
                id = DeckId(id),
                title = title,
                description = description,
                cardCount = cards.size,
                origin = CatalogOrigin.Service,
            ),
        cards = previewCards,
    )
}

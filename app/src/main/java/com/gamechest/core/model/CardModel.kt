package com.gamechest.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardSuit {
    HEARTS, DIAMONDS, CLUBS, SPADES, SPECIAL
}

@Serializable
data class GameCard(
    val id: String,
    val name: String,
    val suit: CardSuit = CardSuit.SPECIAL,
    val value: Int = 0,
    val rank: String = "",
    val description: String = "",
    val frontAsset: String = "",
    val backAsset: String = "",
    val isFaceUp: Boolean = false,
    val ownerId: String? = null
)

@Serializable
enum class CardZoneType {
    PLAYER_HAND,
    DRAW_DECK,
    DISCARD_PILE,
    COMMUNITY_SLOT,
    SECRET_VAULT
}

@Serializable
data class CardZoneConfig(
    val id: String,
    val zoneType: CardZoneType,
    val ownerPlayerId: String? = null,
    val maxCards: Int = 10,
    val isVisibleToAll: Boolean = false,
    val isVisibleToOwnerOnly: Boolean = true,
    val x: Float = 0.5f,
    val y: Float = 0.9f,
    val width: Float = 200f,
    val height: Float = 120f
)

@Serializable
data class CardTableConfig(
    val deckCount: Int = 1,
    val initialCardsPerPlayer: Int = 5,
    val maxCardsPerHand: Int = 10,
    val zones: List<CardZoneConfig> = emptyList()
)

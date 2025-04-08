package com.focusflow.domain.model

import java.util.*

enum class TreeType {
    MAPLE,
    PINE,
    CHERRY,
    OAK,
    WILLOW,
    SAKURA
}

enum class GrowthStage {
    SEED,
    SPROUT,
    SAPLING,
    GROWING,
    MATURE,
    WITHERED
}

data class Tree(
    val id: Long = 0,
    val treeType: TreeType = TreeType.MAPLE,
    val growthStage: GrowthStage = GrowthStage.SEED,
    val plantedDate: Date = Date(),
    val completedDate: Date? = null,
    val sessionId: Long? = null
) {
    val isFullyGrown: Boolean
        get() = growthStage == GrowthStage.MATURE
        
    val isWithered: Boolean
        get() = growthStage == GrowthStage.WITHERED

    /**
     * Returns a new Tree instance with the growth stage updated based on progress
     * @param progressPercent The current progress percentage (0-100)
     */
    fun withUpdatedGrowthStage(progressPercent: Float): Tree {
        val newStage = when {
            progressPercent < 0.01f -> GrowthStage.SEED
            progressPercent < 0.25f -> GrowthStage.SPROUT
            progressPercent < 0.50f -> GrowthStage.SAPLING
            progressPercent < 0.75f -> GrowthStage.GROWING
            else -> GrowthStage.MATURE
        }
        
        return this.copy(growthStage = newStage)
    }
    
    /**
     * Returns a new Tree instance marked as withered (for abandoned sessions)
     */
    fun withered(): Tree {
        return this.copy(growthStage = GrowthStage.WITHERED)
    }
    
    /**
     * Returns a new Tree instance marked as complete
     */
    fun completed(): Tree {
        return this.copy(growthStage = GrowthStage.MATURE, completedDate = Date())
    }
}

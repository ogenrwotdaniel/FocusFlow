package com.focusflow.social

import com.focusflow.domain.model.Tree
import com.focusflow.domain.model.TreeType
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.sqrt

/**
 * Represents a shared virtual forest where users can plant trees based on their productivity.
 * The community garden serves as a visual representation of collective focus achievement.
 */
data class CommunityGarden(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val ownerId: String,
    val isPublic: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val trees: List<SharedTree> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val totalFocusMinutes: Int = 0,
    val gardenLevel: Int = 1,
    val theme: GardenTheme = GardenTheme.FOREST
) {
    /**
     * Calculate the garden's health based on recent activity
     * @return health percentage (0-100)
     */
    fun calculateHealth(): Int {
        if (trees.isEmpty()) return 100
        
        // Check how many trees were planted in the last 7 days
        val recentTrees = trees.count { 
            it.plantedAt.isAfter(LocalDateTime.now().minusDays(7))
        }
        
        // Calculate health based on recent activity
        val activityRatio = (recentTrees.toDouble() / memberIds.size.coerceAtLeast(1)).coerceAtMost(1.0)
        return (activityRatio * 100).toInt()
    }
    
    /**
     * Calculate the total area of the garden based on number of trees and garden level
     * @return area in virtual square meters
     */
    fun calculateArea(): Int {
        // Base area plus additional area for each tree
        return 100 + (50 * sqrt(trees.size.toDouble())).toInt() + ((gardenLevel - 1) * 100)
    }
    
    /**
     * Get the top contributors to the garden
     * @return list of top 5 contributors by focus minutes
     */
    fun getTopContributors(limit: Int = 5): List<GardenContributor> {
        // Group trees by user and sum their contribution minutes
        return trees.groupBy { it.contributorId }
            .map { (userId, userTrees) ->
                GardenContributor(
                    userId = userId,
                    totalMinutesContributed = userTrees.sumOf { it.contributionMinutes },
                    treeCount = userTrees.size
                )
            }
            .sortedByDescending { it.totalMinutesContributed }
            .take(limit)
    }
    
    /**
     * Check if the garden is eligible for level upgrade
     */
    fun canLevelUp(): Boolean {
        // Garden can level up when it reaches certain tree milestones
        val treesNeededForNextLevel = gardenLevel * 10
        return trees.size >= treesNeededForNextLevel
    }
    
    /**
     * Determine the next milestone for garden progression
     */
    fun getNextMilestone(): GardenMilestone {
        val treesNeededForNextLevel = gardenLevel * 10
        val treesRemaining = (treesNeededForNextLevel - trees.size).coerceAtLeast(0)
        
        return GardenMilestone(
            title = "Level ${gardenLevel + 1} Garden",
            description = "Plant $treesRemaining more trees to expand your garden",
            treesRemaining = treesRemaining,
            reward = "New ${theme.name.lowercase()} decorations and expanded garden area"
        )
    }
    
    /**
     * Add a new tree to the garden
     */
    fun addTree(tree: Tree, contributorId: String, contributionMinutes: Int): CommunityGarden {
        val sharedTree = SharedTree(
            tree = tree,
            contributorId = contributorId,
            contributionMinutes = contributionMinutes,
            plantedAt = LocalDateTime.now(),
            position = generateTreePosition()
        )
        
        return copy(
            trees = trees + sharedTree,
            totalFocusMinutes = totalFocusMinutes + contributionMinutes
        )
    }
    
    /**
     * Generate a position for a new tree
     */
    private fun generateTreePosition(): TreePosition {
        // Create a simple algorithm for tree positioning to avoid overlaps
        // This would be more sophisticated in a real implementation with proper layout
        val gardenRadius = sqrt(calculateArea() / Math.PI)
        
        // Try to find a position that's not too close to existing trees
        var attempts = 0
        var position: TreePosition
        
        do {
            // Generate a random position within the garden radius
            val angle = Math.random() * 2 * Math.PI
            val distance = Math.random() * gardenRadius
            
            position = TreePosition(
                x = (distance * Math.cos(angle)).toFloat(),
                y = (distance * Math.sin(angle)).toFloat()
            )
            
            attempts++
        } while (isPositionTooClose(position) && attempts < 10)
        
        return position
    }
    
    /**
     * Check if a position is too close to existing trees
     */
    private fun isPositionTooClose(position: TreePosition): Boolean {
        // Minimum distance between trees
        val minDistance = 5f
        
        return trees.any { tree ->
            val dx = position.x - tree.position.x
            val dy = position.y - tree.position.y
            val distance = sqrt(dx * dx + dy * dy)
            distance < minDistance
        }
    }
}

/**
 * A tree shared in the community garden
 */
data class SharedTree(
    val id: String = UUID.randomUUID().toString(),
    val tree: Tree,
    val contributorId: String,
    val contributionMinutes: Int,
    val plantedAt: LocalDateTime = LocalDateTime.now(),
    val position: TreePosition,
    val reactions: List<TreeReaction> = emptyList()
)

/**
 * A position for a tree in the 2D garden space
 */
data class TreePosition(
    val x: Float,
    val y: Float
)

/**
 * A reaction to a tree (like, heart, etc.)
 */
data class TreeReaction(
    val userId: String,
    val reactionType: ReactionType,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of reactions that can be given to trees
 */
enum class ReactionType {
    WATER,
    SUNLIGHT,
    HEART,
    STAR,
    RAINBOW
}

/**
 * A milestone for garden progression
 */
data class GardenMilestone(
    val title: String,
    val description: String,
    val treesRemaining: Int,
    val reward: String
)

/**
 * A contributor to the garden
 */
data class GardenContributor(
    val userId: String,
    val totalMinutesContributed: Int,
    val treeCount: Int
)

/**
 * Theme options for the community garden
 */
enum class GardenTheme {
    FOREST,
    TROPICAL,
    MOUNTAIN,
    DESERT,
    ALPINE,
    COASTAL,
    MEADOW,
    MYSTIC
}

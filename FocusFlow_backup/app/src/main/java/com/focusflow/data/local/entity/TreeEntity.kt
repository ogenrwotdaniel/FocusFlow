package com.focusflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusflow.domain.model.GrowthStage
import com.focusflow.domain.model.Tree
import com.focusflow.domain.model.TreeType
import java.util.*

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeType: String,
    val growthStage: String,
    val plantedDateMs: Long,
    val completedDateMs: Long? = null,
    val sessionId: Long? = null
) {
    companion object {
        fun fromDomain(tree: Tree): TreeEntity {
            return TreeEntity(
                id = tree.id,
                treeType = tree.treeType.name,
                growthStage = tree.growthStage.name,
                plantedDateMs = tree.plantedDate.time,
                completedDateMs = tree.completedDate?.time,
                sessionId = tree.sessionId
            )
        }
    }

    fun toDomain(): Tree {
        return Tree(
            id = id,
            treeType = TreeType.valueOf(treeType),
            growthStage = GrowthStage.valueOf(growthStage),
            plantedDate = Date(plantedDateMs),
            completedDate = completedDateMs?.let { Date(it) },
            sessionId = sessionId
        )
    }
}

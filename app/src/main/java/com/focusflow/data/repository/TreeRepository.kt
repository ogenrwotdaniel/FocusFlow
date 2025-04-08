package com.focusflow.data.repository

import com.focusflow.data.local.dao.TreeDao
import com.focusflow.data.local.entity.TreeEntity
import com.focusflow.domain.model.Tree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TreeRepository @Inject constructor(
    private val treeDao: TreeDao
) {
    suspend fun createTree(tree: Tree): Long {
        return treeDao.insertTree(TreeEntity.fromDomain(tree))
    }

    suspend fun updateTree(tree: Tree) {
        treeDao.updateTree(TreeEntity.fromDomain(tree))
    }

    suspend fun getTreeById(id: Long): Tree? {
        return treeDao.getTreeById(id)?.toDomain()
    }

    fun getAllTrees(): Flow<List<Tree>> {
        return treeDao.getAllTrees().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFullyGrownTrees(): Flow<List<Tree>> {
        return treeDao.getFullyGrownTrees().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getTreeForSession(sessionId: Long): Tree? {
        return treeDao.getTreeForSession(sessionId)?.toDomain()
    }

    fun getFullyGrownTreeCount(): Flow<Int> {
        return treeDao.getFullyGrownTreeCount()
    }

    fun getTreesForDay(date: Date): Flow<List<Tree>> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return treeDao.getTreesForDay(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogCategoryTreeTest {
    @Test
    fun normalizesLevelsWithoutDepthLimit() {
        val categories = listOf(
            category("root", null, 0),
            category("one", "root", 1),
            category("two", "one", 2),
            category("three", "two", 3),
        )

        val normalized = normalizeCategoryTree(
            existing = categories,
            input = CategoryInput(
                id = "four",
                parentId = "three",
                name = "Livello quattro",
                description = "",
            ),
            generatedId = "unused",
        )

        assertEquals(4, normalized.single { it.id == "four" }.level)
    }

    @Test
    fun movingCategoryRecalculatesDescendantLevels() {
        val categories = listOf(
            category("root-a", null, 0),
            category("root-b", null, 0),
            category("branch", "root-a", 1),
            category("leaf", "branch", 2),
        )

        val normalized = normalizeCategoryTree(
            existing = categories,
            input = CategoryInput(
                id = "branch",
                parentId = null,
                name = "Branch",
                description = "",
            ),
            generatedId = "unused",
        )

        assertEquals(0, normalized.single { it.id == "branch" }.level)
        assertEquals(1, normalized.single { it.id == "leaf" }.level)
    }

    @Test
    fun rejectsCycles() {
        val categories = listOf(
            category("root", null, 0),
            category("branch", "root", 1),
            category("leaf", "branch", 2),
        )

        assertThrows(IllegalArgumentException::class.java) {
            normalizeCategoryTree(
                existing = categories,
                input = CategoryInput(
                    id = "root",
                    parentId = "leaf",
                    name = "Root",
                    description = "",
                ),
                generatedId = "unused",
            )
        }
    }

    private fun category(id: String, parentId: String?, level: Int) =
        OrderCategoryEntity(
            id = id,
            parentId = parentId,
            level = level,
            name = id,
            description = null,
            active = true,
        )
}

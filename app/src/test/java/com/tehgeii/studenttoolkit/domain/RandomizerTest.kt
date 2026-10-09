package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.domain.classroom.AssignRule
import com.tehgeii.studenttoolkit.domain.classroom.GroupMode
import com.tehgeii.studenttoolkit.domain.classroom.Randomizer
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomizerTest {
    private val names = listOf("Andi", "Budi", "Citra", "Dimas", "Eka", "Fajar", "Gilang", "Hana")

    @Test
    fun parsesNamesIgnoringBlankLines() {
        assertEquals(listOf("Andi", "Budi"), Randomizer.parseNames("  Andi \n\n   \nBudi\n"))
        assertEquals(emptyList<String>(), Randomizer.parseNames(""))
    }

    @Test
    fun balancedGroupSizes() {
        assertEquals(listOf(3, 3, 2), Randomizer.groupSizes(8, 3))
        assertEquals(listOf(4, 3, 3), Randomizer.groupSizes(10, 3))
        assertEquals(listOf(1, 1), Randomizer.groupSizes(2, 5))
        assertEquals(listOf(1), Randomizer.groupSizes(1, 1))
        assertEquals(3, Randomizer.groupCount(8, GroupMode.BySize(3)))
        assertEquals(3, Randomizer.groupCount(10, GroupMode.BySize(4)))
    }

    @Test
    fun everyoneAppearsExactlyOnce() {
        val withDuplicates = names + "Andi"
        repeat(50) { seed ->
            val groups = Randomizer.makeGroups(withDuplicates, GroupMode.ByCount(4), Random(seed))
            assertEquals(withDuplicates.sorted(), groups.flatten().sorted())
            val sizes = groups.map { it.size }
            assertTrue(sizes.max() - sizes.min() <= 1)
        }
    }

    @Test
    fun largeListsStayFair() {
        val many = (1..1000).map { "Student $it" }
        val groups = Randomizer.makeGroups(many, GroupMode.BySize(7), Random(1))
        assertEquals(143, groups.size)
        assertEquals(many.toSet(), groups.flatten().toSet())
        assertEquals(1000, groups.sumOf { it.size })
    }

    @Test
    fun presentationOrderIsAPermutation() {
        val order = Randomizer.presentationOrder(names, Random(7))
        assertEquals(names.sorted(), order.sorted())
    }

    @Test
    fun taskAssignmentRules() {
        val people = listOf("Andi", "Budi", "Citra", "Dimas", "Eka")
        val tasks = listOf("Frontend", "Backend", "Docs")
        assertTrue(Randomizer.needsRule(people.size, tasks.size))

        val repeated = Randomizer.assignTasks(people, tasks, AssignRule.REPEAT_TASKS, Random(1))
        assertEquals(5, repeated.assignments.size)
        assertEquals(tasks.toSet(), repeated.assignments.flatMap { it.tasks }.toSet())

        val unassigned = Randomizer.assignTasks(people, tasks, AssignRule.LEAVE_PEOPLE_UNASSIGNED, Random(1))
        assertEquals(3, unassigned.assignments.size)
        assertEquals(2, unassigned.unassignedPeople.size)

        val multi = Randomizer.assignTasks(listOf("A", "B"), tasks, AssignRule.MULTIPLE_TASKS_PER_PERSON, Random(1))
        assertEquals(tasks.sorted(), multi.assignments.flatMap { it.tasks }.sorted())

        val equal = Randomizer.assignTasks(listOf("A", "B", "C"), tasks, null, Random(1))
        assertEquals(3, equal.assignments.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun uneven_without_rule_is_rejected() {
        Randomizer.assignTasks(listOf("A", "B"), listOf("x"), null, Random(1))
    }
}

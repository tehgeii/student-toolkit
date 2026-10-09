package com.tehgeii.studenttoolkit.domain.classroom

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

sealed interface GroupMode {
    data class ByCount(val groups: Int) : GroupMode
    data class BySize(val perGroup: Int) : GroupMode
}

/** What to do when the number of people and tasks differ. Chosen by the user, never guessed. */
enum class AssignRule(val label: String) {
    REPEAT_TASKS("Allow repeated tasks"),
    LEAVE_PEOPLE_UNASSIGNED("Leave some people unassigned"),
    MULTIPLE_TASKS_PER_PERSON("Give some people more than one task"),
    LEAVE_TASKS_UNASSIGNED("Leave some tasks unassigned"),
}

data class Assignment(val person: String, val tasks: List<String>)

data class AssignmentResult(
    val assignments: List<Assignment>,
    val unassignedPeople: List<String>,
    val unassignedTasks: List<String>,
)

/**
 * Fair randomization helpers. Every valid participant is kept exactly once (duplicate names are
 * treated as different people), groups are balanced, and the random source is SecureRandom.
 */
object Randomizer {
    val defaultRandom: Random get() = SecureRandom().asKotlinRandom()

    /** One name per line; surrounding whitespace and empty lines are ignored. */
    fun parseNames(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun duplicateNames(names: List<String>): Set<String> =
        names.groupBy { it.lowercase() }.filter { it.value.size > 1 }.map { it.value.first() }.toSet()

    /** Balanced sizes: 10 people in 3 groups -> [4, 3, 3]. */
    fun groupSizes(people: Int, groups: Int): List<Int> {
        if (people <= 0 || groups <= 0) return emptyList()
        val g = groups.coerceAtMost(people)
        val base = people / g
        val extra = people % g
        return List(g) { index -> if (index < extra) base + 1 else base }
    }

    fun groupCount(people: Int, mode: GroupMode): Int = when (mode) {
        is GroupMode.ByCount -> mode.groups.coerceAtMost(people)
        is GroupMode.BySize -> if (mode.perGroup <= 0) 0 else (people + mode.perGroup - 1) / mode.perGroup
    }

    fun makeGroups(names: List<String>, mode: GroupMode, random: Random = defaultRandom): List<List<String>> {
        val count = groupCount(names.size, mode)
        if (count <= 0) return emptyList()
        val shuffled = names.shuffled(random)
        val result = mutableListOf<List<String>>()
        var start = 0
        for (size in groupSizes(names.size, count)) {
            result += shuffled.subList(start, start + size)
            start += size
        }
        return result
    }

    fun presentationOrder(names: List<String>, random: Random = defaultRandom): List<String> = names.shuffled(random)

    /** True when the user must pick an [AssignRule] before assigning. */
    fun needsRule(people: Int, tasks: Int): Boolean = people > 0 && tasks > 0 && people != tasks

    fun rulesFor(people: Int, tasks: Int): List<AssignRule> = when {
        people > tasks -> listOf(AssignRule.REPEAT_TASKS, AssignRule.LEAVE_PEOPLE_UNASSIGNED)
        tasks > people -> listOf(AssignRule.MULTIPLE_TASKS_PER_PERSON, AssignRule.LEAVE_TASKS_UNASSIGNED)
        else -> emptyList()
    }

    fun assignTasks(
        people: List<String>,
        tasks: List<String>,
        rule: AssignRule?,
        random: Random = defaultRandom,
    ): AssignmentResult {
        require(people.isNotEmpty() && tasks.isNotEmpty()) { "Add people and tasks first." }
        val p = people.shuffled(random)
        val t = tasks.shuffled(random)
        if (p.size == t.size) {
            return AssignmentResult(p.zip(t).map { Assignment(it.first, listOf(it.second)) }, emptyList(), emptyList())
        }
        requireNotNull(rule) { "Choose how to handle different numbers of people and tasks." }
        require(rule in rulesFor(p.size, t.size)) { "That rule does not apply here." }
        return when (rule) {
            AssignRule.REPEAT_TASKS -> {
                // Cycle through the shuffled tasks so repeats are spread evenly.
                val list = p.mapIndexed { i, person -> Assignment(person, listOf(t[i % t.size])) }
                AssignmentResult(list, emptyList(), emptyList())
            }
            AssignRule.LEAVE_PEOPLE_UNASSIGNED -> AssignmentResult(
                p.take(t.size).zip(t).map { Assignment(it.first, listOf(it.second)) },
                p.drop(t.size),
                emptyList(),
            )
            AssignRule.MULTIPLE_TASKS_PER_PERSON -> {
                val buckets = List(p.size) { mutableListOf<String>() }
                t.forEachIndexed { i, task -> buckets[i % p.size] += task }
                AssignmentResult(p.mapIndexed { i, person -> Assignment(person, buckets[i]) }, emptyList(), emptyList())
            }
            AssignRule.LEAVE_TASKS_UNASSIGNED -> AssignmentResult(
                p.zip(t.take(p.size)).map { Assignment(it.first, listOf(it.second)) },
                emptyList(),
                t.drop(p.size),
            )
        }
    }
}

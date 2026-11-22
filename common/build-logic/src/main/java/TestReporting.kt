//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

import org.gradle.api.GradleException

// -- Test reporting ------------------------------------------------------------
//
// Shared console reporting for the Android (`printTestSummary`) and iOS
// (`printTestSummaryiOS`) test summary tasks. Reading results stays in each
// platform build script; the platforms convert what they read into
// [SuiteResult] / [CoverageRow] and hand it to the functions below.
//
// Top-level declarations here are on the classpath of every build script that
// applies id("base-conventions"), so they can be called without imports.

private const val TABLE_WIDTH = 80
private const val COVERAGE_NAME_MIN_WIDTH = 15
private const val COVERAGE_NAME_MAX_WIDTH = 46

/** Pass/fail counts for one test suite (test class). */
data class SuiteResult(
    val name: String,
    val tests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
)

/**
 * One row of the CODE COVERAGE table. Android fills one row per JaCoCo counter
 * (Instructions, Branches, ...); iOS fills one row per build target (lines).
 */
data class CoverageRow(
    val name: String,
    val covered: Long,
    val total: Long,
) {
    val missed: Long = total - covered
    val pct: Double = if (total == 0L) 100.0 else covered * 100.0 / total
}

/**
 * Prints the TEST RESULTS table of per-suite pass/fail counts. When [suites] is
 * empty, prints the [emptyNotice] lines instead of the table.
 *
 * Returns `true` if any suite has failing tests, so the caller can fail the build
 * after the full report has been displayed (see [failBuildIfTestsFailed]).
 */
fun printTestResultsTable(
    suites: List<SuiteResult>,
    emptyNotice: List<String>,
): Boolean {
    val bar = "=".repeat(TABLE_WIDTH)
    val sep = "-".repeat(TABLE_WIDTH)

    println()
    println(bar)
    println(" TEST RESULTS")
    println(bar)

    if (suites.isEmpty()) {
        emptyNotice.forEach { println(" $it") }
        println(bar)
        println()
        return false
    }

    val nameW = maxOf(suites.maxOf { it.name.length }, "Suite".length)

    println(
        " %-${nameW}s   %5s   %6s   %6s   %6s"
            .format("Suite", "Tests", "Passed", "Failed", "Pass %"),
    )
    println(sep)

    suites.forEach { s ->
        val pct = if (s.tests > 0) s.passed * 100.0 / s.tests else 100.0
        println(
            " %-${nameW}s   %5d   %6d   %6d   %5.1f%%"
                .format(s.name, s.tests, s.passed, s.failed, pct),
        )
    }

    val totalTests = suites.sumOf { it.tests }
    val totalPassed = suites.sumOf { it.passed }
    val totalFailed = suites.sumOf { it.failed }
    val totalPct = if (totalTests > 0) totalPassed * 100.0 / totalTests else 100.0

    println(sep)
    println(
        " %-${nameW}s   %5d   %6d   %6d   %5.1f%%"
            .format("TOTAL", totalTests, totalPassed, totalFailed, totalPct),
    )
    println(bar)
    println()

    return totalFailed > 0
}

/**
 * Throws a [GradleException] if any suite in [suites] has failing tests.
 * [dash] is the punctuation used in the message (each platform keeps its
 * existing wording).
 */
fun failBuildIfTestsFailed(
    suites: List<SuiteResult>,
    dash: String = "-",
) {
    val failed = suites.sumOf { it.failed }
    if (failed > 0) {
        throw GradleException("$failed test(s) failed $dash see the TEST RESULTS table above.")
    }
}

/**
 * Prints the CODE COVERAGE table. Each platform supplies its own rows and the
 * header of the first column ([nameHeader], e.g. "Metric" or "Target").
 *
 * [headerNotes] are printed under the title (e.g. the HTML report location).
 * When [rows] is empty, the [emptyNotice] lines are printed instead of the table.
 */
fun printCoverageTable(
    nameHeader: String,
    rows: List<CoverageRow>,
    emptyNotice: List<String>,
    headerNotes: List<String> = emptyList(),
) {
    val bar = "=".repeat(TABLE_WIDTH)
    val sep = "-".repeat(TABLE_WIDTH)

    println(bar)
    println(" CODE COVERAGE")
    headerNotes.forEach { println(" $it") }
    println(bar)

    if (rows.isEmpty()) {
        emptyNotice.forEach { println(" $it") }
        println(bar)
        println()
        return
    }

    val nameW =
        maxOf(COVERAGE_NAME_MIN_WIDTH, nameHeader.length, rows.maxOf { it.name.length })
            .coerceAtMost(COVERAGE_NAME_MAX_WIDTH)

    println(
        " %-${nameW}s   %7s   %7s   %7s   %8s"
            .format(nameHeader, "Covered", "Missed", "Total", "Coverage"),
    )
    println(sep)

    rows.forEach { r ->
        println(
            " %-${nameW}s   %7d   %7d   %7d   %7.1f%%"
                .format(r.name.take(nameW), r.covered, r.missed, r.total, r.pct),
        )
    }

    println(bar)
    println()
}

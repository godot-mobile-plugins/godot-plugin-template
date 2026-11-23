//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// -- JUnit XML results ---------------------------------------------------------
//
// Reads JUnit-format XML result files (one <testsuite> root element per file)
// into [SuiteResult]s for the shared TEST RESULTS table in TestReporting.kt.
// Used by the Android unit tests (:android:printTestSummary) and the headless
// Godot integration tests (:addon:runIntegrationTests).

/**
 * Returns a DocumentBuilder that will not attempt any network or filesystem
 * access to resolve external DTD or entity declarations. JaCoCo's XML report
 * contains a DOCTYPE declaration that references "report.dtd"; suppressing
 * external entity loading prevents IO errors when parsing offline.
 */
fun safeXmlBuilder(): javax.xml.parsers.DocumentBuilder {
    val factory = DocumentBuilderFactory.newInstance()
    try {
        factory.isValidating = false
        factory.isNamespaceAware = false
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    } catch (_: Exception) {
        // Swallow: some parser implementations don't expose these features.
    }
    return factory.newDocumentBuilder()
}

/**
 * Parses every JUnit-format XML file in [resultsDir] and returns one
 * [SuiteResult] per file, sorted alphabetically by the simple class name.
 * Malformed or unreadable files are silently skipped.
 */
fun parseSuiteResults(resultsDir: File): List<SuiteResult> {
    if (!resultsDir.exists()) return emptyList()
    val builder = safeXmlBuilder()
    return (resultsDir.listFiles { f -> f.extension == "xml" } ?: emptyArray())
        .mapNotNull { file ->
            runCatching {
                val root = builder.parse(file).documentElement
                // The "name" attribute is the fully-qualified class name; strip the package.
                val name = root.getAttribute("name").substringAfterLast('.')
                val tests = root.getAttribute("tests").toIntOrNull() ?: 0
                val failures = root.getAttribute("failures").toIntOrNull() ?: 0
                val errors = root.getAttribute("errors").toIntOrNull() ?: 0
                val skipped = root.getAttribute("skipped").toIntOrNull() ?: 0
                val failed = failures + errors
                SuiteResult(name, tests, maxOf(0, tests - failed - skipped), failed, skipped)
            }.getOrNull()
        }.sortedBy { it.name }
}

package org.dynamisui.demo.localization;

import org.dynamislocalization.api.value.LocaleDescriptor;
import org.dynamislocalization.api.value.LocaleKey;
import org.dynamislocalization.api.value.MissingKeyBehavior;
import org.dynamislocalization.core.JsonStringTableLoader;
import org.dynamislocalization.format.ParameterSubstitutor;
import org.dynamislocalization.runtime.LocalizationRuntime;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Terminal demo for DynamisLocalization.
 *
 * Demonstrates:
 * - Runtime locale switching
 * - String lookup with RETURN_KEY fallback
 * - Plural forms (English, French, Russian CLDR rules)
 * - Parameter substitution ({name}, {amount, currency}, {score, number})
 * - Locale change listener notification
 *
 * Run: cd demo && mvn exec:java -pl demo-localization
 */
public final class LocalizationDemo {

    private static final String NS = "dynamis.demo";
    private static final String SEP = "─".repeat(56);

    public static void main(String[] args) throws Exception {
        Path resourcePath = resolveResourcePath();

        LocalizationRuntime runtime = LocalizationRuntime.builder()
            .initialLocale(LocaleDescriptor.EN_US)
            .supportedLocales(List.of(
                LocaleDescriptor.EN_US,
                LocaleDescriptor.FR_FR,
                LocaleDescriptor.RU_RU))
            .loader(new JsonStringTableLoader(resourcePath))
            .namespace(NS)
            .missingKeyBehavior(MissingKeyBehavior.RETURN_KEY)
            .build();

        // Register locale change listener
        runtime.service().addLocaleChangeListener((prev, curr) ->
            System.out.printf("%n  *** Locale changed: %s → %s ***%n%n",
                prev.bcp47Tag(), curr.bcp47Tag()));

        ParameterSubstitutor sub = runtime.substitutor();

        // Demo all three locales
        for (LocaleDescriptor locale : List.of(
                LocaleDescriptor.EN_US,
                LocaleDescriptor.FR_FR,
                LocaleDescriptor.RU_RU)) {

            runtime.switchLocale(locale);
            var svc = runtime.service();

            System.out.println(SEP);
            System.out.println("  " + svc.get(LocaleKey.of(NS + ":demo.title")));
            System.out.println("  " + svc.get(LocaleKey.of(NS + ":demo.locale.label")));
            System.out.println(SEP);

            // Simple parameter substitution
            String greeting = sub.substitute(
                svc.get(LocaleKey.of(NS + ":demo.greeting")),
                Map.of("name", "Aria", "place", "Ashford"));
            System.out.println("  " + greeting);

            // Currency formatting
            String balance = sub.substitute(
                svc.get(LocaleKey.of(NS + ":demo.balance")),
                Map.of("amount", 1_234.50));
            System.out.println("  " + balance);

            // Number formatting
            String score = sub.substitute(
                svc.get(LocaleKey.of(NS + ":demo.score")),
                Map.of("score", 9_876_543));
            System.out.println("  " + score);

            // Plural forms — items
            System.out.println();
            for (long count : List.of(0L, 1L, 2L, 5L, 11L, 21L)) {
                String plural = sub.substitute(
                    svc.getPlural(LocaleKey.of(NS + ":demo.items"), count),
                    Map.of("count", count));
                System.out.println("  [" + count + "] " + plural);
            }

            // Plural forms — enemies
            System.out.println();
            for (long count : List.of(1L, 2L, 5L, 11L, 21L)) {
                String plural = sub.substitute(
                    svc.getPlural(LocaleKey.of(NS + ":demo.enemies"), count),
                    Map.of("count", count));
                System.out.println("  [" + count + "] " + plural);
            }

            System.out.println();
        }

        System.out.println(SEP);
        System.out.println("  Demo complete.");
        System.out.println(SEP);
    }

    private static Path resolveResourcePath() {
        try {
            var url = LocalizationDemo.class.getClassLoader()
                .getResource("localization");
            if (url == null) {
                throw new IllegalStateException(
                    "localization/ resource directory not found on classpath");
            }
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve resource path", e);
        }
    }
}

package org.dynamisengine.ui.demo.ai;

import org.dynamisengine.ai.cognition.BeliefModel;
import org.dynamisengine.ai.cognition.DefaultCognitionService;
import org.dynamisengine.core.entity.EntityId;
import org.dynamisengine.ai.social.DefaultSocialSystem;
import org.dynamisengine.ai.social.ReputationEvent;
import org.dynamisengine.ai.social.ReputationEventType;
import org.dynamisengine.ai.social.Rumor;
import org.dynamisengine.ai.social.RumorPropagator;
import org.dynamisengine.ai.social.RumorQueue;
import org.dynamisengine.ai.social.SocialGraph;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Headless simulation demo for DynamisAI (v2, strongly typed to installed API).
 */
public final class AiSimDemo {

    private static final String SEP = "─".repeat(72);

    public static void main(String[] args) {
        System.out.println(SEP);
        System.out.println("  DynamisAI Headless Demo v2 — Beliefs + Rumor Propagation");
        System.out.println("  " + Instant.now());
        System.out.println(SEP);

        EntityId aria = EntityId.of(1L);
        EntityId dorian = EntityId.of(2L);
        EntityId lyra = EntityId.of(3L);

        section("1) Cognition: BeliefModel population");
        DefaultCognitionService cognition = new DefaultCognitionService(
            (org.dynamisengine.ai.cognition.InferenceBackend) new NoopInferenceBackend().asInterface()
        );

        BeliefModel ariaBeliefs = cognition.beliefsFor(aria);
        ariaBeliefs.assertBelief("threat.seen", true, 0.95f, 1L);
        ariaBeliefs.assertBelief("ally.dorian.location", "market_square", 0.80f, 1L);
        ariaBeliefs.assertBelief("mood", "wary", 0.70f, 1L);

        System.out.println("  Aria beliefCount() = " + ariaBeliefs.beliefCount());
        printBeliefs(ariaBeliefs);

        section("2) Social: graph + rumor queues");
        DefaultSocialSystem social = new DefaultSocialSystem();
        social.adjustTrust(aria, dorian, 0.80f);
        social.adjustTrust(dorian, lyra, 0.70f);

        RumorPropagator propagator = social.rumorPropagator();
        RumorQueue ariaQueue = new RumorQueue();
        RumorQueue dorianQueue = new RumorQueue();
        RumorQueue lyraQueue = new RumorQueue();

        propagator.registerQueue(aria, ariaQueue);
        propagator.registerQueue(dorian, dorianQueue);
        propagator.registerQueue(lyra, lyraQueue);

        System.out.println("  Queues registered. pendingRumorCount() = " + propagator.pendingRumorCount());

        section("3) Rumor: seed + propagate");
        ReputationEvent reputationEvent = new ReputationEvent(
            aria,
            dorian,
            ReputationEventType.HARMED,
            0.90f,
            60L,
            true,
            aria
        );

        Rumor seeded = propagator.seedRumor(reputationEvent);
        System.out.println("  Seeded rumor: " + safeToString(seeded));

        propagator.post(aria, seeded);
        System.out.println("  Posted rumor to Aria. ariaQueue.size()=" + ariaQueue.size());

        SocialGraph graph = social.graph();
        long tick = 60L;
        propagator.propagate(graph, tick);
        System.out.println("  propagate(graph, " + tick + ") complete.");
        System.out.println("  pendingRumorCount() = " + propagator.pendingRumorCount());

        section("4) Results: queue drains");
        drainQueue("Aria", ariaQueue);
        drainQueue("Dorian", dorianQueue);
        drainQueue("Lyra", lyraQueue);

        section("5) Optional: belief decay tick");
        ariaBeliefs.decay(120L);
        System.out.println("  Aria beliefCount() after decay(120) = " + ariaBeliefs.beliefCount());

        cognition.shutdown();

        System.out.println();
        System.out.println(SEP);
        System.out.println("  ✓ Demo complete (typed to installed API signatures).");
        System.out.println(SEP);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(SEP);
        System.out.println("  " + title);
        System.out.println(SEP);
    }

    private static void printBeliefs(BeliefModel model) {
        List<?> beliefs = model.allBeliefs();
        if (beliefs == null || beliefs.isEmpty()) {
            System.out.println("  (no beliefs)");
            return;
        }
        beliefs.stream()
            .map(Objects::toString)
            .sorted()
            .forEach(b -> System.out.println("  " + b));
    }

    private static void drainQueue(String name, RumorQueue q) {
        var drained = q.drain();
        int n = drained != null ? drained.size() : 0;
        System.out.println("  " + name + " drained " + n + " rumor(s)");
        if (drained != null) {
            drained.stream()
                .map(AiSimDemo::safeToString)
                .sorted(Comparator.naturalOrder())
                .forEach(r -> System.out.println("    - " + r));
        }
    }

    private static String safeToString(Object o) {
        try {
            return String.valueOf(o);
        } catch (Throwable t) {
            return "<toString failed: " + t.getClass().getSimpleName() + ">";
        }
    }
}

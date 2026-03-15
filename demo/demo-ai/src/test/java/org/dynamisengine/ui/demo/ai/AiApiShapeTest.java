package org.dynamisengine.ui.demo.ai;

import org.dynamisengine.ai.cognition.DefaultCognitionService;
import org.dynamisengine.ai.cognition.InferenceBackend;
import org.dynamisengine.ai.social.ReputationEvent;
import org.dynamisengine.ai.social.ReputationEventType;
import org.dynamisengine.core.entity.EntityId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

final class AiApiShapeTest {

    @Test
    void defaultCognitionService_exposes_minimum_supported_constructor() throws Exception {
        // Canonical baseline: demo-ai relies on being able to construct with an InferenceBackend.
        Constructor<DefaultCognitionService> ctor =
            DefaultCognitionService.class.getConstructor(InferenceBackend.class);
        assertNotNull(ctor, "Expected DefaultCognitionService(InferenceBackend) constructor");
    }

    @Test
    void reputationEvent_exposes_expected_constructor_shape() throws Exception {
        // Your proven working ctor shape:
        // ReputationEvent(EntityId, EntityId, ReputationEventType, float, long, boolean, EntityId)
        Constructor<ReputationEvent> ctor =
            ReputationEvent.class.getConstructor(
                EntityId.class,
                EntityId.class,
                ReputationEventType.class,
                float.class,
                long.class,
                boolean.class,
                EntityId.class
            );
        assertNotNull(ctor, "Expected ReputationEvent ctor with (EntityId, EntityId, Type, float, long, boolean, EntityId)");
    }

    @Test
    void reputationEventType_contains_harmed_enum_value() {
        // Canary for enum drift: your demo uses HARMED.
        assertDoesNotThrow(() -> ReputationEventType.valueOf("HARMED"),
            "Expected ReputationEventType.HARMED to exist");
    }
}

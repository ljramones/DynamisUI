package org.dynamisui.demo.ai;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates an InferenceBackend instance without depending on its method signatures.
 *
 * DefaultCognitionService requires an InferenceBackend, but this demo doesn’t
 * call requestDialogue()/inferDeterministic(), so the backend should never be invoked.
 *
 * If it *is* invoked, this throws to make the failure obvious.
 */
final class NoopInferenceBackend implements InvocationHandler {

    private final Object proxy;

    NoopInferenceBackend() {
        try {
            Class<?> iface = Class.forName("org.dynamisai.cognition.InferenceBackend");
            this.proxy = Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                this
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "org.dynamisai.cognition.InferenceBackend not found on classpath", e);
        }
    }

    @Override
    public Object invoke(Object p, Method method, Object[] args) {
        throw new UnsupportedOperationException(
            "InferenceBackend was invoked in the headless demo. " +
            "This demo only exercises beliefs/social/rumors. Method: " + method);
    }

    @Override
    public String toString() {
        return "NoopInferenceBackend(proxy=" + proxy.getClass().getName() + ")";
    }

    Object asInterface() {
        return proxy;
    }
}

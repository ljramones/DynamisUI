module org.dynamisengine.ui.debug {
    requires org.dynamisengine.core;
    requires org.dynamisengine.ui.api;
    requires org.dynamisengine.ui.core;
    requires org.dynamisengine.debug.api;
    requires org.dynamisengine.debug.core;

    exports org.dynamisengine.ui.debug;
    exports org.dynamisengine.ui.debug.model;
    exports org.dynamisengine.ui.debug.builder;
    exports org.dynamisengine.ui.debug.render;
    exports org.dynamisengine.ui.debug.runtime;
}

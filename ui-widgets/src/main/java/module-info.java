module org.dynamisengine.ui.widgets {
    requires org.dynamisengine.core;
    requires dynamis.event;
    requires org.dynamisengine.ui.api;
    requires org.dynamisengine.ui.core;
    requires org.dynamisengine.localization.api;

    exports org.dynamisengine.ui.widgets;
    exports org.dynamisengine.ui.widgets.hud;
    exports org.dynamisengine.ui.widgets.dialogue;
    exports org.dynamisengine.ui.widgets.inventory;
    exports org.dynamisengine.ui.widgets.menu;
}

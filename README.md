# DynamisUI

Renderer-agnostic retained-mode UI framework with immediate-mode debug overlays
for the Dynamis ecosystem.

DynamisUI owns:

* In-game HUD (health, stamina, minimap)
* Dialogue / conversation UI
* Inventory, shop, menus
* Debug / developer overlays (AI inspector, performance)
* Locale-aware text rendering

It is explicitly **decoupled from any rendering backend**.
Rendering is provided via the `UIRenderer` SPI and implemented externally (e.g., JavaFX, LWJGL, Vulkan).

---

# Design Goals

* **Renderer Agnostic** — no rendering libraries inside core modules
* **Retained Scene Graph** — predictable layout and composition
* **Immediate Debug Layer** — zero-cost when disabled
* **Locale-Aware** — integrates with DynamisLocalization
* **Deterministic** — no reflection, no annotation scanning, no runtime magic
* **Modular** — strict dependency direction

---

# Module Structure

```
ui-api        → pure contracts (SPI + value types)
ui-core       → scene graph, layout, event routing
ui-widgets    → production UI components
ui-debug      → immediate-mode debug overlays
ui-runtime    → ecosystem wiring + game-loop integration
```

Dependency flow:

```
ui-api
   ↓
ui-core
   ↓        ↓
ui-widgets  ui-debug
      ↓
   ui-runtime
```

No module depends upward.

---

# Core Concepts

## UIRenderer (SPI)

Rendering backend abstraction.

```java
void beginFrame(float width, float height);
void drawRect(Bounds bounds, Color color);
float drawText(String text, float x, float y, FontDescriptor font, Color color);
void pushClip(Bounds bounds);
void popClip();
void endFrame();
```

Concrete implementations live outside this repo.

Example implementation: `demo-ui` → `JavaFxUIRenderer`.

---

## UINode

Base retained-mode scene graph node.

Responsibilities:

* Holds children
* Manages layout
* Applies padding
* Handles event propagation
* Clips to bounds

Subclass and override:

```java
protected void renderSelf(UIRenderer renderer)
```

Children render automatically after `renderSelf`.

---

## UIStage

Root of the scene graph.

Game loop integration:

```java
stage.update(tick);
renderer.beginFrame(w, h);
stage.render(renderer);
renderer.endFrame();
```

Supports:

* Layer stack (bottom → top)
* Event dispatch (top → bottom)
* Locale rebind
* Resize handling

---

## DebugOverlay

Immediate-mode overlay drawn after scene graph.

* Zero cost when disabled
* Frame-local panel registration
* Performance + AI inspector panels included

---

## Localization Integration

Widgets hold `LocaleKey` — not raw strings.

When locale changes:

```java
LocaleChangedEvent → UIStage.rebind()
```

All `LocalizedNode` instances re-resolve text automatically.

---

# Building

## Build core modules

```bash
cd DynamisCore && mvn install
cd DynamisEvent && mvn install
cd DynamisLocalization && mvn install
cd DynamisUI && mvn install
```

---

# Demo Modules

Located in:

```
DynamisUI/demo/
```

These are standalone Maven modules, not part of the main reactor.

---

## demo-localization

Terminal demo:

* Runtime locale switching (EN / FR / RU)
* CLDR plural rules
* Currency + number formatting

Run:

```bash
cd demo
mvn exec:java -pl demo-localization
```

---

## demo-ai

Headless simulation demo:

* BeliefModel population
* Social trust graph
* Rumor seeding + propagation
* API shape canary tests

Run:

```bash
cd demo
mvn exec:java -pl demo-ai
```

Run tests:

```bash
mvn test -pl demo-ai
```

---

## demo-ui (JavaFX)

Renderer bridge demo:

* HUD (health + stamina bars)
* Dialogue panel
* Debug overlay (AI inspector + performance)
* Locale switching (L key)

Run (local machine, not sandbox):

```bash
cd demo
mvn exec:java -pl demo-ui
```

If you hit cache or pipeline issues:

```bash
mvn exec:java -pl demo-ui \
  -Dexec.jvmArgs="-Djavafx.cachedir=/tmp/openjfx-cache -Dprism.order=es2,sw"
```

JavaFX platform classifier is mapped via Maven OS/arch profiles:

| Platform            | Classifier    |
| ------------------- | ------------- |
| macOS Apple Silicon | `mac-aarch64` |
| macOS Intel         | `mac-x64`     |
| Windows             | `win`         |
| Linux               | `linux`       |

---

# Testing

Current test coverage:

| Module     | Tests          |
| ---------- | -------------- |
| ui-api     | 14             |
| ui-core    | 15             |
| ui-widgets | 14             |
| ui-debug   | 14             |
| ui-runtime | 9              |
| demo-ai    | 3 (API canary) |
| **Total**  | **69+**        |

Run full suite:

```bash
mvn test
```

---

# Conventions

* groupId: `org.dynamisengine.ui`
* Package root: `org.dynamisengine.ui.*`
* Logging: `DynamisLogger` only
* Exceptions: `DynamisException`
* Java 25 (preview enabled)
* No reflection in core modules
* No rendering library imports outside demo modules

---

# Why Renderer-Agnostic?

UI libraries that bind directly to a graphics API eventually become constraints.

DynamisUI avoids:

* JavaFX scene graph ownership conflicts
* ImGui production UI limitations
* LWJGL hard binding
* Threading collisions with game loops

The UI declares intent.
The engine controls rendering.

---

# Current Status

**Version:** `1.0.0-SNAPSHOT`
**State:** Stable foundation
**All modules compile and test cleanly**


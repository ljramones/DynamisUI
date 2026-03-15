# DynamisUI — Agent Guidelines

## What This Repo Is
Renderer-agnostic retained-mode UI framework with immediate-mode debug overlays
for the Dynamis ecosystem. Owns HUD, dialogue, inventory, menus, and dev overlays.
UIRenderer is a SPI — concrete implementations live in the game, not here.

## Module Structure
| Module | Purpose |
|---|---|
| `ui-api` | Pure contracts — UIRenderer SPI, UIComponent, UIEvent, value types. |
| `ui-core` | Scene graph, layout engine, event routing, animation. |
| `ui-widgets` | Production UI — HUD, dialogue, inventory, shop, menus, settings. |
| `ui-debug` | Immediate-mode debug overlays — AI inspector, LOD, performance. |
| `ui-runtime` | Assembly, game-loop integration, ecosystem wiring. |

## Dependency Order
```
ui-api → ui-core → ui-widgets / ui-debug → ui-runtime
```

## Conventions (must match DynamisCore baseline)
- groupId: `org.dynamisengine.ui`
- Package root: `org.dynamisengine.ui.*`
- Logging: `DynamisLogger` only — never SLF4J
- Exceptions: root in `DynamisException`
- `module-info.java` required in every module
- JUnit 5.11.4, SpotBugs 4.9.8.2, maven-compiler-plugin 3.14.0, maven-surefire-plugin 3.5.2

## Critical Boundary Rules
- `ui-api` — depends on `dynamis-core` only. No rendering library imports ever.
- `ui-core` — depends on `ui-api` and `dynamis-core` only.
- `ui-widgets` — depends on `ui-core`, `localization-api`, `dynamis-event`. No rendering library imports.
- `ui-debug` — depends on `ui-core` only. No DynamisAI imports (data passed in, not pulled).
- `ui-runtime` — only module that wires ecosystem dependencies together.
- UIRenderer implementations NEVER live in this repo.

## Build Commands
```bash
cd DynamisCore && mvn install && cd ..
cd DynamisEvent && mvn install && cd ..
cd DynamisLocalization && mvn install && cd ..
cd DynamisUI && mvn install
```

# DynamisUI Architecture Review

Date: 2026-03-10  
Scope: Deep boundary ratification for `DynamisUI` (review/documentation only)

## 1. Repo Overview

Observed modules:

- `ui-api`
- `ui-core`
- `ui-widgets`
- `ui-debug`
- `ui-runtime`
- `demo/*` modules (standalone demos, not in main reactor)

Observed implementation shape:

- `ui-api` defines UI contracts, UI event types, layout/value models, and renderer SPI (`UIRenderer`).
- `ui-core` provides retained-mode scene graph (`UINode`, `UIStage`), layout implementations, and local UI event dispatch.
- `ui-widgets` provides production presentation components (HUD, labels, dialogue panel, menu/inventory placeholders).
- `ui-debug` provides immediate-mode debug overlay panels (`DebugOverlay`, `PerformanceOverlay`, `AIInspectorOverlay`) without hard dependency on AI implementation modules.
- `ui-runtime` assembles runtime wiring (`UIRuntime`) and integrates with localization + event bus for locale-change rebind.

Dependency signals from poms/module-info:

- Core dependency base: `DynamisCore`.
- Runtime integration dependency: `DynamisEvent` and `DynamisLocalization` (`localization-api`, `localization-runtime`).
- No direct compile dependency on `DynamisLightEngine`, `DynamisGPU`, `DynamisWorldEngine`, `DynamisSession`, `DynamisContent`, `DynamisScripting`, or `DynamisInput` in main UI modules.
- Rendering backend is externalized behind `UIRenderer`; concrete backend lives in demo (`JavaFxUIRenderer`).

## 2. Strict Ownership Statement

### What DynamisUI should own

- Presentation-layer composition: widgets, scene graph nodes, layout behavior, local UI event propagation.
- UI runtime orchestration at presentation scope (frame begin/update/render sequencing for UI tree).
- UI-only state and interactions (visible/hidden, selected choice, debug panel composition, local animations).
- Localization consumption for displayed text.
- Renderer abstraction contract for drawing UI primitives.

### What is appropriate for a UI subsystem

- Retained-mode node hierarchy and widget composition.
- Input-to-UI event handling **after** input is translated into UI events.
- UI-bound view/presenter facades over external authorities.
- Debug overlays as presentation instruments (not authority owners).

### What DynamisUI must never own

- World authority and simulation tick authority.
- Scripting runtime control/orchestration authority.
- Session persistence authority.
- Runtime content authority.
- Localization authority (it may consume localization services, not define locale catalogs/policy).
- Render planning ownership and GPU execution ownership.

## 3. Dependency Rules

### Allowed dependencies for DynamisUI

- `DynamisCore` for shared primitives/logging/errors.
- `DynamisEvent` for cross-system event transport where needed (for example locale change notifications).
- `DynamisLocalization` as a consumed service for text lookup/locale changes.
- External renderer adapters through SPI implementations outside core UI modules.

### Forbidden dependencies for DynamisUI

- Direct dependence on render execution/planning internals (`DynamisGPU`, `DynamisLightEngine` internals).
- Direct dependence on world/session/content authority implementations.
- Direct dependence on scripting engine internals for control flow/policy.

### Who may depend on DynamisUI

- Host/runtime layers that need UI presentation.
- Feature systems that provide UI data models to widgets/presenters.

### Boundary requirements

- World/session/content/scripting/localization should be consumed through narrow presentation-facing contracts or event/view-model adapters.
- UI must not become a control-plane policy orchestrator.

## 4. Public vs Internal Boundary

### Canonical public surface (recommended)

- `ui-api` contracts should be the stable public surface.
- `ui-runtime.UIRuntime` is a reasonable top-level runtime facade.
- `ui-widgets` exported types are public presentation components.

### Internal/implementation surface (should remain internal where possible)

- Concrete scene-graph/runtime internals (`UINode` traversal details, layout internals, debug panel implementation details).
- Demo modules and renderer bridge (`demo/*`, `JavaFxUIRenderer`) should not be treated as engine API.

### Boundary concern

- `ui-core`, `ui-widgets`, and `ui-debug` export concrete implementation packages; this can freeze implementation details as external API sooner than needed.
- Some widgets currently mix interaction handling and data semantics (for example `DialoguePanel` directly dispatches/handles choice clicks) without explicit presenter/view-model seam.

## 5. Policy Leakage / Overlap Findings

## Major clean boundaries confirmed

- Renderer-agnostic design is real: no direct render backend imports in main modules.
- Explicit statement in `UIEvent` that UI events are internal and separate from `DynamisEvent` transport semantics is clean.
- Localization is consumed, not owned (`Label` + `LocalizationService`; locale change rebind via `LocaleChangedEvent`).
- Debug overlays are presentation-level and intentionally avoid direct AI module dependency.

## Policy leakage / overlap identified

- **DynamisInput overlap risk (watch):** UI handles UI events, but explicit boundary for input translation (raw input -> `UIEvent`) is not formalized in this repo. Keep raw input authority in `DynamisInput`.
- **DynamisScripting overlap risk (low-to-moderate, mainly demos):** demo modules touch AI/Scripting-facing examples; ensure these remain demo-only and do not pull scripting control policy into main UI modules.
- **DynamisLightEngine overlap risk (watch):** runtime includes `beginFrame/update/render` sequencing; keep this at UI presentation level and avoid pass/frame-graph/render-policy ownership.
- **World/Session/Content overlap risk (low currently):** no direct dependencies today, but `ui-runtime` event integration can drift into orchestration if it grows beyond presentation concerns.

## 6. Ratification Result

**Judgment: ratified with constraints**

Why:

- Main modules are strongly aligned with presentation ownership and mostly clean dependency direction.
- Major authorities (world/session/content/scripting/render-planning) are not directly owned in current code.
- Constraints are needed around public surface breadth and ensuring runtime sequencing does not evolve into control-plane orchestration.

## 7. Recommended Next Step

1. Keep UI strictly as presentation/runtime binding layer.
2. Ratify a narrow boundary contract with `DynamisInput` (input translation ownership) to prevent input-policy drift.
3. Ratify integration contract with `DynamisLightEngine` as render-consumer/provider boundary only (UI draw submission, not render planning).
4. Next repo to review: **DynamisInput** (high-value next boundary because it sits directly at the UI control edge).

---

This document is a boundary-ratification review artifact. It does not perform refactors in this pass.

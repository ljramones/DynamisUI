## demo-ui (JavaFX)

Run (macOS / Windows / Linux):

```bash
cd ~/Dynamis/DynamisUI/demo
mvn -e -pl demo-ui exec:java
```

If you hit cache/lock issues or graphics pipeline issues:

```bash
mvn -e -pl demo-ui exec:java \
  -Dexec.jvmArgs="-Djavafx.cachedir=/tmp/openjfx-cache -Dprism.order=es2,sw -Dprism.verbose=true"
```

Notes:

- JavaFX is not runnable inside sandboxed/headless environments (QuantumRenderer pipeline).
- OpenJFX native classifier is mapped via Maven OS/arch profiles to:
  - macOS Apple Silicon: mac-aarch64
  - macOS Intel: mac-x64
  - Windows: win
  - Linux: linux

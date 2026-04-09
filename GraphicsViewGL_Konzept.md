# GraphicsViewGL – Konzept für ein OpenGL-Backend

**Version:** 1.0
**Datum:** 2026-04-05
**Basis:** GraphicsView 2.2.0-SNAPSHOT (Analyse-Stand April 2026)
**Autor:** Konzept erstellt auf Basis vollständiger Codeanalyse + Technologierecherche

---

## 1. Zusammenfassung

Das GraphicsView-Framework ist eine Java-Reimplementierung von Qt's Graphics View Framework. Die aktuelle Rendering-Infrastruktur basiert vollständig auf Java 2D (`Graphics2D`/AWT). Ziel dieses Konzepts ist ein **OpenGL-beschleunigtes Backend**, das:

- die bestehende API (`IDrawable`, `GraphicsScene`, `GraphicsView`) vollständig unverändert lässt,
- in das Maven-Modul `GraphicsViewGL` integriert wird (welches aktuell nur Proof-of-Concept-Beispiele enthält),
- GPU-Beschleunigung für Karten-/Tile-Rendering ermöglicht, einschließlich perspektivischem Kippen für 3D-Effekte,
- Java 8 Runtime-Kompatibilität beibehält.

Das Dokument analysiert die Architektur, bewertet LWJGL 3 vs. JOGL und leitet einen konkreten Architekturvorschlag ab.

---

## 2. Analyse der bestehenden Architektur

### 2.1 Modul-Übersicht

```
GraphicsView (Parent POM, Version 2.2.0-SNAPSHOT, Java 8, LGPL v3)
├── GraphicsViewCore       — Kern-Rendering (Graphics2D)
├── GeoGraphicsView        — Geo-Erweiterung mit Tile-Cache-Pipeline
├── GeometryEditor         — Geometrie-Editier-Werkzeuge
├── GeotoolsAdapter        — Geotools-Integration
├── GraphicsViewGL         — [Aktuell nur 2 Lehr-Beispiele, kein produktiver Code]
└── GraphicsViewTests      — Testsuite
```

### 2.2 Rendering-Pipeline (Ist-Zustand)

Der komplette Rendering-Pfad ist `Graphics2D`-basiert:

```
GraphicsView.doPaint(Graphics2D g2d)
  └─ internalPaint()
       1. scene.markClean()
       2. applyRenderingHints(g2d)           // Anti-Aliasing etc.
       3. g2d.setTransform(viewTransform)    // View-Matrix (Affine Transform)
       4. notifyPrePaintListeners(g2d, ctx)  // z.B. TileHandler-Hintergrund
       5. getVisibleItems(viewRect, filter)  // Spatial Query per QuadTree
       6. sort(items, by: zOrder)
       7. FOR EACH item:
            item.draw(g2d, drawContext)
            └─ g2d.setTransform(worldTransform)  // Item-Matrix
            └─ drawable.paintItem(g2d, style, ctx) // IDrawable-Impl.
                ├─ ShapeDrawable  → g2d.fill/stroke(shape)
                ├─ ImageDrawable  → g2d.drawImage(image, ...)
                └─ TextDrawable   → g2d.drawString(text, ...)
            └─ draw children recursively
       8. notifyPostPaintListeners(g2d, ctx)
```

Die Hauptklassen und ihre Rollen:

| Klasse / Interface | Rolle |
|----|-----|
| `GraphicsScene` | Container aller Items; Dirty-Tracking; `IItemStorage` (QuadTree/List) |
| `GraphicsView` | Viewport-Verwaltung; Render-Loop (ScheduledExecutorService, 30 FPS); View-Transform |
| `GraphicsItem` | Szenengraph-Knoten; lokale + Welt-Transform; Dirty-Caching; Z-Order; Kinder |
| `IDrawable` | Rendering-Strategie pro Item: `paintItem(Graphics2D, DrawableStyle, IDrawContext)` |
| `IDrawContext` | Rendering-Kontext: Zugriff auf Zoom, sichtbares Rechteck, View-Referenz |
| `IRenderTarget` | Abstraktion über das Swing-Fenster: `getComponent()`, `requestRepaint()` |
| `IPaintListener` | Pre-/Post-Paint-Hooks: `prePaint(g2d, ctx)` / `postPaint(g2d, ctx)` |
| `IGraphicsViewHandler` | Plugin-Schnittstelle: `install(view)` / `uninstall(view)` / `notifySceneCleared()` |

### 2.3 Koordinaten- und Transform-System

GraphicsView verwendet zwei Koordinatenräume:
- **Szenen-Koordinaten:** Logische Einheiten (z.B. Meter oder Grad bei Geo)
- **View-Koordinaten (Pixel):** Bildschirmpixel des `IRenderTarget`

Die View-Transformation ist eine `AffineTransform` aus Center (X/Y), Scale (X/Y) und Rotation. Item-Transforms werden hierarchisch komponiert (Eltern-Kind). Beide liegen als beobachtbare `IParameter<T>` vor (Java-Beans-PropertyChange).


## 3. Erweiterungspunkte: Wo kann OpenGL andocken?

Es gibt fünf natürliche Schnittstellen, an denen ein OpenGL-Backend eingehängt werden kann, ohne die bestehende API zu brechen:

### 3.1 `IRenderTarget` — Die wichtigste Schnittstelle (Empfohlen: Primärer Einstiegspunkt)

```java
public interface IRenderTarget {
    void setGraphicsView(GraphicsView view);
    void requestRepaint();
    Rectangle getVisibleRect();
    int getWidth();
    int getHeight();
    Component getComponent(); // ← gibt die Swing-Komponente zurück
}
```

Durch Implementierung von `IRenderTarget` kann die Swing-Komponente durch einen OpenGL-Canvas ersetzt werden. Dies ist der sauberste Einstiegspunkt, weil er die Rendering-Infrastruktur isoliert.

**Implementierungsoption A (JOGL):** `GLCanvas` oder `GLJPanel` als `Component`-Quelle

### 3.2 `IDrawable` — Item-spezifisches GL-Rendering

```java
public interface IDrawable {
    void paintItem(Graphics2D g, DrawableStyle style, IDrawContext ctx);
}
```

## 4. Status des bestehenden GraphicsViewGL-Moduls

Das Modul `GraphicsViewGL` enthält **keinen produktiven Code für ein OpenGL-Backend**.

## 5. Technologievergleich: LWJGL 3 vs. JOGL (Stand April 2026)

### 5.1 Überblick

| Kriterium | LWJGL 3 | JOGL |
|-----------|---------|------|
| Aktuelle Stable-Version | 3.3.6 (Januar 2025) | 2.6.0 (August 2025) |
| Java 8 Runtime | Ja (3.3.x-Linie) | Ja (explizit validiert in 2.6.0) |
| Swing/AWT-Integration | Via `lwjgl3-awt` (externes Community-Projekt) | Nativ: `GLCanvas`, `GLJPanel`, `NewtCanvasAWT` |
| 2D-Tile-/Karten-Rendering | Vollständig möglich | Vollständig möglich + NASA WorldWind als Referenz |
| Perspektivisches 3D | Vollständiges OpenGL-API | Vollständiges OpenGL-API |
| Community-Größe | Groß (Game-Dev, Minecraft-Ökosystem) | Klein (GIS/Wissenschaft) |
| Dokumentationsqualität | Gut bis sehr gut | Ausreichend, lückenhaft |
| Maven Central | Sauber verfügbar | Verfügbar (Versionssprung 2.3.2 → 2.6.0) |
| Maintenance-Gesundheit | Stark, viele Contributors | Funktional, primär ein Hauptentwickler |
| macOS OpenGL-Problemtatik | Vorhanden (GLFW + EDT-Konflikt) | Vorhanden, aber AWT-nativ handhabbar |

### 5.2 Swing/AWT-Integration im Detail

Dies ist der entscheidende Unterschied für dieses Projekt:

**JOGL (nativ):**
JOGL wurde von Anfang an für Swing/AWT-Embedding entwickelt. Es liefert:
- `GLCanvas` (heavyweight AWT-`Canvas`, volle GPU-Beschleunigung)
- `GLJPanel` (lightweight Swing-`JPanel`, nutzt GPU-Readback – etwas langsamer, aber keine Z-Order-Probleme mit anderen Swing-Komponenten)
- `NewtCanvasAWT` (NEWT-Fenster in AWT eingebettet, beste Performance)

## 6. Architekturvorschlag für das Modul `GraphicsViewGL`

### 6.1 Überblick: Adapter/Bridge-Pattern

Das Ziel ist, die gesamte bestehende API unverändert zu lassen. Das OpenGL-Backend soll als **optionales, austauschbares Rendering-Subsystem** funktionieren. Jede Anwendung kann zwischen dem Standard-Java2D-Backend und dem GL-Backend wählen, ohne Code an anderer Stelle ändern zu müssen.

```
bestehende API (unverändert)          neues GL-Backend (GraphicsViewGL)
─────────────────────────────         ──────────────────────────────────
IRenderTarget                ←────── GLRenderTarget (JOGL GLCanvas/GLJPanel)
```

### 6.2 Neue Klassen und Interfaces

#### 6.2.1 `GLRenderTarget` (implements `IRenderTarget`)

Kernkomponente: Ersetzt `JPanelRenderTarget` durch einen JOGL-Canvas. Implementiert `GLEventListener` für den JOGL-Render-Callback.

Verantwortlichkeiten:
- Hält `GLCanvas` oder `GLJPanel` (konfigurierbar, Default: `GLCanvas` für maximale Performance)
- Implementiert `GLEventListener.display()` → delegiert an `GLGraphicsView.doPaintGL(gl)`
- `getComponent()` gibt den `GLCanvas`/`GLJPanel` zurück
- `requestRepaint()` ruft `glAutoDrawable.display()` oder `SwingUtilities.invokeLater(animator::display)` auf

- **Strategie B – Nativer GL-Render-Loop:** Der Render-Loop wird komplett neu als GL-Pipeline implementiert. Items mit `IGLDrawable`-Implementierung werden nativ gerendert; alle anderen werden in einen Offscreen-Buffer (PBuffer/FBO) per Java2D gerendert und dann als Textur composited.
#### 6.2.7 GLSL-Shader für Tile-Rendering

Minimal zwei Shader-Programme:
- **Flat Shader:** Tile-Rendering in 2D (direkte Draufsicht) – orthographische Projektion
- **Perspective Tilt Shader:** Tile-Rendering mit einstellbarem Neigungswinkel (Pitch) – perspektivische Projektion mit `fov`, `pitch`, `heading`

Shader werden als Ressourcen-Dateien im Modul mitgeliefert (`.vert`/`.frag` im Classpath).

### 6.3 Maven-Modul-Struktur (nach Umbau)

```
GraphicsViewGL/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── de/sos/gvc/gl/
        │       ├── GLRenderTarget.java
        │       ├── GLGraphicsView.java
        │       ├── IGLDrawable.java
        │       ├── IGLDrawContext.java
        │       ├── GLDrawContext.java
        │       ├── GLTextureCache.java
        │       └── tiles/
        │           ├── GLTileHandler.java
        │           ├── GLTileDrawable.java
        │           └── GLTileTextureUploader.java
        └── resources/
            └── de/sos/gvc/gl/shader/
                ├── tile_flat.vert
                ├── tile_flat.frag
                ├── tile_tilt.vert
                └── tile_tilt.frag
```

**Neue pom.xml-Abhängigkeiten:**
- `org.jogamp.gluegen:gluegen-rt-main:2.6.0`
- `org.jogamp.jogl:jogl-all-main:2.6.0`
- `org.joml:joml:1.10.7` (oder aktuell stabile, bleibt erhalten)
- LWJGL-Abhängigkeiten entfernen (oder in separates Submodul für Legacy-Beispiele auslagern)

### 6.4 Entscheidung: Rendering-Strategie A oder B?

| Aspekt | Strategie A (GL-backed Graphics2D) | Strategie B (Nativer GL-Loop) |
|--------|-----------------------------------|-----------------------------|
| Aufwand | Gering – alle IDrawables funktionieren sofort | Hoch – jede IDrawable-Impl. braucht ggf. GL-Pendant |
| Performance | Mittel – Graphics2D-Overhead bleibt | Hoch – direktes OpenGL, keine Readbacks |
| API-Bruch | Keiner | Keiner für externe API; interner Render-Loop ändert sich |
| Tile-3D-Tilt | Möglich, aber unelegant | Sauber mit Shader |
| Implementierungsrisiko | Niedrig | Mittel |

# YNS Styles - Fabric 1.21.11 (v0.3.0)

Pro Kategorie (Totem, Obsidian, Crystal, Anchor, Schwert, Enderperle, Hotbar ...) einen eigenen Style waehlen.
Die Auswahl wird zu EINEM Pack gebaut (`resourcepacks/YNS-Active`) und sofort aktiviert.

## Bedienung
- Taste: **Rechts-Shift** (aenderbar in den Steuerungs-Einstellungen, Kategorie "YNS Styles")
- Links Kategorie waehlen, rechts den Style anklicken -> wird direkt angewendet

## Eigene Texturen
- **Pack-Ordner** (`.minecraft/yns/styles/`): eigene `.zip`-Packs oder entpackte Ordner reinlegen.
  Ein Pack erscheint automatisch in jeder Kategorie, fuer die es Texturen enthaelt.
- **Textur-Ordner** (`.minecraft/yns/textures/`): einzelne PNGs reinlegen, z. B. `totem_of_undying.png`,
  `obsidian.png`, `diamond_sword.png`. Es zaehlt nur der Dateiname (Animationen: `datei.png.mcmeta` dazu).
  Danach "Aktualisieren" und in der Kategorie `[Eigene Texturen]` waehlen.
- "Zurücksetzen" = ueberall wieder Vanilla

## Dateien
- `.minecraft/yns/config.json` - Auswahl
- `.minecraft/resourcepacks/YNS-Active/` - das erzeugte Pack (nicht von Hand aendern)

## Cloud (optional, standardmaessig aus)
`cloudBaseUrl` in `config.json` kann auf ein eigenes Repo zeigen (mit `index.json` und `packs/`).

## Nicht enthalten
- Echte Himmels-Skyboxen (brauchen OptiFine/FabricSkyBoxes). Die Kategorie "Himmel" tauscht nur Sonne, Mond, Wolken usw. aus.

## Bauen
Java 21 + Gradle 9.x: `gradle build` (oder `build.bat`) -> JAR in `build/libs/yns-0.3.0.jar`.
Alternativ GitHub Actions (`.github/workflows/build.yml`).

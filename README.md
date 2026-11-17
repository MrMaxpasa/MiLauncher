# Mi Launcher

A minimalist, one‑handed Android home screen built with **Kotlin** and **Jetpack Compose**.
Thin lines, pure black for OLED screens, and everything reachable with your thumb.

*Un launcher minimalista para Android, pensado para usarse con una mano. [Versión en español más abajo.](#español)*

<!-- Screenshots: add home.png, alphabet.png and notes.png to docs/screenshots/ and remove this comment wrapper
<p>
  <img src="docs/screenshots/home.png" width="250" alt="Home screen">
  <img src="docs/screenshots/alphabet.png" width="250" alt="Alphabet wave">
  <img src="docs/screenshots/notes.png" width="250" alt="Notes panel">
</p>
-->

## Features

- **Typographic clock** — the day in huge hairline letters, crossed by a line with the time, date and your next calendar event (or battery level). Tap each part to open Alarms, Calendar or battery usage.
- **Favorites list** — your apps in a vertical list with monochrome icons (Android 13+ themed icons, grayscale fallback). Long‑press to add, remove or reorder by dragging.
- **Alphabet scroller with a wave effect** — slide along the A–Z column: nearby letters bend away and grow, a large guide letter follows your finger, with haptic ticks and a spring bounce on release. The favorites star and the notebook live in the same column.
- **App drawer** — every installed app grouped by letter with thin separators.
- **Notes panel** — slides up from the notebook icon: a 7‑day strip from the device calendar (dots on busy days) above a hosted **Google Keep** widget, so notes stay in Keep and keep syncing.
- **Notifications next to apps** — a dot and the latest notification under each app name; tap it to open the notification, long‑press the app to clear them.
- **Now playing** — the current track appears as a row with an animated equalizer and play/pause/next controls.
- **Gestures** — swipe down anywhere on the home screen to open the system notification shade.

## Tech

| Area | What it uses |
|---|---|
| UI | Jetpack Compose, Material 3, custom `Canvas` line icons, `graphicsLayer` animations, `Animatable` springs |
| Architecture | Single activity + `AndroidViewModel` with `StateFlow`, small repositories |
| Apps & icons | `PackageManager`, `AdaptiveIconDrawable.getMonochrome()` |
| Widgets | `AppWidgetHost` / `AppWidgetHostView`, bind + configure flow |
| Calendar | `CalendarContract.Instances` (read‑only) |
| Notifications & media | `NotificationListenerService`, `MediaSessionManager`, `MediaController` |
| Storage | `SharedPreferences` (favorites order, chosen widget) |

Min SDK 26 · Target SDK 35 · Kotlin 2.1

## Build & run

1. Open the project in Android Studio.
2. Run it on a device or emulator.
3. Press Home and choose **Mi Launcher** (or *Settings › Apps › Default apps › Home app*).
4. First run: tap **Activar** to grant notification access (notifications + music), open the notebook and tap **Mostrar mi calendario**, then pick the Keep widget.

> Installing from an APK file on Android 13+? Notification access is blocked for side‑loaded apps until you allow it in *Settings › Apps › Mi Launcher › ⋮ › Allow restricted settings*.

## Project structure

```
app/src/main/java/com/milauncher/
  MainActivity.kt          HOME activity, lifecycle, widget results
  LauncherViewModel.kt     apps, favorites, calendar state, UI state
  NotesWidgetHost.kt       hosts the notes (Keep) widget
  NotifListener.kt         notifications + media sessions
  data/                    app list, icons, calendar
  ui/                      clock, alphabet bar, rows, panels, theme, icons
```

## Credits

- Font: [Jost](https://github.com/indestructible-type/Jost) by indestructible type — SIL Open Font License 1.1 (`licenses/Jost-OFL.txt`).
- Code: MIT License (`LICENSE`).

---

## Español

Launcher minimalista para Android hecho con **Kotlin** y **Jetpack Compose**: líneas finas, negro puro para pantallas OLED y todo al alcance del pulgar.

**Funciones**
- **Reloj tipográfico**: el día en letras enormes y finas, cruzado por la hora, la fecha y el próximo evento (o la batería). Cada parte se puede tocar.
- **Favoritas** en lista vertical con iconos monocromos; pulsación larga para añadir, quitar u ordenar arrastrando.
- **Alfabeto con efecto ola**: las letras cercanas al dedo se apartan y crecen, aparece una letra guía grande, vibra en cada cambio y rebota al soltar. La estrella y la libreta forman parte de la misma columna.
- **Lista de apps** agrupada por letra con separadores finos.
- **Panel de notas**: semana de 7 días desde el calendario del móvil y el widget de **Google Keep** dentro del launcher.
- **Notificaciones junto a cada app** y **música** con ecualizador animado y controles.
- **Gestos**: deslizar hacia abajo abre las notificaciones del sistema.

**Cómo probarlo**: ábrelo en Android Studio, ejecútalo en el móvil y elige *Mi Launcher* como aplicación de inicio.

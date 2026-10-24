# Contributing

Thanks for helping. Bug reports, docs fixes and pull requests are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on the approach.
- New effects need a clear gap: Kapow leaves transform-only animations (shake, bounce, slide, fade) to other libraries. Say which app moment the effect is for.
- Visual bugs depend on the platform. Say which target, device and OS version you saw it on, and attach a screen recording if you can.

## Setup

You need JDK 17 and an Android SDK. The Gradle wrapper handles the rest. For iOS you need Xcode on macOS.

The docs site and the browser tests use [Bun](https://bun.sh) 1.3+ and Node.js 22.19+ (the version is in `.node-version`).

```sh
./gradlew :kapow:jvmTest                              # desktop tests, quickest feedback
./gradlew :samples:web:wasmJsBrowserDevelopmentRun    # the effect gallery in the browser
./gradlew :samples:desktop:run                        # the same gallery in a desktop window (hotRunJvm for hot reload)
```

## Where things live

| path                                 | what                                                                                 |
| ------------------------------------ | ------------------------------------------------------------------------------------ |
| `kapow/src/commonMain`               | The public API, the effect nodes, the particle system and the pure timing math.      |
| `kapow/src/*Main/.../platform`       | Reduced-motion readers for Android, iOS, the browser and the JVM.                    |
| `kapow-crease`                       | Effects driven by the hinge angle, through Crease.                                   |
| `kapow-multihaptic`                  | A `KapowFeedback` handler backed by multihaptic.                                     |
| `samples/gallery`                    | The gallery's examples, their controls and the catalog, shared by web and desktop.   |
| `samples/web`                        | The effect gallery, compiled to Kotlin/Wasm. Each example's source is shown next to it. |
| `samples/desktop`                    | The same gallery in a desktop window (JVM).                                          |
| `samples/design`                     | Theme and widgets the gallery uses.                                                  |
| `apps/docs`                          | The docs site. Its code samples are compiled against the library.                    |
| `design/`                            | The design system the gallery and the docs use.                                      |

## Checks

```sh
./gradlew spotlessCheck apiCheck :kapow:jvmTest :kapow:testAndroidHostTest :kapow:koverVerify   # what CI runs
./gradlew :kapow:iosSimulatorArm64Test     # on macOS
./gradlew :kapow:verifyRoborazziJvm        # screenshot tests; recordRoborazziJvm after an intended visual change
./gradlew spotlessApply                    # format the code (ktlint_official)
./gradlew apiDump                          # after changing the public API
bun run build:docs                         # from the repository root
bun run test:web                           # browser tests of the gallery, after :samples:web:wasmJsBrowserDistribution
```

CI runs all of them on every pull request. `koverVerify` fails the build when coverage drops below the gate in the Gradle build.

## Conventions

- Kotlin in the official style. Run `./gradlew spotlessApply` before you push.
- Everything that isn't public API is `internal`. Changing the public API means running `./gradlew apiDump` and reviewing the diff in each module's `api` folder.
- Animation frames must not recompose. Effects keep their state in a `Modifier.Node` and read it in the draw or layer phase; the recomposition test in `kapow/src/jvmTest` checks this for every effect.
- Pure math (springs, curves, particle steps, mask geometry) lives in plain functions and is tested in `commonTest`.
- If you port math or timings from Pow, add the attribution header the other ported files carry.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` for anything users will notice.

## License

By contributing you agree that your contributions are licensed under the MIT license.

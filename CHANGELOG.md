# Changelog

All notable changes to `io.github.jantursky:kapow` and its companion artifacts are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-24

### Added

- Change effects that play once when a value changes: `Modifier.changeEffect(value, effect)` with `ChangeEffect.spray` (particles in a `ParticleShape`, as text or from a painter), `rise` (a label that floats up), `shine`, `pulse` (a ring or fill in the element's `Shape`), `glow`, `spin` (around the X, Y or Z axis), `ripple`, `flash`, `burst` (a comic star burst), `speedLines`, `sparkle`, `fireworks`, `confettiPop`, `trace` (a stroke around the outline), `strike`, `stamp` and `glitch`. `.delay(…)` starts any of them later. Shine, and a pulse with more than one ring, ignore changes that arrive while they still play, so quick changes don't stack up. The first composition plays nothing.
- Conditional effects that run while a condition is true: `Modifier.conditionalEffect(condition, effect)` with `ConditionalEffect.glow`, `pushDown`, `smoke`, `borderBeam`, `marchingAnts`, `rainbowBorder`, `sparkles`, `orbit`, `bubbles`, `flame`, `grayscale`, `spotlight` and `repeat(changeEffect, every)`.
- Progress effects drawn at a progress you supply, such as a scroll offset: `Modifier.progressEffect(effect) { progress }` with `ProgressEffect.shine`, `glow`, `iris`, `fill` (a liquid rising with a wave) and `trace` (the outline drawn up to the progress).
- Transitions for `AnimatedVisibility` and `AnimatedContent`: `KapowEnter` and `KapowExit` with `iris`, `clock`, `wipe`, `glare`, `blinds`, `dissolve`, `curtain`, `shapeIris`, `checker`, `tear` and `blob`, plus `anvil` and `pop` to enter and `poof`, `shatter` and `disintegrate` to exit. `StarShape` is a star or comic-burst `Shape` for `shapeIris` and anywhere else a `Shape` fits. Use `KapowAnimatedVisibility`, or `Modifier.kapowTransition(scope, enter, exit)` next to Compose's own transitions.
- `KapowParticleLayer`, which draws particles above its content so a clipping parent doesn't cut them off.
- Reduced motion from the platform (animator duration scale on Android, Reduce Motion on iOS, `prefers-reduced-motion` in the browser), with `LocalKapowMotion` to override it. Moving effects become a short brighten or fade.
- `KapowFeedback` and `KapowFeedbackEvent`: every effect can send an event for haptics or sound through `LocalKapowFeedback`.
- `kapow-crease` (`io.github.jantursky:kapow-crease`): `Modifier.hingeEffect(effect, angleRange)` plays a progress effect at the hinge angle of a foldable.
- `kapow-multihaptic` (`io.github.jantursky:kapow-multihaptic`): `rememberMultihapticFeedback()` plays feedback events as haptics through multihaptic.
- Targets: Android (minSdk 23), iOS, JVM and wasmJs. The core needs only Compose runtime, foundation, UI and animation. Not yet checked on an Android or iOS device, or in Firefox and Safari.

[Unreleased]: https://github.com/jantursky/kapow/compare/1.0.0...HEAD
[1.0.0]: https://github.com/jantursky/kapow/releases/tag/1.0.0

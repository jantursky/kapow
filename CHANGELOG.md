# Changelog

All notable changes to `io.github.jantursky:kapow` and its companion artifacts are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2026-10-28

### Added

- `KapowTrigger` and `rememberKapowTrigger()`: `Modifier.changeEffect(trigger, effect)` plays the effect each time `trigger.fire()` is called, from a click or any event, without a value to watch and without recomposing.
- Effects that follow an `InteractionSource`: `Modifier.conditionalEffect(interactionSource, interaction, effect)` runs while the element is pressed, hovered, focused or dragged (`KapowInteraction`), and `Modifier.changeEffect(interactionSource, interaction, effect)` plays each time that interaction starts.
- `Modifier.edgeEffect(condition, edge, effect)` plays when a Boolean turns true or false (`KapowEdge`), and `playOnStart` on `changeEffect`, `edgeEffect` and triggers plays the effect on the first composition.

### Changed

- `androidx.compose.foundation` is now an `api` dependency of the core, because `InteractionSource` is part of the new signatures. `changeEffect(value, …)` gained `playOnStart`; its 1.1.0 signature stays as a hidden overload.

## [1.1.0] - 2026-10-26

### Added

- New effects: `ChangeEffect.hearts`; `ConditionalEffect.shimmer`, `breathe`, `ping` and `confettiRain`; `Modifier.kapowSkeleton(loading)`, a shimmering placeholder over always-laid-out content that fades away when loading ends; `KapowTypewriter` (typed or scrambled text with an optional cursor); `KapowCounter` (digits that roll, or a number that counts); `Modifier.kapowTilt` (3D tilt toward the pointer with a light sheen).
- Many more options on every existing effect: all 17 change effects, 13 conditional effects, 5 progress effects and the transitions (soft edges, directions, easing, seeds, brushes, counts, sizes and more). The defaults draw what they drew before.
- `ParticlePhysics` and `EmissionArea` for direction, spread, speed, gravity, drag, lifetime, size, spin, fade and where particles start, on the particle effects.
- Wrappers that work on any effect: `ChangeEffect.cooldown`, `times`, `speed`, `reducedMotion`; `ConditionalEffect.fadeIn`, `fadeOut`, `startDelay`, `speed`, `reducedMotion`; `ProgressEffect.curve`, `smoothed`, `reducedMotion`; `KapowEnter` and `KapowExit` `.delay`, `.easing`, `.reducedMotion`. `ReducedMotionMode` chooses the reduced-motion behavior of one effect.
- `KapowStagger.delay(index)` for cascading entrances and effects in lists.

### Changed

- Factories that gained options keep their 1.0.0 signatures as hidden overloads, so code and binaries built against 1.0.0 keep working.

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

[Unreleased]: https://github.com/jantursky/kapow/compare/1.2.0...HEAD
[1.2.0]: https://github.com/jantursky/kapow/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/jantursky/kapow/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/jantursky/kapow/releases/tag/1.0.0

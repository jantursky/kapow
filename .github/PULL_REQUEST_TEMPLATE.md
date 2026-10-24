## What and why

<!-- What this changes, and the problem it solves. Link the issue if there is one. -->

## Checklist

- [ ] `./gradlew spotlessCheck apiCheck :kapow:jvmTest :kapow:testAndroidHostTest :kapow:koverVerify` passes
- [ ] `./gradlew :kapow:iosSimulatorArm64Test` passes, if iOS code changed
- [ ] `./gradlew apiDump` was run, if the public API changed
- [ ] New behavior has tests
- [ ] `./gradlew :kapow:verifyRoborazziJvm` passes, or screenshots were re-recorded on purpose
- [ ] The gallery in `samples/web` has an example, if this adds an effect
- [ ] Docs in `apps/docs` are updated, if the behavior users see changed
- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]`

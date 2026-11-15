import { test } from '@playwright/test'

type Engine = 'chromium' | 'firefox' | 'webkit'

/**
 * Gallery tests that a browser engine can't pass yet. The README and the docs platform table name these. A test is
 * listed here instead of being retried until it passes. `issue` is the upstream issue, or null while none is filed.
 */
export type EngineSkip = {
  engine: Engine
  /** The test title, exactly as passed to `test(...)`. */
  title: string
  reason: string
  issue: string | null
}

export const engineSkips: EngineSkip[] = [
  {
    engine: 'webkit',
    title: 'the overview links to the docs and copies the install line',
    reason: "Playwright's WebKit does not know the clipboard-write permission the test grants.",
    issue: null,
  },
]

/**
 * Console errors an engine logs while the gallery works, so the "no console errors" checks ignore them there only.
 * Every other error still fails the test.
 */
export type EngineConsoleError = {
  engine: Engine
  pattern: RegExp
  reason: string
  issue: string | null
}

export const engineConsoleErrors: EngineConsoleError[] = [
  {
    engine: 'webkit',
    pattern: /WEBGL_debug_renderer_info not enabled/,
    reason: "Skiko asks the GPU's name, which WebKit hides on purpose; the query fails and Skiko carries on.",
    issue: null,
  },
  {
    engine: 'webkit',
    pattern: /WebGL: INVALID_OPERATION: delete: object does not belong to this context/,
    reason: 'WebKit logs it when Skiko frees a GL object after the canvas context changed; nothing visible breaks.',
    issue: null,
  },
]

/** Skips the running test when its engine and title are on the list. Call it first in the test body. */
export const skipKnownGaps = (engine: string, title: string) => {
  const skip = engineSkips.find((entry) => entry.engine === engine && entry.title === title)
  test.skip(skip !== undefined, skip ? `${skip.reason} (${skip.issue ?? 'no upstream issue filed'})` : '')
}

/** Whether [message] is a console error [engine] is known to log. */
export const isKnownConsoleError = (engine: string, message: string) =>
  engineConsoleErrors.some((entry) => entry.engine === engine && entry.pattern.test(message))

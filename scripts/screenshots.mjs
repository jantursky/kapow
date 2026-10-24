// Builds the web gallery, serves it and records the README GIFs to .github/assets/: one per group of effects,
// in the light and the dark theme. Run it with `bun run screenshots` whenever the gallery or an effect changes.
//
// The page runs on Playwright's fake clock, so every frame is exactly 40 ms after the last however long a
// screenshot takes. ffmpeg (on the PATH) turns the frames into a GIF.

/* global document -- used inside callbacks that run in the page */

import { spawnSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { chromium } from '@playwright/test'
import sharp from 'sharp'

import { serve } from './serve-demo.mjs'

const root = fileURLToPath(new URL('..', import.meta.url))
const out = join(root, '.github/assets')
const FRAME_MS = 40
const WIDTH = 560

/**
 * Each GIF plays a few gallery pages one after another. A step either presses the page's main control
 * (`press`) or lets time run (`ms`); every step records its frames.
 */
const GIFS = [
  {
    name: 'change',
    pages: [
      { id: 'change-spray', steps: [{ press: true }, { ms: 1400 }] },
      { id: 'change-shine', steps: [{ press: true }, { ms: 1200 }] },
      { id: 'change-pulse', steps: [{ press: true }, { ms: 1400 }] },
      { id: 'change-spin', steps: [{ press: true }, { ms: 1600 }] },
    ],
  },
  {
    name: 'conditional',
    pages: [
      { id: 'conditional-repeat', steps: [{ press: true }, { ms: 4200 }] },
      { id: 'conditional-glow', steps: [{ ms: 600 }, { press: true }, { ms: 1000 }, { press: true }, { ms: 800 }] },
      { id: 'conditional-smoke', steps: [{ press: true }, { ms: 2400 }] },
    ],
  },
  {
    name: 'transitions',
    pages: [
      { id: 'transition-iris', steps: [{ press: true }, { ms: 900 }, { press: true }, { ms: 900 }] },
      { id: 'transition-poof', steps: [{ ms: 400 }, { press: true }, { ms: 900 }] },
      { id: 'transition-anvil', steps: [{ press: true }, { ms: 1800 }] },
    ],
  },
]

const build = () => {
  const result = spawnSync('./gradlew', [':samples:web:wasmJsBrowserDistribution'], { cwd: root, stdio: 'inherit' })
  if (result.status !== 0) process.exit(result.status ?? 1)
}

/** Runs the fake clock until the stage canvas has drawn, since nothing paints while the clock stands still. */
const settle = async (page) => {
  for (let i = 0; i < 300; i++) {
    await page.clock.runFor(100)
    if (await page.locator('#stage canvas').isVisible()) break
  }
  await page.evaluate(() => document.fonts.ready)
  await page.clock.runFor(1000)
}

/** Presses the first control under the stage. The first time, Tab moves focus from the canvas into it. */
const press = async (page, first) => {
  if (first) {
    await page.locator('#stage canvas').focus()
    await page.keyboard.press('Tab')
  }
  await page.keyboard.press('Enter')
}

/**
 * Finds the effect area in a screenshot of the example card: the rectangle in the stage's background color,
 * without the controls below it. The color is read inside the stage, a little in from its left edge, before
 * anything plays; from there it walks out to the stage's edges.
 */
const stageArea = async (png) => {
  const { data, info } = await sharp(png).raw().toBuffer({ resolveWithObject: true })
  const pixel = (x, y) => Array.from(data.subarray((y * info.width + x) * info.channels, (y * info.width + x) * info.channels + 3))
  const y0 = Math.round(info.height / 3)
  const x0 = Math.round(info.width * 0.12)
  const stage = pixel(x0, y0)
  const at = (x, y) => pixel(x, y).every((c, k) => Math.abs(c - stage[k]) <= 3)
  let left = x0
  while (left > 0 && at(left - 1, y0)) left--
  let top = y0
  while (top > 0 && at(x0, top - 1)) top--
  let bottom = y0
  while (bottom < info.height - 1 && at(x0, bottom + 1)) bottom++
  // Inset past the stage's rounded corners, so only its flat background frames the effect.
  const inset = 24
  return {
    area: { left: left + inset, top: top + inset, width: info.width - 2 * (left + inset), height: bottom - top + 1 - 2 * inset },
    stage,
  }
}

/** Every frame of a GIF has this size; each page's stage is fitted into it on its own background. */
const FRAME = { width: WIDTH * 2, height: Math.round(WIDTH * 2 * 0.6) }

const record = async (browser, url, gif, theme, dir) => {
  let frame = 0
  for (const view of gif.pages) {
    const context = await browser.newContext({
      viewport: { width: 1280, height: 900 },
      deviceScaleFactor: 2,
      colorScheme: theme,
      reducedMotion: 'no-preference',
    })
    const page = await context.newPage()
    await page.clock.install()
    // ?autoplay=0 opens the page with autoplay paused, so only the steps below play the effect.
    await page.goto(`${url}?autoplay=0#${view.id}`)
    await settle(page)
    const stage = page.locator('#stage')
    await stage.scrollIntoViewIfNeeded()
    const { area, stage: [r, g, b] } = await stageArea(await stage.screenshot())
    let pressed = false
    for (const step of view.steps) {
      if (step.press) {
        await press(page, !pressed)
        pressed = true
      }
      const frames = Math.max(1, Math.round((step.ms ?? FRAME_MS) / FRAME_MS))
      for (let i = 0; i < frames; i++) {
        await page.clock.runFor(FRAME_MS)
        const png = await sharp(await stage.screenshot())
          .extract(area)
          .resize({ ...FRAME, fit: 'contain', background: { r, g, b } })
          .png()
          .toBuffer()
        writeFileSync(join(dir, `${String(frame++).padStart(5, '0')}.png`), png)
      }
    }
    await context.close()
  }

  const file = join(out, `${gif.name}-${theme}.gif`)
  const filters = `fps=${1000 / FRAME_MS},scale=${WIDTH}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=96:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle`
  const ffmpeg = spawnSync(
    'ffmpeg',
    ['-y', '-loglevel', 'error', '-framerate', String(1000 / FRAME_MS), '-i', join(dir, '%05d.png'), '-vf', filters, file],
    { stdio: 'inherit' },
  )
  if (ffmpeg.status !== 0) throw new Error(`ffmpeg failed for ${file}`)
  console.log(`${file.slice(root.length)}  ${Math.round(statSync(file).size / 1024)} kB`)
}

if (!process.argv.includes('--no-build')) build()
mkdirSync(out, { recursive: true })
const server = await serve()
const url = `http://127.0.0.1:${server.address().port}/`
const browser = await chromium.launch()
try {
  for (const gif of GIFS) {
    for (const theme of ['light', 'dark']) {
      const dir = mkdtempSync(join(tmpdir(), `kapow-${gif.name}-`))
      try {
        await record(browser, url, gif, theme, dir)
      } finally {
        rmSync(dir, { recursive: true, force: true })
      }
    }
  }
} finally {
  await browser.close()
  server.close()
}

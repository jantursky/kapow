import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import sharp from 'sharp'

// Skiko warns about an API it uses itself. Nothing else may reach the console as an error.
const KNOWN_ERRORS = [/Accessing `memory` via `wasmExports` is deprecated/]

const collectErrors = (page: Page) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error' && !KNOWN_ERRORS.some((known) => known.test(message.text()))) {
      errors.push(message.text())
    }
  })
  return errors
}

// The page is ready once the stage and the frame meter have each mounted a Compose canvas.
const ready = async (page: Page) => {
  await expect(page.locator('#stage canvas')).toHaveCount(1, { timeout: 30_000 })
  await expect(page.locator('#frame-meter canvas')).toHaveCount(1)
}

// How far two screenshots of the stage are apart: the mean difference per channel, 0 to 255.
const difference = async (a: Buffer, b: Buffer) => {
  const [x, y] = await Promise.all([a, b].map((png) => sharp(png).raw().toBuffer()))
  let sum = 0
  for (let i = 0; i < x.length; i++) sum += Math.abs(x[i] - y[i])
  return sum / x.length
}

const openSidebar = async (page: Page) => {
  const details = page.locator('#sidebar-details')
  if ((await details.getAttribute('open')) === null) await details.locator('summary').click()
}

test('the overview loads without console errors', async ({ page }) => {
  const errors = collectErrors(page)
  await page.goto('/')
  await expect(page).toHaveTitle('Kapow')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Effects that fire when something changes.')
  await ready(page)
  expect(errors).toEqual([])
})

test('the sidebar lists every group and navigates to a page', async ({ page }) => {
  await page.goto('/')
  await ready(page)
  await openSidebar(page)
  const sidebar = page.getByRole('navigation', { name: 'Effects' })
  for (const group of ['Change effects', 'Conditional effects', 'Progress effects', 'Transitions', 'More']) {
    await expect(sidebar.getByRole('heading', { name: group })).toBeVisible()
  }

  await sidebar.getByRole('link', { name: 'Shine' }).first().click()
  await expect(page).toHaveURL(/#change-shine$/)
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Shine')
  await openSidebar(page)
  await expect(sidebar.getByRole('link', { name: 'Shine' }).first()).toHaveAttribute('aria-current', 'page')
  await expect(page.locator('#source')).toContainText('fun ShineExample(')

  await openSidebar(page)
  await sidebar.getByRole('link', { name: 'Spray' }).click()
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Spray')
  await expect(page.locator('#source')).toContainText('fun SprayExample(')
})

test('a deep link opens its page directly', async ({ page }) => {
  await page.goto('/#transition-iris')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Iris')
  await expect(page.locator('#example-group')).toHaveText('Transitions')
  await expect(page.locator('#overview')).toBeHidden()
})

test('the theme switch flips the page and remembers the choice', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.goto('/')
  const html = page.locator('html')
  await expect(html).toHaveAttribute('data-theme', 'dark')

  await page.getByRole('button', { name: 'Switch to light theme' }).click()
  await expect(html).toHaveAttribute('data-theme', 'light')
  await expect(page.getByRole('button', { name: 'Switch to dark theme' })).toBeVisible()

  await page.reload()
  await expect(html).toHaveAttribute('data-theme', 'light')
})

test('the page follows the system theme until the visitor picks one', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' })
  await page.goto('/')
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.emulateMedia({ colorScheme: 'dark' })
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})

test('the reduced motion switch offers system, off and on', async ({ page }) => {
  await page.goto('/')
  const motion = page.getByLabel('Reduced motion')
  await expect(motion).toHaveValue('system')
  await expect(motion.locator('option')).toHaveText(['System', 'Off', 'On'])
  await motion.selectOption('reduced')
  await expect(motion).toHaveValue('reduced')
})

test('haptics are off by default and firing with them on raises no errors', async ({ page }) => {
  const errors = collectErrors(page)
  await page.goto('/#change-shine')
  await ready(page)
  const haptics = page.getByLabel('Haptics')
  await expect(haptics).not.toBeChecked()
  await haptics.check()
  await page.locator('#stage canvas').focus()
  await page.keyboard.press('Tab')
  await page.keyboard.press('Enter')
  await page.waitForTimeout(700)
  expect(errors).toEqual([])
})

test('the frame meter renders', async ({ page }) => {
  await page.goto('/')
  await ready(page)
  const meter = page.locator('#frame-meter')
  await expect(meter).toBeVisible()
  const box = await meter.boundingBox()
  expect(box?.width).toBeGreaterThan(100)
  // Compose draws the numbers into a GPU canvas, so look at the pixels the page shows: a dot and text on a
  // flat background leave some variation.
  await expect
    .poll(async () => {
      const { channels } = await sharp(await meter.screenshot()).stats()
      return Math.max(...channels.map((channel) => channel.stdev))
    })
    .toBeGreaterThan(5)
})

// The pages with a live example. A page joins this list when its entry gets an Example in Catalog.kt.
const LIVE = [
  'change-shine',
  'change-pulse',
  'change-glow',
  'change-delay',
  'conditional-glow',
  'progress-shine',
  'progress-glow',
  'progress-scroll',
  'more-hinge',
  'more-feedback',
  'change-spray',
  'change-rise',
  'change-spin',
  'conditional-repeat',
  'conditional-push-down',
  'conditional-smoke',
  'progress-iris',
  'transition-iris',
  'transition-clock',
  'transition-wipe',
  'transition-glare',
  'transition-blinds',
  'transition-poof',
  'transition-anvil',
  'transition-pop',
  'more-particle-layer',
]

for (const id of LIVE) {
  test(`${id} loads, shows its source and fires without errors`, async ({ page }) => {
    const errors = collectErrors(page)
    await page.goto(`/#${id}`)
    await ready(page)
    await expect(page.locator('#source')).toContainText('@Composable')
    // The first control is the primary pill: Tab moves focus from the canvas into it, Enter presses it.
    await page.locator('#stage canvas').focus()
    await page.keyboard.press('Tab')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(700)
    await page.keyboard.press('Enter')
    await page.waitForTimeout(700)
    expect(errors).toEqual([])
  })
}

for (const path of ['/', '/#transition-iris']) {
  test(`the page never scrolls sideways (${path})`, async ({ page }) => {
    await page.goto(path)
    await ready(page)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow).toBeLessThanOrEqual(0)
  })
}

test('an example plays on its own as soon as its page opens', async ({ page }) => {
  await page.goto('/#transition-iris')
  await ready(page)
  await expect(page.getByRole('button', { name: 'Pause autoplay' })).toBeVisible()
  const stage = page.locator('#stage')
  const first = await stage.screenshot()
  // The iris opens over the content without anyone pressing Show.
  await expect.poll(async () => difference(first, await stage.screenshot()), { timeout: 5_000 }).toBeGreaterThan(2)
})

test('pausing stops autoplay and playing starts it again', async ({ page }) => {
  await page.goto('/#transition-iris')
  await ready(page)
  await page.getByRole('button', { name: 'Pause autoplay' }).click()
  const play = page.getByRole('button', { name: 'Play autoplay' })
  await expect(play).toBeVisible()
  // Let a running transition finish, then nothing moves.
  await page.waitForTimeout(1_200)
  const stage = page.locator('#stage')
  const paused = await stage.screenshot()
  await page.waitForTimeout(3_000)
  expect(await difference(paused, await stage.screenshot())).toBeLessThan(0.5)

  await play.click()
  await expect(page.getByRole('button', { name: 'Pause autoplay' })).toBeVisible()
  await expect.poll(async () => difference(paused, await stage.screenshot()), { timeout: 5_000 }).toBeGreaterThan(2)
})

test('?autoplay=0 opens an example paused, and its controls still work', async ({ page }) => {
  await page.goto('/?autoplay=0#transition-iris')
  await ready(page)
  await expect(page.getByRole('button', { name: 'Play autoplay' })).toBeVisible()
  const stage = page.locator('#stage')
  const still = await stage.screenshot()
  await page.waitForTimeout(2_000)
  expect(await difference(still, await stage.screenshot())).toBeLessThan(0.5)
  // Show is the first control.
  await page.locator('#stage canvas').focus()
  await page.keyboard.press('Tab')
  await page.keyboard.press('Enter')
  await expect.poll(async () => difference(still, await stage.screenshot()), { timeout: 5_000 }).toBeGreaterThan(2)
})

const LINKS = {
  Docs: 'https://kapow.vercel.app',
  GitHub: 'https://github.com/jantursky/kapow',
  'Maven Central': 'https://central.sonatype.com/artifact/io.github.jantursky/kapow',
}

test('the nav names the version and links to the docs, GitHub and Maven Central', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('.version')).toHaveText(/^v\d+\.\d+\.\d+/)
  const nav = page.getByRole('navigation', { name: 'Project' })
  for (const [name, href] of Object.entries(LINKS)) {
    // On a phone some of them fold away; they are still there for wider windows.
    await expect(nav.getByRole('link', { name, includeHidden: true })).toHaveAttribute('href', href)
  }
})

for (const path of ['/', '/#change-shine']) {
  test(`every view has the footer with its links (${path})`, async ({ page }) => {
    await page.goto(path)
    const footer = page.getByRole('contentinfo')
    await expect(footer).toContainText('Kapow · MIT licensed · Inspired by Pow')
    await expect(footer.getByRole('link', { name: 'Pow' })).toHaveAttribute('href', 'https://github.com/EmergeTools/Pow')
    const links = footer.getByRole('navigation', { name: 'Footer' })
    for (const [name, href] of Object.entries({
      ...LINKS,
      License: 'https://github.com/jantursky/kapow/blob/main/LICENSE',
    })) {
      await expect(links.getByRole('link', { name })).toHaveAttribute('href', href)
    }
  })
}

test('the overview links to the docs and copies the install line', async ({ page, context }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write'])
  await page.goto('/')
  await expect(page.getByRole('link', { name: 'Read the docs' })).toHaveAttribute('href', LINKS.Docs)
  const line = page.locator('#install-line')
  await expect(line).toHaveText(/^implementation\("io\.github\.jantursky:kapow:\d+\.\d+\.\d+.*"\)$/)
  await page.locator('#copy-install').click()
  await expect(page.locator('#copy-install')).toHaveText('Copied')
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(await line.textContent())
})

test('copying without a clipboard says so instead of throwing', async ({ page }) => {
  const errors = collectErrors(page)
  await page.addInitScript(() => Object.defineProperty(navigator, 'clipboard', { value: undefined }))
  await page.goto('/')
  await page.locator('#copy-install').click()
  await expect(page.locator('#copy-install')).toHaveText('Copy failed')
  expect(errors).toEqual([])
})

for (const [view, path] of [['the overview', '/'], ['an example', '/#change-shine']]) {
  for (const theme of ['dark', 'light'] as const) {
    test(`no serious accessibility violations on ${view} in the ${theme} theme`, async ({ page }) => {
      await page.emulateMedia({ colorScheme: theme })
      await page.goto(path)
      await ready(page)
      await openSidebar(page)
      // The Compose canvases draw their own pixels, so axe checks the page around them.
      const results = await new AxeBuilder({ page }).exclude('#stage').exclude('#frame-meter').analyze()
      const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical')
      expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
    })
  }
}

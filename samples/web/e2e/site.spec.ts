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

test('the page never scrolls sideways', async ({ page }) => {
  await page.goto('/')
  await ready(page)
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(0)
})

for (const theme of ['dark', 'light'] as const) {
  test(`no serious accessibility violations on the overview in the ${theme} theme`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: theme })
    await page.goto('/')
    await ready(page)
    await openSidebar(page)
    // The Compose canvases draw their own pixels, so axe checks the page around them.
    const results = await new AxeBuilder({ page }).exclude('#stage').exclude('#frame-meter').analyze()
    const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical')
    expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
}

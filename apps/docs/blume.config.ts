import { readFileSync } from 'node:fs'

import { defineConfig } from 'blume'

const properties = readFileSync(new URL('../../gradle.properties', import.meta.url), 'utf8')
const version = /^VERSION_NAME=(.+)$/m.exec(properties)?.[1]?.trim()
if (!version) throw new Error('VERSION_NAME is missing from gradle.properties')

export default defineConfig({
  title: 'Kapow',
  description:
    'Particle, light and mask effects for Compose Multiplatform, fired by value changes and conditions, plus enter and exit transitions.',
  logo: '/logo.svg',
  github: {
    owner: 'jantursky',
    repo: 'kapow',
    dir: 'apps/docs',
  },
  navigation: {
    actions: [
      { href: 'https://kapow-demo.vercel.app', label: 'Demo' },
      { href: 'https://central.sonatype.com/artifact/io.github.jantursky/kapow', label: 'Maven Central' },
    ],
  },
  content: {
    root: 'docs',
  },
  theme: {
    accent: '#6b62f2',
    radius: 'lg',
    mode: 'dark',
  },
  deployment: {
    site: 'https://kapow.vercel.app',
  },
  variables: {
    version,
    demo: 'https://kapow-demo.vercel.app',
  },
})

#!/usr/bin/env node
// Fails when the gallery's production wasm files, compressed with gzip level 9, grow past the budget in
// samples/web/size-budget.json. Run it after `./gradlew :samples:web:wasmJsBrowserDistribution`.
// Usage: node scripts/check-wasm-size.mjs [--budget path] [--dist path]
import { readFileSync, readdirSync } from 'node:fs'
import { gzipSync, constants } from 'node:zlib'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const args = process.argv.slice(2)
const option = (name, fallback) => {
  const at = args.indexOf(name)
  return at >= 0 ? resolve(args[at + 1]) : fallback
}
const budgetFile = option('--budget', join(root, 'samples/web/size-budget.json'))
const dist = option('--dist', join(root, 'samples/web/build/dist/wasmJs/productionExecutable'))

const files = readdirSync(dist).filter((name) => name.endsWith('.wasm'))
if (files.length === 0) {
  console.error(`No .wasm files in ${dist}. Build the gallery first.`)
  process.exit(2)
}
let total = 0
for (const name of files) {
  const gzipped = gzipSync(readFileSync(join(dist, name)), { level: constants.Z_BEST_COMPRESSION }).length
  total += gzipped
  console.log(`${name}: ${gzipped} bytes gzip`)
}
const { maxGzipBytes } = JSON.parse(readFileSync(budgetFile, 'utf8'))
console.log(`total: ${total} bytes gzip, budget: ${maxGzipBytes} bytes`)
if (total > maxGzipBytes) {
  console.error(`Over budget by ${total - maxGzipBytes} bytes. Shrink the build, or raise the budget in ${budgetFile} on purpose.`)
  process.exit(1)
}

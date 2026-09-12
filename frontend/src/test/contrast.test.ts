import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * The engineering standards ask for WCAG 2.1 AA contrast "nos dois temas", which is the
 * kind of promise that quietly stops being true the first time somebody nudges a colour.
 * This reads the real stylesheet rather than a copy of the values, so it fails when the
 * palette changes rather than when a duplicate of it does.
 *
 * It caught the light theme's accent at 3.94:1 — the primary button, including the
 * acknowledgment on the crisis screen.
 */
const AA_NORMAL_TEXT = 4.5

const css = readFileSync(resolve(process.cwd(), 'src/index.css'), 'utf8')

function tokensFor(selector: string): Record<string, string> {
  const block = css.slice(css.indexOf(selector) + selector.length)
  const body = block.slice(block.indexOf('{') + 1, block.indexOf('}'))
  return Object.fromEntries(
    [...body.matchAll(/--([\w-]+):\s*(#[0-9a-f]{6})/gi)].map((m) => [m[1], m[2]]),
  )
}

function relativeLuminance(hex: string): number {
  const channels = [1, 3, 5].map(
    (offset) => Number.parseInt(hex.slice(offset, offset + 2), 16) / 255,
  )
  const linear = channels.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
  return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
}

function contrast(foreground: string, background: string): number {
  const a = relativeLuminance(foreground)
  const b = relativeLuminance(background)
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)
}

const THEMES = {
  dark: tokensFor(':root'),
  light: tokensFor(":root[data-theme='light']"),
}

describe.each(Object.entries(THEMES))('%s theme', (_name, tokens) => {
  it.each([
    ['body text on the page', 'color-text', 'color-bg'],
    ['body text on a card', 'color-text', 'color-surface'],
    ['secondary text on the page', 'color-text-muted', 'color-bg'],
    ['secondary text on a card', 'color-text-muted', 'color-surface'],
    ['label on the primary button', 'color-accent-contrast', 'color-accent'],
  ])('meets AA for %s', (_what, foreground, background) => {
    expect(tokens[foreground]).toBeDefined()
    expect(tokens[background]).toBeDefined()
    expect(contrast(tokens[foreground], tokens[background])).toBeGreaterThanOrEqual(AA_NORMAL_TEXT)
  })
})

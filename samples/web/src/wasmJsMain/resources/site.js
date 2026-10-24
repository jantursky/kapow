// The theme switch, the sidebar on small screens and the source panel for the Kapow gallery. sources.js is
// generated from the example files at build time, so the panel shows the file that runs in the stage.
(function () {
  const sources = window.KAPOW_SOURCES || {}
  const root = document.documentElement

  // Theme. index.html has already set data-theme before the first paint; the Compose viewports watch it.
  const STORAGE_KEY = 'kapow-theme'
  const toggle = document.querySelector('.theme-toggle')
  const savedTheme = () => {
    try {
      return localStorage.getItem(STORAGE_KEY)
    } catch {
      return null
    }
  }
  const applyTheme = (theme) => {
    root.dataset.theme = theme
    toggle?.setAttribute('aria-label', theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme')
  }
  applyTheme(root.dataset.theme === 'light' ? 'light' : 'dark')
  toggle?.addEventListener('click', () => {
    const next = root.dataset.theme === 'dark' ? 'light' : 'dark'
    applyTheme(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {}
  })
  // Follow the system until the visitor picks a theme.
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (event) => {
    if (!savedTheme()) applyTheme(event.matches ? 'dark' : 'light')
  })

  // The effect list is always open beside the content; on a phone it folds away behind its summary.
  const details = document.getElementById('sidebar-details')
  const narrow = matchMedia('(max-width: 900px)')
  const syncSidebar = () => {
    if (details) details.open = !narrow.matches
  }
  syncSidebar()
  narrow.addEventListener('change', syncSidebar)
  window.addEventListener('hashchange', () => {
    if (narrow.matches && details) details.open = false
  })

  // Kotlin highlighting.
  const KEYWORDS = new Set(
    (
      'package import fun val var private internal public class enum object companion if else when ' +
      'return true false null for in is as by'
    ).split(' '),
  )

  const escape = (text) => text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // Comments, strings, annotations, numbers, then words. Good enough for short Kotlin files.
  const TOKEN = /(\/\*[\s\S]*?\*\/|\/\/[^\n]*)|("(?:\\.|[^"\\])*")|(@\w+)|(\b\d[\d_]*(?:\.\d+)?f?\b)|(\b[A-Za-z_]\w*\b)/g

  const highlight = (code) => {
    let out = ''
    let last = 0
    for (const match of code.matchAll(TOKEN)) {
      out += escape(code.slice(last, match.index))
      const [text, comment, string, annotation, number, word] = match
      const end = match.index + text.length
      let kind = null
      if (comment) kind = 'comment'
      else if (string) kind = 'string'
      else if (annotation) kind = 'annotation'
      else if (number) kind = 'number'
      else if (word && KEYWORDS.has(word)) kind = 'keyword'
      else if (word && /^[A-Z]/.test(word)) kind = 'type'
      else if (word && /^\s*[({]/.test(code.slice(end, end + 2))) kind = 'call'
      out += kind ? `<span class="tok-${kind}">${escape(text)}</span>` : escape(text)
      last = end
    }
    return out + escape(code.slice(last))
  }

  // The import block folds away behind a toggle, so the code that matters is what the panel opens on.
  const render = (code) => {
    const lines = code.split('\n')
    const first = lines.findIndex((line) => line.startsWith('import '))
    if (first < 0) return highlight(code)
    let last = first
    while (last + 1 < lines.length && (lines[last + 1].startsWith('import ') || lines[last + 1] === '')) last++
    while (lines[last] === '') last--
    const count = lines.slice(first, last + 1).filter((line) => line.startsWith('import ')).length
    const before = lines.slice(0, first).join('\n') + '\n'
    const imports = lines.slice(first, last + 1).join('\n')
    const after = lines.slice(last + 1).join('\n')
    return (
      highlight(before) +
      `<span class="imports" data-collapsed="true"><button type="button" class="imports-toggle" aria-expanded="false">` +
      `+ ${count} imports</button><span class="imports-body">${highlight(imports)}</span></span>` +
      highlight(after)
    )
  }

  const CHECK = '<svg class="check" viewBox="0 0 16 16" aria-hidden="true"><path d="M3.5 8.5 6.5 11.5 12.5 4.5"/></svg>'

  const copyText = async (button, text) => {
    const label = button.dataset.label || button.textContent.trim()
    button.dataset.label = label
    try {
      await navigator.clipboard.writeText(text)
      button.classList.add('copied')
      button.innerHTML = `${CHECK}<span>Copied</span>`
    } catch {
      button.textContent = 'Copy failed'
    }
    clearTimeout(button.resetTimer)
    button.resetTimer = setTimeout(() => {
      button.classList.remove('copied')
      button.textContent = label
    }, 1600)
  }

  // Called by the Kotlin side whenever the page changes. An empty name, or a file without source, hides the panel.
  const panel = document.getElementById('source')
  window.kapowShowSource = (file) => {
    if (!panel) return
    const code = sources[file]
    panel.hidden = !code
    if (!code) return
    panel.innerHTML = `
      <div class="source-bar">
        <span class="source-name">${escape(file)}</span>
        <button type="button" class="copy">Copy</button>
      </div>
      <pre tabindex="0"><code>${render(code)}</code></pre>`
    panel.querySelector('.copy').addEventListener('click', (event) => copyText(event.currentTarget, code))
    panel.querySelector('code').addEventListener('click', (event) => {
      const button = event.target.closest('.imports-toggle')
      if (!button) return
      const block = button.parentElement
      const collapsed = block.dataset.collapsed !== 'true'
      block.dataset.collapsed = String(collapsed)
      button.setAttribute('aria-expanded', String(!collapsed))
      button.textContent = button.textContent.replace(/^[+−]/, collapsed ? '+' : '−')
    })
  }
})()

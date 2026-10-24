// The theme switch, the sidebar on small screens, the version and install line, and the source panel for the
// Kapow gallery. sources.js is generated at build time from the Gradle version and the example files, so the
// page names the release it was built from and the panel shows the file that runs in the stage.
(function () {
  const sources = window.KAPOW_SOURCES || {}
  const version = window.KAPOW_VERSION || ''
  const root = document.documentElement

  // The version beside the brand and in the install line.
  for (const element of document.querySelectorAll('[data-version]')) element.textContent = version
  for (const element of document.querySelectorAll('[data-version-tag]')) element.hidden = !version

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
      // Without a clipboard (an insecure origin, an old browser) this throws inside the try and says so.
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

  const install = document.getElementById('install-line')
  document.getElementById('copy-install')?.addEventListener('click', (event) => {
    copyText(event.currentTarget, install?.textContent ?? '')
  })

  // The source panel has two tabs: the usage with the values of the controls as they stand now, and the file that
  // runs in the stage. The Kotlin side calls kapowShowSource when the page changes (an empty name, or a file
  // without source, leaves only the usage) and kapowShowUsage after every change of a control.
  const TAB_KEY = 'kapow-source-tab'
  const panel = document.getElementById('source')
  let file = ''
  let usage = ''
  let tab = 'usage'
  try {
    if (localStorage.getItem(TAB_KEY) === 'file') tab = 'file'
  } catch {}

  const usageCode = () => panel?.querySelector('[data-pane="usage"] code')

  const paint = () => {
    if (!panel) return
    const hasFile = Boolean(sources[file])
    panel.hidden = !hasFile && !usage
    if (panel.hidden) return
    const active = tab === 'file' && hasFile ? 'file' : hasFile && !usage ? 'file' : 'usage'
    const tabs = []
    if (usage || !hasFile) tabs.push(['usage', 'Usage'])
    if (hasFile) tabs.push(['file', file])
    panel.innerHTML = `
      <div class="source-bar">
        <div class="source-tabs" role="tablist" aria-label="Source">
          ${tabs
            .map(
              ([id, label]) =>
                `<button type="button" role="tab" id="source-tab-${id}" aria-controls="source-pane-${id}" ` +
                `aria-selected="${id === active}" data-tab="${id}" class="source-tab${id === 'file' ? ' source-name' : ''}">${escape(label)}</button>`,
            )
            .join('')}
        </div>
        <button type="button" class="copy">Copy</button>
      </div>
      ${tabs
        .map(
          ([id]) =>
            `<div role="tabpanel" id="source-pane-${id}" aria-labelledby="source-tab-${id}" data-pane="${id}" ${id === active ? '' : 'hidden'}>` +
            `<pre tabindex="0"><code>${render(id === 'file' ? sources[file] : usage)}</code></pre></div>`,
        )
        .join('')}`
    for (const button of panel.querySelectorAll('[data-tab]')) {
      button.addEventListener('click', () => {
        tab = button.dataset.tab
        try {
          localStorage.setItem(TAB_KEY, tab)
        } catch {}
        paint()
      })
    }
    panel.querySelector('.copy').addEventListener('click', (event) => {
      copyText(event.currentTarget, active === 'file' ? sources[file] : usage)
    })
  }

  const toggleImports = (event) => {
    const button = event.target.closest('.imports-toggle')
    if (!button) return
    const block = button.parentElement
    const collapsed = block.dataset.collapsed !== 'true'
    block.dataset.collapsed = String(collapsed)
    button.setAttribute('aria-expanded', String(!collapsed))
    button.textContent = button.textContent.replace(/^[+−]/, collapsed ? '+' : '−')
  }

  window.kapowShowSource = (name) => {
    file = name
    usage = ''
    paint()
  }

  window.kapowShowUsage = (code) => {
    const first = !usage
    usage = code
    if (first || !usageCode()) {
      paint()
      return
    }
    // Later changes only swap the text, so a selection, a scroll position and the focus in the panel stay put.
    usageCode().innerHTML = render(usage)
  }

  panel?.addEventListener('click', toggleImports)
})()

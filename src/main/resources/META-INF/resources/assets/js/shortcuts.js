/**
 * Tamarind Keyboard Shortcuts Manager
 * Displays available keyboard shortcuts in a modal
 */

const KeyboardShortcuts = {
  shortcuts: {
    'Global': [
      { key: '?', description: 'Show keyboard shortcuts', available: 'all' },
      { key: 'Esc', description: 'Close modal/dialog', available: 'all' },
      { key: 'Ctrl/Cmd+K', description: 'Open command palette (coming soon)', available: 'all', comingSoon: true }
    ],
    'Notebook': [
      { key: 'Shift+Enter', description: 'Run current cell', available: 'notebook' },
      { key: 'Ctrl/Cmd+Enter', description: 'Run selection (or current cell)', available: 'notebook' },
      { key: 'Ctrl+Space', description: 'Trigger autocomplete', available: 'notebook' },
      { key: 'Ctrl/Cmd+S', description: 'Save notebook', available: 'notebook' },
      { key: 'Ctrl/Cmd+/', description: 'Comment/uncomment line', available: 'notebook' },
      { key: 'Tab', description: 'Indent selection', available: 'notebook' },
      { key: 'Shift+Tab', description: 'Outdent selection', available: 'notebook' }
    ],
    'Navigation': [
      { key: 'Ctrl/Cmd+Home', description: 'Scroll to top', available: 'all' },
      { key: 'Ctrl/Cmd+End', description: 'Scroll to bottom', available: 'all' },
      { key: 'Tab', description: 'Navigate between elements', available: 'all' },
      { key: 'Shift+Tab', description: 'Navigate backwards', available: 'all' }
    ],
    'Results': [
      { key: 'Double-click', description: 'Expand/collapse cell content', available: 'notebook' },
      { key: 'Ctrl/Cmd+C', description: 'Copy selected text', available: 'all' }
    ]
  },

  modal: null,
  searchInput: null,
  resultsContainer: null,

  /**
   * Initialize shortcuts system
   */
  init() {
    this.createModal();
    this.attachGlobalListener();
    this.addStyles();
  },

  /**
   * Create the shortcuts modal
   */
  createModal() {
    if (this.modal) return;

    this.modal = document.createElement('div');
    this.modal.id = 'shortcuts-modal';
    this.modal.className = 'shortcuts-modal';
    this.modal.innerHTML = `
      <div class="shortcuts-overlay" onclick="KeyboardShortcuts.close()"></div>
      <div class="shortcuts-content">
        <div class="shortcuts-header">
          <h2>⌨️ Keyboard Shortcuts</h2>
          <button class="shortcuts-close" onclick="KeyboardShortcuts.close()" aria-label="Close">×</button>
        </div>

        <div class="shortcuts-search">
          <input
            type="search"
            id="shortcuts-search-input"
            placeholder="Search shortcuts..."
            aria-label="Search shortcuts"
          />
        </div>

        <div class="shortcuts-body" id="shortcuts-results"></div>

        <div class="shortcuts-footer">
          <span class="shortcuts-tip">💡 Tip: Press <kbd>?</kbd> anytime to show this help</span>
        </div>
      </div>
    `;

    document.body.appendChild(this.modal);

    this.searchInput = document.getElementById('shortcuts-search-input');
    this.resultsContainer = document.getElementById('shortcuts-results');

    // Attach search listener
    if (this.searchInput) {
      this.searchInput.addEventListener('input', (e) => {
        this.filterShortcuts(e.target.value);
      });
    }
  },

  /**
   * Attach global keyboard listener
   */
  attachGlobalListener() {
    document.addEventListener('keydown', (e) => {
      // Open shortcuts with '?' unless in input field
      if (e.key === '?' && !this.isInputFocused()) {
        e.preventDefault();
        this.show();
      }

      // Close with Escape
      if (e.key === 'Escape' && this.isOpen()) {
        e.preventDefault();
        this.close();
      }
    });
  },

  /**
   * Check if an input field is focused
   */
  isInputFocused() {
    const active = document.activeElement;
    return active && (
      active.tagName === 'INPUT' ||
      active.tagName === 'TEXTAREA' ||
      active.isContentEditable ||
      active.closest('.CodeMirror')
    );
  },

  /**
   * Check if modal is open
   */
  isOpen() {
    return this.modal && this.modal.classList.contains('active');
  },

  /**
   * Show the shortcuts modal
   */
  show() {
    if (!this.modal) this.init();

    this.modal.classList.add('active');
    this.renderShortcuts();

    // Focus search input for accessibility
    setTimeout(() => {
      if (this.searchInput) {
        this.searchInput.value = '';
        this.searchInput.focus();
      }
    }, 100);
  },

  /**
   * Close the shortcuts modal
   */
  close() {
    if (this.modal) {
      this.modal.classList.remove('active');
    }
  },

  /**
   * Render all shortcuts
   */
  renderShortcuts(filter = '') {
    if (!this.resultsContainer) return;

    const lowerFilter = filter.toLowerCase();
    let html = '';
    let visibleCount = 0;

    for (const [category, shortcuts] of Object.entries(this.shortcuts)) {
      const filteredShortcuts = filter
        ? shortcuts.filter(s =>
            s.key.toLowerCase().includes(lowerFilter) ||
            s.description.toLowerCase().includes(lowerFilter)
          )
        : shortcuts;

      if (filteredShortcuts.length === 0) continue;

      visibleCount += filteredShortcuts.length;

      html += `
        <div class="shortcuts-category">
          <h3 class="shortcuts-category-title">${category}</h3>
          <div class="shortcuts-list">
            ${filteredShortcuts.map(s => this.renderShortcut(s)).join('')}
          </div>
        </div>
      `;
    }

    if (visibleCount === 0) {
      html = `
        <div class="shortcuts-empty">
          <p>No shortcuts found matching "${this.escapeHtml(filter)}"</p>
          <p class="shortcuts-empty-hint">Try a different search term</p>
        </div>
      `;
    }

    this.resultsContainer.innerHTML = html;
  },

  /**
   * Render a single shortcut
   */
  renderShortcut(shortcut) {
    const keys = shortcut.key.split('+').map(k => k.trim());
    const keysHtml = keys.map(k => `<kbd>${this.escapeHtml(k)}</kbd>`).join('<span class="key-separator">+</span>');

    return `
      <div class="shortcut-item ${shortcut.comingSoon ? 'coming-soon' : ''}">
        <div class="shortcut-keys">
          ${keysHtml}
          ${shortcut.comingSoon ? '<span class="coming-soon-badge">Soon</span>' : ''}
        </div>
        <div class="shortcut-description">${this.escapeHtml(shortcut.description)}</div>
      </div>
    `;
  },

  /**
   * Filter shortcuts by search term
   */
  filterShortcuts(query) {
    this.renderShortcuts(query);
  },

  /**
   * HTML escape utility
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  },

  /**
   * Add keyboard shortcut
   */
  addShortcut(category, key, description, available = 'all') {
    if (!this.shortcuts[category]) {
      this.shortcuts[category] = [];
    }
    this.shortcuts[category].push({ key, description, available });
  },

  /**
   * Add CSS styles
   */
  addStyles() {
    if (document.getElementById('keyboard-shortcuts-styles')) return;

    const style = document.createElement('style');
    style.id = 'keyboard-shortcuts-styles';
    style.textContent = `
      .shortcuts-modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 10001;
        display: none;
        align-items: center;
        justify-content: center;
        padding: 20px;
      }

      .shortcuts-modal.active {
        display: flex;
      }

      .shortcuts-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(2px);
      }

      .shortcuts-content {
        position: relative;
        background: white;
        border-radius: 12px;
        max-width: 700px;
        width: 100%;
        max-height: 85vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        animation: shortcuts-slide-in 0.3s ease-out;
      }

      @keyframes shortcuts-slide-in {
        from {
          opacity: 0;
          transform: translateY(-30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .shortcuts-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 24px 24px 16px;
        border-bottom: 1px solid #e9ecef;
      }

      .shortcuts-header h2 {
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        color: #333;
      }

      .shortcuts-close {
        background: none;
        border: none;
        font-size: 32px;
        line-height: 1;
        color: #999;
        cursor: pointer;
        padding: 0;
        width: 36px;
        height: 36px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        transition: all 0.2s;
      }

      .shortcuts-close:hover {
        background: #f8f9fa;
        color: #333;
      }

      .shortcuts-search {
        padding: 16px 24px;
        border-bottom: 1px solid #e9ecef;
      }

      .shortcuts-search input {
        width: 100%;
        padding: 10px 16px;
        border: 1px solid #dee2e6;
        border-radius: 8px;
        font-size: 14px;
        transition: border-color 0.2s;
      }

      .shortcuts-search input:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }

      .shortcuts-body {
        flex: 1;
        overflow-y: auto;
        padding: 20px 24px;
      }

      .shortcuts-category {
        margin-bottom: 28px;
      }

      .shortcuts-category:last-child {
        margin-bottom: 0;
      }

      .shortcuts-category-title {
        margin: 0 0 12px 0;
        font-size: 15px;
        font-weight: 600;
        color: #667eea;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .shortcuts-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .shortcut-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        background: #f8f9fa;
        border-radius: 8px;
        transition: background 0.2s;
      }

      .shortcut-item:hover {
        background: #e9ecef;
      }

      .shortcut-item.coming-soon {
        opacity: 0.6;
      }

      .shortcut-keys {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
      }

      .shortcut-keys kbd {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 28px;
        height: 28px;
        padding: 4px 8px;
        background: white;
        border: 1px solid #dee2e6;
        border-radius: 6px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
        font-size: 12px;
        font-weight: 600;
        color: #495057;
        box-shadow: 0 2px 0 rgba(0, 0, 0, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
        text-transform: capitalize;
      }

      .key-separator {
        color: #adb5bd;
        font-weight: 500;
        font-size: 14px;
      }

      .coming-soon-badge {
        display: inline-block;
        padding: 2px 8px;
        background: #fff3cd;
        color: #856404;
        border-radius: 12px;
        font-size: 11px;
        font-weight: 600;
        margin-left: 4px;
      }

      .shortcut-description {
        flex: 1;
        margin-left: 16px;
        font-size: 14px;
        color: #495057;
        line-height: 1.4;
      }

      .shortcuts-empty {
        text-align: center;
        padding: 40px 20px;
        color: #6c757d;
      }

      .shortcuts-empty p {
        margin: 0 0 8px 0;
      }

      .shortcuts-empty-hint {
        font-size: 13px;
        color: #adb5bd;
      }

      .shortcuts-footer {
        padding: 16px 24px;
        border-top: 1px solid #e9ecef;
        background: #f8f9fa;
        border-radius: 0 0 12px 12px;
      }

      .shortcuts-tip {
        display: block;
        text-align: center;
        font-size: 13px;
        color: #6c757d;
      }

      .shortcuts-tip kbd {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 20px;
        height: 20px;
        padding: 2px 6px;
        background: white;
        border: 1px solid #dee2e6;
        border-radius: 4px;
        font-family: monospace;
        font-size: 12px;
        font-weight: 600;
        color: #495057;
        margin: 0 2px;
      }

      /* Responsive */
      @media (max-width: 768px) {
        .shortcuts-modal {
          padding: 10px;
        }

        .shortcuts-content {
          max-width: 100%;
          max-height: 90vh;
        }

        .shortcuts-header {
          padding: 20px 16px 12px;
        }

        .shortcuts-header h2 {
          font-size: 18px;
        }

        .shortcuts-search {
          padding: 12px 16px;
        }

        .shortcuts-body {
          padding: 16px;
        }

        .shortcut-item {
          flex-direction: column;
          align-items: flex-start;
          gap: 8px;
        }

        .shortcut-description {
          margin-left: 0;
        }
      }

      /* Print styles */
      @media print {
        .shortcuts-modal {
          position: static;
          display: block;
        }

        .shortcuts-overlay {
          display: none;
        }

        .shortcuts-content {
          max-height: none;
          box-shadow: none;
        }

        .shortcuts-close,
        .shortcuts-search,
        .shortcuts-footer {
          display: none;
        }
      }
    `;

    document.head.appendChild(style);
  }
};

// Make globally available
window.KeyboardShortcuts = KeyboardShortcuts;

// Initialize on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    KeyboardShortcuts.init();
  });
} else {
  KeyboardShortcuts.init();
}


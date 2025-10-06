/**
 * Tamarind UI Utilities
 * Common utility functions for the web UI
 */

const TamarindUtils = {
  /**
   * HTML escape utility to prevent XSS
   */
  escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = String(text);
    return div.innerHTML;
  },

  /**
   * Debounce function calls
   */
  debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  },

  /**
   * Throttle function calls
   */
  throttle(func, limit) {
    let inThrottle;
    return function(...args) {
      if (!inThrottle) {
        func.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  },

  /**
   * Format bytes to human-readable size
   */
  formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  },

  /**
   * Format duration in milliseconds to human-readable string
   */
  formatDuration(ms) {
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
    if (ms < 3600000) return `${(ms / 60000).toFixed(2)}m`;
    return `${(ms / 3600000).toFixed(2)}h`;
  },

  /**
   * Format number with thousand separators
   */
  formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  },

  /**
   * Deep clone an object
   */
  deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
  },

  /**
   * Generate a unique ID
   */
  generateId(prefix = 'id') {
    return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  },

  /**
   * Check if value is empty (null, undefined, empty string, empty array, empty object)
   */
  isEmpty(value) {
    if (value == null) return true;
    if (typeof value === 'string') return value.trim().length === 0;
    if (Array.isArray(value)) return value.length === 0;
    if (typeof value === 'object') return Object.keys(value).length === 0;
    return false;
  },

  /**
   * Validate SQL table/column name
   */
  validateIdentifier(name) {
    if (!name || typeof name !== 'string') {
      return { valid: false, error: 'Name is required' };
    }

    const trimmed = name.trim();

    if (trimmed.length === 0) {
      return { valid: false, error: 'Name cannot be empty' };
    }

    if (trimmed.length > 128) {
      return { valid: false, error: 'Name must be less than 128 characters' };
    }

    // SQL identifier rules: start with letter or underscore, contain only alphanumeric and underscore
    if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(trimmed)) {
      return {
        valid: false,
        error: 'Name must start with letter or underscore and contain only alphanumeric characters and underscores'
      };
    }

    // Check for reserved SQL keywords
    const reservedWords = [
      'SELECT', 'FROM', 'WHERE', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP',
      'ALTER', 'TABLE', 'INDEX', 'VIEW', 'JOIN', 'INNER', 'OUTER', 'LEFT', 'RIGHT',
      'ON', 'AS', 'AND', 'OR', 'NOT', 'NULL', 'TRUE', 'FALSE', 'IN', 'EXISTS'
    ];

    if (reservedWords.includes(trimmed.toUpperCase())) {
      return {
        valid: false,
        error: `"${trimmed}" is a reserved SQL keyword. Please choose a different name.`
      };
    }

    return { valid: true };
  },

  /**
   * Copy text to clipboard
   */
  async copyToClipboard(text) {
    try {
      await navigator.clipboard.writeText(text);
      return { success: true };
    } catch (err) {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      document.body.appendChild(textArea);
      textArea.select();

      try {
        document.execCommand('copy');
        document.body.removeChild(textArea);
        return { success: true };
      } catch (err) {
        document.body.removeChild(textArea);
        return { success: false, error: err.message };
      }
    }
  },

  /**
   * Download data as file
   */
  downloadFile(data, filename, mimeType = 'text/plain') {
    const blob = new Blob([data], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  },

  /**
   * Parse CSV value according to RFC 4180
   */
  parseCsvValue(value) {
    if (value == null) return '';
    const str = String(value);
    const needsEscaping = str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r');

    if (needsEscaping) {
      const escaped = str.replace(/"/g, '""');
      return `"${escaped}"`;
    }

    return str;
  },

  /**
   * Convert array of objects to CSV string
   */
  arrayToCsv(data, columns) {
    if (!data || data.length === 0) return '';

    const cols = columns || Object.keys(data[0]);
    const header = cols.map(c => this.parseCsvValue(c)).join(',');
    const rows = data.map(row =>
      cols.map(col => this.parseCsvValue(row[col])).join(',')
    );

    return [header, ...rows].join('\n');
  },

  /**
   * Show toast notification
   */
  showToast(message, type = 'info', duration = 3000) {
    const toast = document.getElementById('toast');
    if (!toast) {
      console.warn('Toast element not found');
      return;
    }

    toast.textContent = message;
    toast.className = `toast ${type} show`;

    setTimeout(() => {
      toast.className = 'toast';
    }, duration);
  },

  /**
   * Setup modal keyboard navigation and focus trap
   */
  setupModalAccessibility(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;

    const focusableElements = modal.querySelectorAll(
      'button, input, select, textarea, a[href], [tabindex]:not([tabindex="-1"])'
    );

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    const trapFocus = (e) => {
      if (e.key === 'Tab') {
        if (e.shiftKey && document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        } else if (!e.shiftKey && document.activeElement === lastElement) {
          e.preventDefault();
          firstElement.focus();
        }
      } else if (e.key === 'Escape') {
        const closeBtn = modal.querySelector('[data-close]');
        if (closeBtn) closeBtn.click();
      }
    };

    modal.addEventListener('keydown', trapFocus);

    // Focus first element when modal is shown
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'class' && modal.classList.contains('active')) {
          setTimeout(() => firstElement.focus(), 100);
        }
      });
    });

    observer.observe(modal, { attributes: true });

    return () => {
      modal.removeEventListener('keydown', trapFocus);
      observer.disconnect();
    };
  },

  /**
   * Get query parameters from URL
   */
  getQueryParams() {
    const params = {};
    const queryString = window.location.search.substring(1);
    const pairs = queryString.split('&');

    for (let pair of pairs) {
      const [key, value] = pair.split('=');
      if (key) {
        params[decodeURIComponent(key)] = decodeURIComponent(value || '');
      }
    }

    return params;
  },

  /**
   * Update URL query parameters without page reload
   */
  updateQueryParams(params) {
    const url = new URL(window.location);

    for (const [key, value] of Object.entries(params)) {
      if (value === null || value === undefined || value === '') {
        url.searchParams.delete(key);
      } else {
        url.searchParams.set(key, value);
      }
    }

    window.history.replaceState({}, '', url);
  },

  /**
   * Format SQL query (requires sql-formatter library)
   */
  formatSql(sql) {
    if (typeof window.sqlFormatter === 'undefined') {
      console.warn('sql-formatter not loaded');
      return sql;
    }

    try {
      return window.sqlFormatter.format(sql, {
        language: 'sql',
        uppercase: true,
        linesBetweenQueries: 2
      });
    } catch (e) {
      console.error('SQL formatting error:', e);
      return sql;
    }
  },

  /**
   * Check if element is in viewport
   */
  isInViewport(element) {
    const rect = element.getBoundingClientRect();
    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
      rect.right <= (window.innerWidth || document.documentElement.clientWidth)
    );
  },

  /**
   * Smooth scroll to element
   */
  scrollToElement(element, offset = 0) {
    if (!element) return;

    const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
    const offsetPosition = elementPosition - offset;

    window.scrollTo({
      top: offsetPosition,
      behavior: 'smooth'
    });
  },

  /**
   * Local storage with JSON support and error handling
   */
  storage: {
    get(key, defaultValue = null) {
      try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : defaultValue;
      } catch (e) {
        console.error('Storage get error:', e);
        return defaultValue;
      }
    },

    set(key, value) {
      try {
        localStorage.setItem(key, JSON.stringify(value));
        return true;
      } catch (e) {
        console.error('Storage set error:', e);
        return false;
      }
    },

    remove(key) {
      try {
        localStorage.removeItem(key);
        return true;
      } catch (e) {
        console.error('Storage remove error:', e);
        return false;
      }
    },

    clear() {
      try {
        localStorage.clear();
        return true;
      } catch (e) {
        console.error('Storage clear error:', e);
        return false;
      }
    }
  }
};

  /**
   * Loading state manager for global operations
   */
  LoadingManager: {
    activeRequests: new Set(),
    overlay: null,

    init() {
      if (this.overlay) return;

      this.overlay = document.createElement('div');
      this.overlay.id = 'global-loading-overlay';
      this.overlay.innerHTML = `
        <div class="loading-content">
          <div class="loading-spinner"></div>
          <div class="loading-text">Loading...</div>
        </div>
      `;
      document.body.appendChild(this.overlay);

      // Add styles
      const style = document.createElement('style');
      style.textContent = `
        #global-loading-overlay {
          display: none;
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          z-index: 9999;
          align-items: center;
          justify-content: center;
        }

        #global-loading-overlay.active {
          display: flex;
        }

        .loading-content {
          text-align: center;
          color: white;
        }

        .loading-spinner {
          width: 50px;
          height: 50px;
          border: 4px solid rgba(255, 255, 255, 0.3);
          border-top-color: white;
          border-radius: 50%;
          animation: spinner-spin 1s linear infinite;
          margin: 0 auto 15px;
        }

        @keyframes spinner-spin {
          to { transform: rotate(360deg); }
        }

        .loading-text {
          font-size: 16px;
          font-weight: 500;
        }
      `;
      document.head.appendChild(style);
    },

    start(operationId = 'default', message = 'Loading...') {
      this.init();
      this.activeRequests.add(operationId);
      this.updateUI(message);
    },

    stop(operationId = 'default') {
      this.activeRequests.delete(operationId);
      this.updateUI();
    },

    updateUI(message = 'Loading...') {
      if (!this.overlay) return;

      const isLoading = this.activeRequests.size > 0;
      this.overlay.classList.toggle('active', isLoading);

      if (isLoading) {
        const textEl = this.overlay.querySelector('.loading-text');
        if (textEl) textEl.textContent = message;
      }
    },

    stopAll() {
      this.activeRequests.clear();
      this.updateUI();
    }
  }
};

// Make available globally
window.TamarindUtils = TamarindUtils;

// Initialize loading manager on DOM ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    TamarindUtils.LoadingManager.init();
  });
} else {
  TamarindUtils.LoadingManager.init();
}


/**
 * Tamarind Error Handler
 * Provides user-friendly error messages with actionable suggestions
 */

const ErrorHandler = {
  errorMap: {
    'RATE_LIMIT_EXCEEDED': {
      title: 'Too Many Requests',
      message: 'You\'ve made too many requests in a short time.',
      suggestions: [
        'Wait 1 minute before trying again',
        'Consider reducing the frequency of your queries',
        'Contact your administrator if you need higher limits'
      ],
      icon: '⏱️',
      severity: 'warning'
    },

    'SQL_ERROR': {
      title: 'SQL Query Error',
      message: 'There\'s an error in your SQL query.',
      suggestions: [
        'Check for missing or extra semicolons',
        'Verify table and column names exist',
        'Review SQL syntax documentation',
        'Try running DESCRIBE table_name to see available columns'
      ],
      icon: '⚠️',
      severity: 'error'
    },

    'FORBIDDEN_OPERATION': {
      title: 'Operation Not Allowed',
      message: 'You don\'t have permission to perform this action.',
      suggestions: [
        'Contact your administrator for access',
        'Check if you\'re logged in with the correct account',
        'Review the permissions required for this operation'
      ],
      icon: '🚫',
      severity: 'error'
    },

    'NETWORK_ERROR': {
      title: 'Connection Error',
      message: 'Cannot connect to the Tamarind server.',
      suggestions: [
        'Check your internet connection',
        'Verify the server is running',
        'Try refreshing the page',
        'Contact support if the issue persists'
      ],
      icon: '📡',
      severity: 'error'
    },

    'INVALID_REQUEST': {
      title: 'Invalid Request',
      message: 'The request contains invalid data.',
      suggestions: [
        'Check that all required fields are filled',
        'Verify the format of your input',
        'Review the error details below for specifics'
      ],
      icon: '❌',
      severity: 'error'
    },

    'FILE_TOO_LARGE': {
      title: 'File Too Large',
      message: 'The file you\'re trying to upload exceeds the size limit.',
      suggestions: [
        'Maximum file size is 100MB',
        'Try compressing the file',
        'Split the data into smaller files',
        'Use S3 registration for larger datasets'
      ],
      icon: '📦',
      severity: 'warning'
    },

    'UNAUTHORIZED': {
      title: 'Authentication Required',
      message: 'Your session has expired or you\'re not logged in.',
      suggestions: [
        'You\'ll be redirected to the login page',
        'Please log in again to continue'
      ],
      icon: '🔒',
      severity: 'info'
    },

    'INTERNAL_ERROR': {
      title: 'Server Error',
      message: 'An unexpected error occurred on the server.',
      suggestions: [
        'Try again in a moment',
        'If the problem persists, contact support',
        'Include the error details below when reporting'
      ],
      icon: '🔧',
      severity: 'error'
    }
  },

  /**
   * Handle an error and display user-friendly message
   */
  handle(error) {
    const errorCode = error.code || error.error?.code || 'INTERNAL_ERROR';
    const mapped = this.errorMap[errorCode] || this.getDefaultError(error);

    this.show(mapped, error.details || error.message, error);
  },

  /**
   * Get default error mapping for unknown error codes
   */
  getDefaultError(error) {
    return {
      title: 'Error',
      message: error.message || 'An unexpected error occurred',
      suggestions: [
        'Try again',
        'Refresh the page if the problem persists',
        'Contact support if you continue experiencing issues'
      ],
      icon: '❌',
      severity: 'error'
    };
  },

  /**
   * Show error modal with details
   */
  show(errorInfo, details, originalError) {
    // Remove any existing error modals
    const existing = document.querySelector('.error-modal');
    if (existing) existing.remove();

    const modal = document.createElement('div');
    modal.className = 'error-modal active';
    modal.innerHTML = `
      <div class="error-modal-overlay" onclick="this.parentElement.remove()"></div>
      <div class="error-modal-content ${errorInfo.severity}">
        <div class="error-header">
          <span class="error-icon">${errorInfo.icon}</span>
          <h2 class="error-title">${errorInfo.title}</h2>
          <button class="error-close" onclick="this.closest('.error-modal').remove()" aria-label="Close">×</button>
        </div>

        <div class="error-body">
          <p class="error-message">${errorInfo.message}</p>

          ${errorInfo.suggestions && errorInfo.suggestions.length > 0 ? `
            <div class="error-suggestions">
              <strong>What you can do:</strong>
              <ul>
                ${errorInfo.suggestions.map(s => `<li>${s}</li>`).join('')}
              </ul>
            </div>
          ` : ''}

          ${details ? `
            <details class="error-details">
              <summary>Technical details</summary>
              <pre>${this.escapeHtml(String(details))}</pre>
            </details>
          ` : ''}
        </div>

        <div class="error-actions">
          <button class="btn-primary" onclick="this.closest('.error-modal').remove()">
            Got it
          </button>
          ${originalError && this.shouldShowRetry(originalError) ? `
            <button class="btn-secondary" onclick="ErrorHandler.retry('${originalError.operation}')">
              Try Again
            </button>
          ` : ''}
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Focus the close button for accessibility
    setTimeout(() => {
      const closeBtn = modal.querySelector('.error-close');
      if (closeBtn) closeBtn.focus();
    }, 100);

    // Add styles if not already present
    this.addStyles();
  },

  /**
   * Determine if retry button should be shown
   */
  shouldShowRetry(error) {
    const retryableCodes = ['NETWORK_ERROR', 'INTERNAL_ERROR', 'RATE_LIMIT_EXCEEDED'];
    return retryableCodes.includes(error.code);
  },

  /**
   * Retry the failed operation
   */
  retry(operation) {
    // Close the modal
    const modal = document.querySelector('.error-modal');
    if (modal) modal.remove();

    // Emit custom event that components can listen to
    const event = new CustomEvent('tamarind:retry', { detail: { operation } });
    document.dispatchEvent(event);
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
   * Add error modal styles
   */
  addStyles() {
    if (document.getElementById('error-handler-styles')) return;

    const style = document.createElement('style');
    style.id = 'error-handler-styles';
    style.textContent = `
      .error-modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 10000;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
      }

      .error-modal-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(2px);
      }

      .error-modal-content {
        position: relative;
        background: white;
        border-radius: 12px;
        max-width: 500px;
        width: 100%;
        max-height: 80vh;
        overflow-y: auto;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        animation: error-modal-slide-in 0.3s ease-out;
      }

      @keyframes error-modal-slide-in {
        from {
          opacity: 0;
          transform: translateY(-20px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .error-modal-content.error {
        border-top: 4px solid #dc3545;
      }

      .error-modal-content.warning {
        border-top: 4px solid #ffc107;
      }

      .error-modal-content.info {
        border-top: 4px solid #17a2b8;
      }

      .error-header {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 24px 24px 16px;
        border-bottom: 1px solid #e9ecef;
      }

      .error-icon {
        font-size: 32px;
        line-height: 1;
      }

      .error-title {
        flex: 1;
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #333;
      }

      .error-close {
        background: none;
        border: none;
        font-size: 28px;
        line-height: 1;
        color: #999;
        cursor: pointer;
        padding: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        transition: all 0.2s;
      }

      .error-close:hover {
        background: #f8f9fa;
        color: #333;
      }

      .error-body {
        padding: 20px 24px;
      }

      .error-message {
        margin: 0 0 16px 0;
        font-size: 15px;
        line-height: 1.6;
        color: #495057;
      }

      .error-suggestions {
        margin: 16px 0;
        padding: 16px;
        background: #f8f9fa;
        border-radius: 8px;
        border-left: 3px solid #667eea;
      }

      .error-suggestions strong {
        display: block;
        margin-bottom: 8px;
        color: #333;
        font-size: 14px;
      }

      .error-suggestions ul {
        margin: 0;
        padding-left: 20px;
      }

      .error-suggestions li {
        margin: 6px 0;
        font-size: 14px;
        line-height: 1.5;
        color: #495057;
      }

      .error-details {
        margin: 16px 0 0 0;
        border: 1px solid #e9ecef;
        border-radius: 6px;
        overflow: hidden;
      }

      .error-details summary {
        padding: 12px;
        background: #f8f9fa;
        cursor: pointer;
        font-weight: 500;
        font-size: 14px;
        color: #495057;
        user-select: none;
      }

      .error-details summary:hover {
        background: #e9ecef;
      }

      .error-details pre {
        margin: 0;
        padding: 16px;
        background: #f8f9fa;
        font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
        font-size: 12px;
        line-height: 1.5;
        overflow-x: auto;
        color: #495057;
      }

      .error-actions {
        display: flex;
        gap: 12px;
        padding: 16px 24px 24px;
        justify-content: flex-end;
      }

      .error-actions button {
        padding: 10px 20px;
        border: none;
        border-radius: 6px;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
      }

      .error-actions .btn-primary {
        background: #667eea;
        color: white;
      }

      .error-actions .btn-primary:hover {
        background: #5568d3;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      }

      .error-actions .btn-secondary {
        background: #e9ecef;
        color: #495057;
      }

      .error-actions .btn-secondary:hover {
        background: #dee2e6;
      }

      /* Responsive */
      @media (max-width: 576px) {
        .error-modal {
          padding: 10px;
        }

        .error-modal-content {
          max-width: 100%;
        }

        .error-header {
          padding: 20px 16px 12px;
        }

        .error-body {
          padding: 16px;
        }

        .error-actions {
          flex-direction: column;
          padding: 12px 16px 20px;
        }

        .error-actions button {
          width: 100%;
        }
      }
    `;

    document.head.appendChild(style);
  },

  /**
   * Show a simple toast notification (for non-critical errors)
   */
  toast(message, type = 'error') {
    if (typeof TamarindUtils !== 'undefined' && TamarindUtils.showToast) {
      TamarindUtils.showToast(message, type);
    } else {
      console.error('Toast notification:', message);
    }
  }
};

// Make globally available
window.ErrorHandler = ErrorHandler;

// Initialize styles on load
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    ErrorHandler.addStyles();
  });
} else {
  ErrorHandler.addStyles();
}


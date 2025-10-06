/**
 * Tamarind Dashboard JavaScript
 * Handles all dashboard interactions, API calls, and UI updates
 */

class TamarindDashboard {
  constructor() {
    this.authToken = null;
    this.currentUser = null;
    this.refreshInterval = null;
  }

  async initialize() {
    // Check authentication
    this.authToken = localStorage.getItem('tamarind_token');
    const userStr = localStorage.getItem('tamarind_user');

    if (!this.authToken || !userStr) {
      window.location.href = '/index.html';
      return;
    }

    try {
      this.currentUser = JSON.parse(userStr);
      this.updateUserDisplay();
      this.setupRoleBasedUI();
      await this.loadOverviewData();
      this.startAutoRefresh();
    } catch (e) {
      console.error('Failed to initialize dashboard:', e);
      window.location.href = '/index.html';
    }
  }

  updateUserDisplay() {
    document.getElementById('userName').textContent = this.currentUser.username;
    document.getElementById('userRole').textContent = this.currentUser.role || 'USER';
    document.getElementById('userAvatar').textContent =
      this.currentUser.username.substring(0, 1).toUpperCase();
  }

  setupRoleBasedUI() {
    const isAdmin = this.currentUser.role === 'ADMIN';

    // Hide user management for non-admins
    if (!isAdmin) {
      const userNavItem = document.querySelector('[data-page="users"]');
      if (userNavItem) {
        userNavItem.style.display = 'none';
      }
    }

    // Set role-specific welcome message
    const welcomeMsg = isAdmin ?
      'Welcome, Administrator!' :
      'Welcome to Tamarind Dashboard';

    if (document.getElementById('welcomeMessage')) {
      document.getElementById('welcomeMessage').textContent = welcomeMsg;
    }
  }

  startAutoRefresh() {
    // Auto-refresh monitoring data every 30 seconds
    this.refreshInterval = setInterval(() => {
      const activePage = document.querySelector('.page.active');
      if (activePage && activePage.id === 'monitoring-page') {
        this.loadMonitoring();
      }
    }, 30000);
  }

  stopAutoRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  showPage(pageName, el) {
    // Update nav items
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.remove('active');
    });
    if (el) {
      el.classList.add('active');
    } else {
      const nav = document.querySelector(`[data-page="${pageName}"]`);
      if (nav) nav.classList.add('active');
    }

    // Update pages
    document.querySelectorAll('.page').forEach(page => {
      page.classList.remove('active');
    });
    const pageEl = document.getElementById(pageName + '-page');
    if (pageEl) pageEl.classList.add('active');

    // Load page data
    switch (pageName) {
      case 'overview':
        this.loadOverviewData();
        break;
      case 'users':
        this.loadUsers();
        break;
      case 'datasources':
        this.loadTables();
        break;
      case 'monitoring':
        this.loadMonitoring();
        break;
      case 'config':
        this.loadConfig();
        break;
    }
  }

  showAlert(message, type = 'info') {
    const alert = document.getElementById('alert');
    alert.className = `alert alert-${type} show`;
    alert.textContent = message;
    setTimeout(() => {
      alert.classList.remove('show');
    }, 5000);
  }

  logout() {
    this.stopAutoRefresh();
    localStorage.removeItem('tamarind_token');
    localStorage.removeItem('tamarind_user');
    window.location.href = '/index.html';
  }

  async apiCall(endpoint, options = {}) {
    const defaultOptions = {
      headers: {
        'Authorization': `Bearer ${this.authToken}`,
        'Content-Type': 'application/json'
      }
    };

    const mergedOptions = {
      ...defaultOptions,
      ...options,
      headers: {
        ...defaultOptions.headers,
        ...options.headers
      }
    };

    try {
      const response = await fetch(endpoint, mergedOptions);

      if (response.status === 401) {
        // Token expired or invalid
        this.logout();
        return null;
      }

      return response;
    } catch (error) {
      console.error('API call failed:', error);
      throw error;
    }
  }

  async loadOverviewData() {
    try {
      const [infoResponse, statsResponse] = await Promise.all([
        this.apiCall('/api/v1/system/info'),
        this.apiCall('/api/v1/system/stats')
      ]);

      if (infoResponse.ok) {
        const infoData = await infoResponse.json();
        this.displaySystemInfo(infoData);
      }

      if (statsResponse.ok) {
        const statsData = await statsResponse.json();
        this.displayStats(statsData);
      }
    } catch (error) {
      console.error('Error loading overview data:', error);
      this.showAlert('Failed to load overview data', 'error');
    }
  }

  displaySystemInfo(response) {
    const data = response.data || response;
    const container = document.getElementById('systemInfo');

    let html = '<div>';
    html += `<div class="config-item"><span class="config-key">Application</span><span class="config-value">${data.name || 'Tamarind'} v${data.version || 'Unknown'}</span></div>`;
    html += `<div class="config-item"><span class="config-key">Status</span><span class="config-value">${data.status || 'Running'}</span></div>`;

    if (data.storage) {
      html += `<div class="config-item"><span class="config-key">OLTP Backend</span><span class="config-value">${data.storage.oltp_backend}</span></div>`;
      html += `<div class="config-item"><span class="config-key">Ephemeral Backend</span><span class="config-value">${data.storage.ephemeral_backend}</span></div>`;
    }

    if (data.users) {
      html += `<div class="config-item"><span class="config-key">Total Users</span><span class="config-value">${data.users.total_users}</span></div>`;
    }

    html += '</div>';
    container.innerHTML = html;
  }

  displayStats(response) {
    const data = response.data || response;

    document.getElementById('totalUsers').textContent = data.total_users || 0;

    if (data.jvm) {
      const uptimeHours = Math.floor(data.jvm.uptime_seconds / 3600);
      document.getElementById('systemUptime').textContent = uptimeHours;
      document.getElementById('memoryUsage').textContent = `${Math.round(data.jvm.heap_usage_percent)}%`;
    }
  }

  async loadUsers() {
    const container = document.getElementById('usersTableContainer');
    container.innerHTML = '<div class="loading"><div class="spinner"></div>Loading users...</div>';

    try {
      const response = await this.apiCall('/api/v1/auth/users');

      if (!response.ok) {
        throw new Error('Failed to load users');
      }

      const result = await response.json();
      this.displayUsers(result.data);
    } catch (error) {
      console.error('Error loading users:', error);
      container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error loading users</div>';
    }
  }

  displayUsers(users) {
    const container = document.getElementById('usersTableContainer');

    if (!users || users.length === 0) {
      container.innerHTML = '<div style="padding: 20px; text-align: center; color: #666;">No users found</div>';
      return;
    }

    const isAdmin = this.currentUser.role === 'ADMIN';

    let html = '<table><thead><tr>';
    html += '<th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Last Login</th>';
    if (isAdmin) html += '<th>Actions</th>';
    html += '</tr></thead><tbody>';

    users.forEach(user => {
      html += '<tr>';
      html += `<td>${this.escapeHtml(user.username)}</td>`;
      html += `<td>${this.escapeHtml(user.email) || '-'}</td>`;
      html += `<td><span class="role-badge">${user.role || 'USER'}</span></td>`;
      html += `<td><span class="status-badge ${user.enabled ? 'status-active' : 'status-inactive'}">${user.enabled ? 'Active' : 'Inactive'}</span></td>`;
      html += `<td>${user.lastLogin ? new Date(user.lastLogin).toLocaleString() : 'Never'}</td>`;

      if (isAdmin && user.username !== this.currentUser.username) {
        html += '<td>';
        html += `<button class="btn-secondary" onclick="dashboard.changeUserRole('${this.escapeHtml(user.username)}', '${user.role}')">Change Role</button>`;
        html += `<button class="btn-danger" onclick="dashboard.deleteUser('${this.escapeHtml(user.username)}')">Delete</button>`;
        html += '</td>';
      } else if (isAdmin) {
        html += '<td><em style="color: #999;">Current user</em></td>';
      }

      html += '</tr>';
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  async changeUserRole(username, currentRole) {
    const newRole = prompt(`Change role for ${username}. Current: ${currentRole}\nEnter new role (ADMIN, USER, or VIEWER):`, currentRole);

    if (!newRole || newRole.toUpperCase() === currentRole) {
      return;
    }

    try {
      const response = await this.apiCall(`/api/v1/auth/users/${username}/role`, {
        method: 'PUT',
        body: JSON.stringify({role: newRole.toUpperCase()})
      });

      if (response.ok) {
        this.showAlert(`Role updated successfully for ${username}`, 'success');
        this.loadUsers();
      } else {
        const error = await response.json();
        this.showAlert(error.message || 'Failed to update role', 'error');
      }
    } catch (error) {
      console.error('Error updating role:', error);
      this.showAlert('Failed to update role', 'error');
    }
  }

  async deleteUser(username) {
    if (!confirm(`Are you sure you want to delete user "${username}"?`)) {
      return;
    }

    try {
      const response = await this.apiCall(`/api/v1/auth/users/${username}`, {
        method: 'DELETE'
      });

      if (response.ok) {
        this.showAlert(`User ${username} deleted successfully`, 'success');
        this.loadUsers();
      } else {
        const error = await response.json();
        this.showAlert(error.message || 'Failed to delete user', 'error');
      }
    } catch (error) {
      console.error('Error deleting user:', error);
      this.showAlert('Failed to delete user', 'error');
    }
  }

  async loadMonitoring() {
    try {
      const [statsResponse, healthResponse] = await Promise.all([
        this.apiCall('/api/v1/system/stats'),
        this.apiCall('/api/v1/system/health')
      ]);

      if (statsResponse.ok) {
        const statsData = await statsResponse.json();
        this.displayMonitoringStats(statsData.data || statsData);
      }

      if (healthResponse.ok) {
        const healthData = await healthResponse.json();
        this.displayHealthChecks(healthData.data || healthData);
      }
    } catch (error) {
      console.error('Error loading monitoring data:', error);
      this.showAlert('Failed to load monitoring data', 'error');
    }
  }

  displayMonitoringStats(data) {
    if (data.jvm) {
      document.getElementById('jvmUptime').textContent = Math.round(data.jvm.uptime_seconds);
      document.getElementById('heapUsed').textContent =
        `${Math.round(data.jvm.heap_used_mb)} / ${Math.round(data.jvm.heap_max_mb)} MB`;
      document.getElementById('heapPercent').textContent =
        `${Math.round(data.jvm.heap_usage_percent)}%`;

      // Display thread info if available
      if (data.jvm.threads) {
        const threadInfo = `${data.jvm.threads.thread_count} threads (${data.jvm.threads.daemon_thread_count} daemon)`;
        if (document.getElementById('threadInfo')) {
          document.getElementById('threadInfo').textContent = threadInfo;
        }
      }

      // Display GC info if available
      if (data.jvm.garbage_collectors && document.getElementById('gcInfo')) {
        const gcSummary = data.jvm.garbage_collectors
          .map(gc => `${gc.name}: ${gc.collection_count} collections`)
          .join(', ');
        document.getElementById('gcInfo').textContent = gcSummary;
      }
    }
  }

  displayHealthChecks(data) {
    document.getElementById('healthStatus').textContent = data.status || 'UNKNOWN';
    document.getElementById('healthTimestamp').textContent =
      data.timestamp ? new Date(data.timestamp).toLocaleString() : '-';

    const container = document.getElementById('healthChecks');

    if (data.checks) {
      let html = '<div style="padding: 20px;">';
      for (const [key, value] of Object.entries(data.checks)) {
        html += `<div class="config-item">`;
        html += `<span class="config-key">${this.escapeHtml(key)}</span>`;
        html += `<span class="config-value">${this.escapeHtml(value)}</span>`;
        html += `</div>`;
      }
      html += '</div>';
      container.innerHTML = html;
    } else {
      container.innerHTML = '<div style="padding: 20px; text-align: center; color: #666;">No health checks available</div>';
    }
  }

  async loadConfig() {
    const container = document.getElementById('configContainer');
    container.innerHTML = '<div class="loading"><div class="spinner"></div>Loading configuration...</div>';

    try {
      const response = await this.apiCall('/api/v1/system/config');

      if (!response.ok) {
        throw new Error('Failed to load configuration');
      }

      const result = await response.json();
      this.displayConfig(result.data || result);
    } catch (error) {
      console.error('Error loading configuration:', error);
      container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error loading configuration</div>';
    }
  }

  displayConfig(config) {
    const container = document.getElementById('configContainer');
    let html = '<div>';

    const renderObject = (obj, prefix = '') => {
      for (const [key, value] of Object.entries(obj)) {
        const fullKey = prefix ? `${prefix}.${key}` : key;

        if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
          renderObject(value, fullKey);
        } else {
          html += `<div class="config-item">`;
          html += `<span class="config-key">${this.escapeHtml(fullKey)}</span>`;
          html += `<span class="config-value">${this.escapeHtml(JSON.stringify(value))}</span>`;
          html += `</div>`;
        }
      }
    };

    renderObject(config);
    html += '</div>';
    container.innerHTML = html;
  }

  async loadTables() {
    const container = document.getElementById('tablesContainer');
    container.innerHTML = '<div class="loading"><div class="spinner"></div>Loading tables...</div>';

    try {
      const response = await this.apiCall('/api/v1/datasources/tables');

      if (!response.ok) {
        throw new Error('Failed to load tables');
      }

      const result = await response.json();
      this.displayTables(result.data);
    } catch (error) {
      console.error('Error loading tables:', error);
      container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error loading tables</div>';
    }
  }

  displayTables(tables) {
    const container = document.getElementById('tablesContainer');

    if (!tables || tables.length === 0) {
      container.innerHTML = '<div style="padding: 20px; text-align: center; color: #666;">No tables found</div>';
      return;
    }

    let html = '<table><thead><tr><th>Table Name</th><th>Actions</th></tr></thead><tbody>';

    tables.forEach(table => {
      html += '<tr>';
      html += `<td>${this.escapeHtml(table)}</td>`;
      html += `<td><button class="btn-secondary" onclick="dashboard.queryTable('${this.escapeHtml(table)}')">Query in Notebook</button></td>`;
      html += '</tr>';
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  }

  queryTable(tableName) {
    localStorage.setItem('tamarind_query_template', `SELECT * FROM ${tableName} LIMIT 100`);
    window.location.href = '/notebook.html';
  }
}

// Global functions for inline event handlers (to be migrated to event delegation)
function refreshUsers() {
  if (window.dashboard) {
    dashboard.loadUsers();
  }
}

function refreshTables() {
  if (window.dashboard) {
    dashboard.loadTables();
  }
}

function refreshMonitoring() {
  if (window.dashboard) {
    dashboard.loadMonitoring();
  }
}

function refreshConfig() {
  if (window.dashboard) {
    dashboard.loadConfig();
  }
}

let selectedFile = null;

function handleFileSelect(event) {
  selectedFile = event.target.files[0];
  if (selectedFile) {
    const uploadArea = document.getElementById('uploadArea');
    const sizeInMB = (selectedFile.size / (1024 * 1024)).toFixed(2);
    uploadArea.innerHTML = `
      <div class="upload-icon">📄</div>
      <div style="font-size: 16px; color: #333; margin-bottom: 5px;">
        ${escapeHtml(selectedFile.name)}
      </div>
      <div style="font-size: 13px; color: #999;">
        ${sizeInMB} MB
      </div>
    `;

    // Enable upload button
    const uploadBtn = document.getElementById('uploadBtn');
    if (uploadBtn) {
      uploadBtn.disabled = false;
    }
  }
}

async function uploadFile() {
  if (!selectedFile) {
    if (window.dashboard) {
      dashboard.showAlert('Please select a file first', 'error');
    }
    return;
  }

  const tableName = document.getElementById('uploadTableName').value.trim();
  const uploadBtn = document.getElementById('uploadBtn');
  const progressBar = document.getElementById('uploadProgress');
  const progressFill = document.getElementById('uploadProgressFill');

  try {
    uploadBtn.disabled = true;
    uploadBtn.textContent = 'Uploading...';
    progressBar.style.display = 'block';
    progressFill.style.width = '0%';

    // Simulate progress (real progress would require chunked upload or XHR)
    const progressInterval = setInterval(() => {
      const currentWidth = parseFloat(progressFill.style.width) || 0;
      if (currentWidth < 90) {
        progressFill.style.width = (currentWidth + 10) + '%';
      }
    }, 200);

    const formData = new FormData();
    formData.append('file', selectedFile);
    if (tableName) {
      formData.append('tableName', tableName);
    }

    const token = localStorage.getItem('tamarind_token');
    const response = await fetch('/api/v1/datasources/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    clearInterval(progressInterval);
    progressFill.style.width = '100%';

    if (response.status === 401) {
      localStorage.removeItem('tamarind_token');
      localStorage.removeItem('tamarind_user');
      window.location.href = '/index.html';
      return;
    }

    const result = await response.json();

    if (response.ok) {
      if (window.dashboard) {
        dashboard.showAlert(`File uploaded successfully${tableName ? ` as table '${tableName}'` : ''}`, 'success');
      }

      // Reset form
      selectedFile = null;
      document.getElementById('uploadTableName').value = '';
      document.getElementById('fileInput').value = '';
      const uploadArea = document.getElementById('uploadArea');
      uploadArea.innerHTML = `
        <div class="upload-icon">📁</div>
        <div style="font-size: 16px; color: #333; margin-bottom: 5px;">
          Click or drag file to upload
        </div>
        <div style="font-size: 13px; color: #999;">
          Supports: Parquet, CSV, JSON (Max 100MB)
        </div>
      `;

      // Refresh tables list
      if (window.dashboard) {
        dashboard.loadTables();
      }
    } else {
      const errorMessage = result.message || result.error?.message || 'Upload failed';
      if (window.dashboard) {
        dashboard.showAlert(errorMessage, 'error');
      }
    }
  } catch (error) {
    console.error('Upload error:', error);
    if (window.dashboard) {
      dashboard.showAlert('Upload failed: ' + error.message, 'error');
    }
  } finally {
    uploadBtn.disabled = false;
    uploadBtn.textContent = 'Upload File';
    setTimeout(() => {
      progressBar.style.display = 'none';
      progressFill.style.width = '0%';
    }, 1000);
  }
}

async function registerS3Table() {
  const tableName = document.getElementById('s3TableName').value.trim();
  const s3Path = document.getElementById('s3Path').value.trim();

  if (!tableName) {
    if (window.dashboard) {
      dashboard.showAlert('Please enter a table name', 'error');
    }
    return;
  }

  if (!s3Path) {
    if (window.dashboard) {
      dashboard.showAlert('Please enter an S3 path', 'error');
    }
    return;
  }

  if (!s3Path.startsWith('s3://')) {
    if (window.dashboard) {
      dashboard.showAlert('S3 path must start with s3://', 'error');
    }
    return;
  }

  try {
    const token = localStorage.getItem('tamarind_token');
    const response = await fetch('/api/v1/datasources/s3/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ tableName, s3Path })
    });

    if (response.status === 401) {
      localStorage.removeItem('tamarind_token');
      localStorage.removeItem('tamarind_user');
      window.location.href = '/index.html';
      return;
    }

    const result = await response.json();

    if (response.ok) {
      if (window.dashboard) {
        dashboard.showAlert(`S3 table '${tableName}' registered successfully`, 'success');
      }

      // Reset form
      document.getElementById('s3TableName').value = '';
      document.getElementById('s3Path').value = '';

      // Refresh tables list
      if (window.dashboard) {
        dashboard.loadTables();
      }
    } else {
      const errorMessage = result.message || result.error?.message || 'Registration failed';
      if (window.dashboard) {
        dashboard.showAlert(errorMessage, 'error');
      }
    }
  } catch (error) {
    console.error('S3 registration error:', error);
    if (window.dashboard) {
      dashboard.showAlert('Registration failed: ' + error.message, 'error');
    }
  }
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Drag and drop support for file upload
document.addEventListener('DOMContentLoaded', () => {
  const uploadArea = document.getElementById('uploadArea');
  if (uploadArea) {
    uploadArea.addEventListener('dragover', (e) => {
      e.preventDefault();
      uploadArea.classList.add('drag-over');
    });

    uploadArea.addEventListener('dragleave', () => {
      uploadArea.classList.remove('drag-over');
    });

    uploadArea.addEventListener('drop', (e) => {
      e.preventDefault();
      uploadArea.classList.remove('drag-over');

      const files = e.dataTransfer.files;
      if (files.length > 0) {
        document.getElementById('fileInput').files = files;
        handleFileSelect({ target: { files } });
      }
    });
  }
});

// Initialize dashboard when DOM is ready
let dashboard;
document.addEventListener('DOMContentLoaded', () => {
  dashboard = new TamarindDashboard();
  dashboard.initialize();
});

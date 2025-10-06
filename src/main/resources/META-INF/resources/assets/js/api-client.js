/**
 * Tamarind API Client
 * Centralized API communication with error handling, token management, and retries
 */

class TamarindApiClient {
  constructor() {
    this.baseUrl = '';
    this.defaultHeaders = {
      'Content-Type': 'application/json'
    };
  }

  getAuthToken() {
    return localStorage.getItem('tamarind_token');
  }

  setAuthToken(token) {
    localStorage.setItem('tamarind_token', token);
  }

  clearAuth() {
    localStorage.removeItem('tamarind_token');
    localStorage.removeItem('tamarind_user');
  }

  async request(endpoint, options = {}) {
    const token = this.getAuthToken();
    const requestId = `req_${Date.now()}_${Math.random()}`;

    const config = {
      ...options,
      headers: {
        ...this.defaultHeaders,
        ...options.headers
      }
    };

    if (token && !options.skipAuth) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // Start loading indicator unless explicitly disabled
    if (!options.skipLoading && typeof TamarindUtils !== 'undefined') {
      const loadingMessage = options.loadingMessage || 'Loading...';
      TamarindUtils.LoadingManager.start(requestId, loadingMessage);
    }

    try {
      const response = await fetch(this.baseUrl + endpoint, config);

      // Handle 401 - redirect to login
      if (response.status === 401) {
        this.clearAuth();
        window.location.href = '/index.html';
        throw new Error('Session expired. Please login again.');
      }

      // Parse response
      const contentType = response.headers.get('content-type');
      let data;

      if (contentType && contentType.includes('application/json')) {
        try {
          data = await response.json();
        } catch (e) {
          throw new Error('Invalid JSON response from server');
        }
      } else {
        const text = await response.text();
        throw new Error(`Unexpected response type: ${contentType}`);
      }

      // Handle error responses
      if (!response.ok) {
        const errorMessage = data.message || data.error?.message || `Request failed with status ${response.status}`;
        const error = new Error(errorMessage);
        error.status = response.status;
        error.code = data.code || data.error?.code;
        error.details = data.details || data.error?.details;
        error.response = data;
        throw error;
      }

      return data;
    } catch (error) {
      // Network errors
      if (error.name === 'TypeError' && error.message === 'Failed to fetch') {
        error.code = 'NETWORK_ERROR';
      }

      // Show error handler if available and not explicitly disabled
      if (!options.skipErrorHandler && typeof ErrorHandler !== 'undefined') {
        ErrorHandler.handle(error);
      }

      throw error;
    } finally {
      // Stop loading indicator
      if (!options.skipLoading && typeof TamarindUtils !== 'undefined') {
        TamarindUtils.LoadingManager.stop(requestId);
      }
    }
  }

  async get(endpoint, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'GET'
    });
  }

  async post(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async put(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(body)
    });
  }

  async delete(endpoint, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'DELETE'
    });
  }

  // Auth endpoints
  async login(username, password) {
    const response = await this.post('/api/v1/auth/login', { username, password }, { skipAuth: true });
    if (response.data && response.data.token) {
      this.setAuthToken(response.data.token);
      if (response.data.user) {
        localStorage.setItem('tamarind_user', JSON.stringify(response.data.user));
      }
    }
    return response;
  }

  async register(username, email, password) {
    return this.post('/api/v1/auth/register', { username, email, password }, { skipAuth: true });
  }

  async listUsers() {
    return this.get('/api/v1/auth/users');
  }

  async changeUserRole(username, role) {
    return this.put(`/api/v1/auth/users/${username}/role`, { role });
  }

  async deleteUser(username) {
    return this.delete(`/api/v1/auth/users/${username}`);
  }

  // Query endpoints
  async executeQuery(sql, options = {}) {
    return this.post('/api/v1/query', { sql, options });
  }

  async cancelQuery(queryId) {
    return this.post(`/api/v1/query/${queryId}/cancel`);
  }

  // Data source endpoints
  async listTables() {
    return this.get('/api/v1/datasources/tables');
  }

  async uploadFile(file, tableName) {
    const formData = new FormData();
    formData.append('file', file);
    if (tableName) {
      formData.append('tableName', tableName);
    }

    const token = this.getAuthToken();
    const response = await fetch(this.baseUrl + '/api/v1/datasources/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });

    if (response.status === 401) {
      this.clearAuth();
      window.location.href = '/index.html';
      throw new Error('Session expired. Please login again.');
    }

    const data = await response.json();

    if (!response.ok) {
      const errorMessage = data.message || data.error?.message || 'Upload failed';
      const error = new Error(errorMessage);
      error.status = response.status;
      error.response = data;
      throw error;
    }

    return data;
  }

  async registerS3Table(tableName, s3Path, options = {}) {
    return this.post('/api/v1/datasources/s3/register', {
      tableName,
      s3Path,
      ...options
    });
  }

  // System endpoints
  async getSystemInfo() {
    return this.get('/api/v1/system/info');
  }

  async getSystemStats() {
    return this.get('/api/v1/system/stats');
  }

  async getSystemHealth() {
    return this.get('/api/v1/system/health');
  }

  async getSystemConfig() {
    return this.get('/api/v1/system/config');
  }
}

// Global instance
const apiClient = new TamarindApiClient();


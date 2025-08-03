import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const serviceAPI = {
  // Service Configuration
  getAllServices: () => api.get('/services'),
  getEnabledServices: () => api.get('/services/enabled'),
  getServiceById: (id) => api.get(`/services/${id}`),
  getServiceByName: (serviceName) => api.get(`/services/name/${serviceName}`),
  createService: (serviceConfig) => api.post('/services', serviceConfig),
  updateService: (id, serviceConfig) => api.put(`/services/${id}`, serviceConfig),
  deleteService: (id) => api.delete(`/services/${id}`),
  toggleServiceStatus: (id) => api.post(`/services/${id}/toggle`),
  validateFile: (validationRequest) => api.post('/services/validate-file', validationRequest),
  getServiceStats: () => api.get('/services/stats'),
};

export const ssoAPI = {
  // SSO Configuration
  getAllSsoConfigurations: () => api.get('/sso'),
  getEnabledSsoConfigurations: () => api.get('/sso/enabled'),
  getSsoConfigurationById: (id) => api.get(`/sso/${id}`),
  getSsoConfigurationByOrganization: (orgId) => api.get(`/sso/organization/${orgId}`),
  createSsoConfiguration: (ssoConfig) => api.post('/sso', ssoConfig),
  updateSsoConfiguration: (id, ssoConfig) => api.put(`/sso/${id}`, ssoConfig),
  deleteSsoConfiguration: (id) => api.delete(`/sso/${id}`),
  toggleSsoConfigurationStatus: (id) => api.post(`/sso/${id}/toggle`),
  testSsoConnection: (id) => api.post(`/sso/${id}/test`),
};

export const authAPI = {
  // Authentication
  login: (credentials) => api.post('/auth/login', credentials),
  loginWithSSO: (organizationId) => api.get(`/auth/sso/${organizationId}`),
  logout: () => api.post('/auth/logout'),
  refreshToken: () => api.post('/auth/refresh'),
  getCurrentUser: () => api.get('/auth/user'),
  checkAuth: () => api.get('/auth/check'),
};

// Add request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth data and redirect to login
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
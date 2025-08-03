import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const fileTransferAPI = {
  // Get all file transfers
  getAllFileTransfers: () => api.get('/file-transfers'),
  
  // Get file transfer by ID
  getFileTransferById: (id) => api.get(`/file-transfers/${id}`),
  
  // Get file transfers by service
  getFileTransfersByService: (serviceType) => api.get(`/file-transfers/service/${serviceType}`),
  
  // Get file transfers by status
  getFileTransfersByStatus: (status) => api.get(`/file-transfers/status/${status}`),
  
  // Get file transfers by direction
  getFileTransfersByDirection: (direction) => api.get(`/file-transfers/direction/${direction}`),
  
  // Get file transfers by service and status
  getFileTransfersByServiceAndStatus: (serviceType, status) => 
    api.get(`/file-transfers/service/${serviceType}/status/${status}`),
  
  // Get file transfers by date range
  getFileTransfersByDateRange: (startDate, endDate) => 
    api.get(`/file-transfers/date-range?startDate=${startDate}&endDate=${endDate}`),
  
  // Get file transfers by service and date range
  getFileTransfersByServiceAndDateRange: (serviceType, startDate, endDate) => 
    api.get(`/file-transfers/service/${serviceType}/date-range?startDate=${startDate}&endDate=${endDate}`),
  
  // Retry transfer
  retryTransfer: (id) => api.post(`/file-transfers/${id}/retry`),
  
  // Cancel transfer
  cancelTransfer: (id) => api.post(`/file-transfers/${id}/cancel`),
  
  // Get distinct service types
  getDistinctServiceTypes: () => api.get('/file-transfers/services'),
};

export default api;
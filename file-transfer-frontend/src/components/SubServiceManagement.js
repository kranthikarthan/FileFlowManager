import React, { useState, useEffect } from 'react';
import './SubServiceManagement.css';

const SubServiceManagement = ({ tenantId }) => {
  const [subServices, setSubServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingSubService, setEditingSubService] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [timeZoneInfo, setTimeZoneInfo] = useState(null);

  const [formData, setFormData] = useState({
    serviceName: '',
    subServiceName: '',
    tenantId: tenantId || '',
    inboundPath: '',
    outboundPath: '',
    enabled: true,
    cutOffTime: '23:59:59',
    cutOffTimeType: 'DAILY',
    schemaValidationEnabled: true,
    binaryFileBypass: true,
    maxRetries: 3,
    pollIntervalSeconds: 30,
    description: ''
  });

  const cutOffTimeTypes = [
    { value: 'DAILY', label: 'Daily' },
    { value: 'WEEKDAY_WEEKEND', label: 'Weekday/Weekend' },
    { value: 'INDIVIDUAL_DAYS', label: 'Individual Days' }
  ];

  const fileTypes = [
    'COBOL_FLAT_FILE',
    'BINARY_FILE', 
    'XML',
    'JSON',
    'CSV',
    'FIXED_WIDTH',
    'TEXT'
  ];

  useEffect(() => {
    if (tenantId) {
      fetchSubServices();
      fetchTimeZoneInfo();
    }
  }, [tenantId]);

  const fetchSubServices = async () => {
    try {
      const response = await fetch(`/api/sub-services/tenant/${tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setSubServices(data);
      } else {
        setError('Failed to fetch sub-services');
      }
    } catch (err) {
      setError('Error fetching sub-services: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchTimeZoneInfo = async () => {
    try {
      const response = await fetch(`/api/sub-services/tenant/${tenantId}/timezone-info`);
      if (response.ok) {
        const data = await response.json();
        setTimeZoneInfo(data);
      }
    } catch (err) {
      console.warn('Could not fetch timezone info:', err.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const url = editingSubService 
        ? `/api/sub-services/${editingSubService.id}`
        : '/api/sub-services';
      
      const method = editingSubService ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        await fetchSubServices();
        resetForm();
        setError('');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to save sub-service');
      }
    } catch (err) {
      setError('Error saving sub-service: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (subService) => {
    setEditingSubService(subService);
    setFormData({ ...subService });
    setShowCreateForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this sub-service?')) {
      return;
    }

    try {
      const response = await fetch(`/api/sub-services/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        await fetchSubServices();
        setError('');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to delete sub-service');
      }
    } catch (err) {
      setError('Error deleting sub-service: ' + err.message);
    }
  };

  const resetForm = () => {
    setFormData({
      serviceName: '',
      subServiceName: '',
      tenantId: tenantId || '',
      inboundPath: '',
      outboundPath: '',
      enabled: true,
      cutOffTime: '23:59:59',
      cutOffTimeType: 'DAILY',
      schemaValidationEnabled: true,
      binaryFileBypass: true,
      maxRetries: 3,
      pollIntervalSeconds: 30,
      description: ''
    });
    setEditingSubService(null);
    setShowCreateForm(false);
  };

  const getCutOffInfo = async (serviceName, subServiceName) => {
    try {
      const response = await fetch(
        `/api/sub-services/tenant/${tenantId}/service/${serviceName}/subservice/${subServiceName}/cut-off-info`
      );
      if (response.ok) {
        return await response.json();
      }
    } catch (err) {
      console.warn('Could not fetch cut-off info:', err.message);
    }
    return null;
  };

  const formatCutOffStatus = (subService) => {
    const [cutOffInfo, setCutOffInfo] = useState(null);
    
    useEffect(() => {
      getCutOffInfo(subService.serviceName, subService.subServiceName)
        .then(setCutOffInfo);
    }, [subService.serviceName, subService.subServiceName]);

    if (!cutOffInfo) return subService.cutOffTime;

    return (
      <div className="cut-off-status">
        <div className="cut-off-time">{cutOffInfo.cutOffTime}</div>
        <div className={`status ${cutOffInfo.inProcessingWindow ? 'active' : 'inactive'}`}>
          {cutOffInfo.inProcessingWindow ? 'Active' : 'Cut-off Passed'}
        </div>
        {cutOffInfo.inProcessingWindow && (
          <div className="time-remaining">
            {cutOffInfo.timeUntilCutOffHours}h {cutOffInfo.timeUntilCutOffMinutes % 60}m remaining
          </div>
        )}
      </div>
    );
  };

  if (loading && subServices.length === 0) {
    return <div className="loading">Loading sub-services...</div>;
  }

  return (
    <div className="sub-service-management">
      <div className="header">
        <h2>Sub-Service Management</h2>
        {timeZoneInfo && (
          <div className="timezone-info">
            <span className="timezone-label">Tenant Timezone:</span>
            <span className="timezone-value">{timeZoneInfo.displayName}</span>
            <span className="current-time">{timeZoneInfo.currentTime}</span>
          </div>
        )}
        <button 
          className="btn btn-primary"
          onClick={() => setShowCreateForm(true)}
        >
          Add Sub-Service
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="sub-services-grid">
        {subServices.map(subService => (
          <div key={subService.id} className={`sub-service-card ${!subService.enabled ? 'disabled' : ''}`}>
            <div className="sub-service-header">
              <h3>{subService.serviceName} / {subService.subServiceName}</h3>
              <div className="sub-service-actions">
                <button 
                  className="btn btn-sm btn-secondary"
                  onClick={() => handleEdit(subService)}
                >
                  Edit
                </button>
                <button 
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDelete(subService.id)}
                >
                  Delete
                </button>
              </div>
            </div>
            
            <div className="sub-service-details">
              <div className="detail-row">
                <span className="label">Status:</span>
                <span className={`status ${subService.enabled ? 'enabled' : 'disabled'}`}>
                  {subService.enabled ? 'Enabled' : 'Disabled'}
                </span>
              </div>
              
              <div className="detail-row">
                <span className="label">Inbound Path:</span>
                <span className="value">{subService.inboundPath}</span>
              </div>
              
              <div className="detail-row">
                <span className="label">Outbound Path:</span>
                <span className="value">{subService.outboundPath}</span>
              </div>
              
              <div className="detail-row">
                <span className="label">Cut-off Time:</span>
                <span className="value">{formatCutOffStatus(subService)}</span>
              </div>
              
              <div className="detail-row">
                <span className="label">Schema Validation:</span>
                <span className={`status ${subService.schemaValidationEnabled ? 'enabled' : 'disabled'}`}>
                  {subService.schemaValidationEnabled ? 'Enabled' : 'Disabled'}
                </span>
              </div>

              {subService.description && (
                <div className="detail-row">
                  <span className="label">Description:</span>
                  <span className="value">{subService.description}</span>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {showCreateForm && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3>{editingSubService ? 'Edit Sub-Service' : 'Create New Sub-Service'}</h3>
              <button className="btn btn-close" onClick={resetForm}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="sub-service-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Service Name *</label>
                  <input
                    type="text"
                    name="serviceName"
                    value={formData.serviceName}
                    onChange={handleInputChange}
                    required
                  />
                </div>
                
                <div className="form-group">
                  <label>Sub-Service Name *</label>
                  <input
                    type="text"
                    name="subServiceName"
                    value={formData.subServiceName}
                    onChange={handleInputChange}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Inbound Path *</label>
                  <input
                    type="text"
                    name="inboundPath"
                    value={formData.inboundPath}
                    onChange={handleInputChange}
                    required
                  />
                </div>
                
                <div className="form-group">
                  <label>Outbound Path *</label>
                  <input
                    type="text"
                    name="outboundPath"
                    value={formData.outboundPath}
                    onChange={handleInputChange}
                    required
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Cut-off Time</label>
                  <input
                    type="time"
                    name="cutOffTime"
                    value={formData.cutOffTime}
                    onChange={handleInputChange}
                    step="1"
                  />
                </div>
                
                <div className="form-group">
                  <label>Cut-off Time Type</label>
                  <select
                    name="cutOffTimeType"
                    value={formData.cutOffTimeType}
                    onChange={handleInputChange}
                  >
                    {cutOffTimeTypes.map(type => (
                      <option key={type.value} value={type.value}>
                        {type.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Max Retries</label>
                  <input
                    type="number"
                    name="maxRetries"
                    value={formData.maxRetries}
                    onChange={handleInputChange}
                    min="0"
                    max="10"
                  />
                </div>
                
                <div className="form-group">
                  <label>Poll Interval (seconds)</label>
                  <input
                    type="number"
                    name="pollIntervalSeconds"
                    value={formData.pollIntervalSeconds}
                    onChange={handleInputChange}
                    min="5"
                    max="300"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows="3"
                />
              </div>

              <div className="form-checkboxes">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="enabled"
                    checked={formData.enabled}
                    onChange={handleInputChange}
                  />
                  Enabled
                </label>
                
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="schemaValidationEnabled"
                    checked={formData.schemaValidationEnabled}
                    onChange={handleInputChange}
                  />
                  Schema Validation Enabled
                </label>
                
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="binaryFileBypass"
                    checked={formData.binaryFileBypass}
                    onChange={handleInputChange}
                  />
                  Binary File Bypass
                </label>
              </div>

              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Saving...' : (editingSubService ? 'Update' : 'Create')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default SubServiceManagement;
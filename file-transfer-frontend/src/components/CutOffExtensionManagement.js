import React, { useState, useEffect } from 'react';
import './CutOffExtensionManagement.css';

const CutOffExtensionManagement = ({ tenantId }) => {
  const [extensions, setExtensions] = useState([]);
  const [subServices, setSubServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showRequestForm, setShowRequestForm] = useState(false);
  const [timeZoneInfo, setTimeZoneInfo] = useState(null);

  const [formData, setFormData] = useState({
    tenantId: tenantId || '',
    serviceName: '',
    subServiceName: '',
    extensionDate: '',
    originalCutOffTime: '',
    extendedCutOffTime: '',
    reason: '',
    priority: 'NORMAL',
    notes: ''
  });

  const priorities = [
    { value: 'LOW', label: 'Low', color: '#28a745' },
    { value: 'NORMAL', label: 'Normal', color: '#007bff' },
    { value: 'HIGH', label: 'High', color: '#ffc107' },
    { value: 'CRITICAL', label: 'Critical', color: '#dc3545' }
  ];

  const statuses = [
    { value: 'PENDING', label: 'Pending', color: '#ffc107' },
    { value: 'APPROVED', label: 'Approved', color: '#28a745' },
    { value: 'REJECTED', label: 'Rejected', color: '#dc3545' },
    { value: 'ACTIVE', label: 'Active', color: '#17a2b8' },
    { value: 'EXPIRED', label: 'Expired', color: '#6c757d' }
  ];

  useEffect(() => {
    if (tenantId) {
      fetchExtensions();
      fetchSubServices();
      fetchTimeZoneInfo();
    }
  }, [tenantId]);

  const fetchExtensions = async () => {
    try {
      const response = await fetch(`/api/cutoff-extensions/pending/tenant/${tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setExtensions(data);
      } else {
        setError('Failed to fetch extensions');
      }
    } catch (err) {
      setError('Error fetching extensions: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchSubServices = async () => {
    try {
      const response = await fetch(`/api/sub-services/tenant/${tenantId}`);
      if (response.ok) {
        const data = await response.json();
        setSubServices(data);
      }
    } catch (err) {
      console.warn('Could not fetch sub-services:', err.message);
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
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Auto-populate original cut-off time when service/subservice is selected
    if (name === 'serviceName' || name === 'subServiceName') {
      const selectedSubService = subServices.find(
        ss => ss.serviceName === (name === 'serviceName' ? value : formData.serviceName) &&
              ss.subServiceName === (name === 'subServiceName' ? value : formData.subServiceName)
      );
      if (selectedSubService) {
        setFormData(prev => ({
          ...prev,
          originalCutOffTime: selectedSubService.cutOffTime || '23:59:59'
        }));
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await fetch('/api/cutoff-extensions/request', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        await fetchExtensions();
        resetForm();
        setError('');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to request extension');
      }
    } catch (err) {
      setError('Error requesting extension: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (extensionId) => {
    try {
      const response = await fetch(`/api/cutoff-extensions/${extensionId}/approve`, {
        method: 'POST'
      });

      if (response.ok) {
        await fetchExtensions();
        setError('');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to approve extension');
      }
    } catch (err) {
      setError('Error approving extension: ' + err.message);
    }
  };

  const handleReject = async (extensionId) => {
    const rejectionReason = prompt('Please provide a reason for rejection:');
    if (!rejectionReason) return;

    try {
      const response = await fetch(`/api/cutoff-extensions/${extensionId}/reject`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ rejectionReason }),
      });

      if (response.ok) {
        await fetchExtensions();
        setError('');
      } else {
        const errorData = await response.json();
        setError(errorData.error || 'Failed to reject extension');
      }
    } catch (err) {
      setError('Error rejecting extension: ' + err.message);
    }
  };

  const resetForm = () => {
    setFormData({
      tenantId: tenantId || '',
      serviceName: '',
      subServiceName: '',
      extensionDate: '',
      originalCutOffTime: '',
      extendedCutOffTime: '',
      reason: '',
      priority: 'NORMAL',
      notes: ''
    });
    setShowRequestForm(false);
  };

  const getStatusBadge = (status) => {
    const statusConfig = statuses.find(s => s.value === status) || statuses[0];
    return (
      <span 
        className="status-badge" 
        style={{ backgroundColor: statusConfig.color }}
      >
        {statusConfig.label}
      </span>
    );
  };

  const getPriorityBadge = (priority) => {
    const priorityConfig = priorities.find(p => p.value === priority) || priorities[1];
    return (
      <span 
        className="priority-badge" 
        style={{ backgroundColor: priorityConfig.color }}
      >
        {priorityConfig.label}
      </span>
    );
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return 'N/A';
    try {
      return new Date(dateTimeStr).toLocaleString();
    } catch {
      return dateTimeStr;
    }
  };

  const getUniqueServices = () => {
    const services = subServices.reduce((acc, ss) => {
      if (!acc.find(s => s.serviceName === ss.serviceName)) {
        acc.push({ serviceName: ss.serviceName });
      }
      return acc;
    }, []);
    return services;
  };

  const getSubServicesForService = (serviceName) => {
    return subServices.filter(ss => ss.serviceName === serviceName);
  };

  if (loading && extensions.length === 0) {
    return <div className="loading">Loading cut-off extensions...</div>;
  }

  return (
    <div className="cutoff-extension-management">
      <div className="header">
        <h2>Cut-off Time Extensions</h2>
        {timeZoneInfo && (
          <div className="timezone-info">
            <span className="timezone-label">Tenant Timezone:</span>
            <span className="timezone-value">{timeZoneInfo.displayName}</span>
            <span className="current-time">{timeZoneInfo.currentTime}</span>
          </div>
        )}
        <button 
          className="btn btn-primary"
          onClick={() => setShowRequestForm(true)}
        >
          Request Extension
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="extensions-grid">
        {extensions.length === 0 ? (
          <div className="no-extensions">
            <p>No cut-off time extensions found.</p>
            <button 
              className="btn btn-primary"
              onClick={() => setShowRequestForm(true)}
            >
              Request First Extension
            </button>
          </div>
        ) : (
          extensions.map(extension => (
            <div key={extension.id} className="extension-card">
              <div className="extension-header">
                <h3>{extension.serviceName} / {extension.subServiceName}</h3>
                <div className="extension-badges">
                  {getPriorityBadge(extension.priority)}
                  {getStatusBadge(extension.status)}
                </div>
              </div>
              
              <div className="extension-details">
                <div className="detail-row">
                  <span className="label">Extension Date:</span>
                  <span className="value">{extension.extensionDate}</span>
                </div>
                
                <div className="detail-row">
                  <span className="label">Original Cut-off:</span>
                  <span className="value time">{extension.originalCutOffTime}</span>
                </div>
                
                <div className="detail-row">
                  <span className="label">Extended Cut-off:</span>
                  <span className="value time extended">{extension.extendedCutOffTime}</span>
                </div>
                
                <div className="detail-row">
                  <span className="label">Reason:</span>
                  <span className="value">{extension.reason}</span>
                </div>
                
                <div className="detail-row">
                  <span className="label">Requested by:</span>
                  <span className="value">{extension.requestedBy}</span>
                </div>
                
                <div className="detail-row">
                  <span className="label">Requested at:</span>
                  <span className="value">{formatDateTime(extension.requestedAt)}</span>
                </div>

                {extension.approvedBy && (
                  <div className="detail-row">
                    <span className="label">Approved by:</span>
                    <span className="value">{extension.approvedBy}</span>
                  </div>
                )}

                {extension.approvedAt && (
                  <div className="detail-row">
                    <span className="label">Approved at:</span>
                    <span className="value">{formatDateTime(extension.approvedAt)}</span>
                  </div>
                )}

                {extension.rejectionReason && (
                  <div className="detail-row">
                    <span className="label">Rejection reason:</span>
                    <span className="value rejection">{extension.rejectionReason}</span>
                  </div>
                )}

                {extension.notes && (
                  <div className="detail-row">
                    <span className="label">Notes:</span>
                    <span className="value">{extension.notes}</span>
                  </div>
                )}
              </div>

              {extension.status === 'PENDING' && (
                <div className="extension-actions">
                  <button 
                    className="btn btn-sm btn-success"
                    onClick={() => handleApprove(extension.id)}
                  >
                    Approve
                  </button>
                  <button 
                    className="btn btn-sm btn-danger"
                    onClick={() => handleReject(extension.id)}
                  >
                    Reject
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {showRequestForm && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h3>Request Cut-off Time Extension</h3>
              <button className="btn btn-close" onClick={resetForm}>×</button>
            </div>
            
            <form onSubmit={handleSubmit} className="extension-form">
              <div className="form-row">
                <div className="form-group">
                  <label>Service Name *</label>
                  <select
                    name="serviceName"
                    value={formData.serviceName}
                    onChange={handleInputChange}
                    required
                  >
                    <option value="">Select Service</option>
                    {getUniqueServices().map(service => (
                      <option key={service.serviceName} value={service.serviceName}>
                        {service.serviceName}
                      </option>
                    ))}
                  </select>
                </div>
                
                <div className="form-group">
                  <label>Sub-Service Name *</label>
                  <select
                    name="subServiceName"
                    value={formData.subServiceName}
                    onChange={handleInputChange}
                    required
                    disabled={!formData.serviceName}
                  >
                    <option value="">Select Sub-Service</option>
                    {getSubServicesForService(formData.serviceName).map(subService => (
                      <option key={subService.subServiceName} value={subService.subServiceName}>
                        {subService.subServiceName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Extension Date *</label>
                  <input
                    type="date"
                    name="extensionDate"
                    value={formData.extensionDate}
                    onChange={handleInputChange}
                    required
                    min={new Date().toISOString().split('T')[0]}
                  />
                </div>
                
                <div className="form-group">
                  <label>Priority</label>
                  <select
                    name="priority"
                    value={formData.priority}
                    onChange={handleInputChange}
                  >
                    {priorities.map(priority => (
                      <option key={priority.value} value={priority.value}>
                        {priority.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Original Cut-off Time</label>
                  <input
                    type="time"
                    name="originalCutOffTime"
                    value={formData.originalCutOffTime}
                    onChange={handleInputChange}
                    step="1"
                    readOnly
                  />
                </div>
                
                <div className="form-group">
                  <label>Extended Cut-off Time *</label>
                  <input
                    type="time"
                    name="extendedCutOffTime"
                    value={formData.extendedCutOffTime}
                    onChange={handleInputChange}
                    step="1"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Reason for Extension *</label>
                <textarea
                  name="reason"
                  value={formData.reason}
                  onChange={handleInputChange}
                  rows="3"
                  required
                  placeholder="Please provide a detailed reason for the cut-off time extension..."
                />
              </div>

              <div className="form-group">
                <label>Additional Notes</label>
                <textarea
                  name="notes"
                  value={formData.notes}
                  onChange={handleInputChange}
                  rows="2"
                  placeholder="Any additional information or special instructions..."
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Requesting...' : 'Request Extension'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default CutOffExtensionManagement;
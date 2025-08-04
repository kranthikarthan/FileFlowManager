import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './AlertManagement.css';

const AlertManagement = () => {
    const [activeTab, setActiveTab] = useState('configurations');
    const [tenants, setTenants] = useState([]);
    const [selectedTenant, setSelectedTenant] = useState('');
    const [services, setServices] = useState([]);
    const [selectedService, setSelectedService] = useState('');
    const [subServices, setSubServices] = useState([]);
    const [selectedSubService, setSelectedSubService] = useState('');
    
    // Alert Configurations
    const [alertConfigs, setAlertConfigs] = useState([]);
    const [showConfigForm, setShowConfigForm] = useState(false);
    const [editingConfig, setEditingConfig] = useState(null);
    const [configFormData, setConfigFormData] = useState({
        tenantId: '',
        serviceName: '',
        subServiceName: '',
        alertType: 'CUT_OFF_MISSED',
        alertDurationMinutes: 60,
        enabled: true,
        emailRecipients: '',
        notificationChannels: ''
    });

    // Alert History
    const [alertHistory, setAlertHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const alertTypes = [
        { value: 'CUT_OFF_MISSED', label: 'Cut-off Missed' },
        { value: 'EOT_NOT_RECEIVED', label: 'EOT Not Received' },
        { value: 'PROCESSING_FAILED', label: 'Processing Failed' }
    ];

    useEffect(() => {
        fetchTenants();
    }, []);

    useEffect(() => {
        if (selectedTenant) {
            fetchServices();
            fetchAlertConfigs();
            fetchAlertHistory();
        }
    }, [selectedTenant]);

    useEffect(() => {
        if (selectedService) {
            fetchSubServices();
        }
    }, [selectedService]);

    const fetchTenants = async () => {
        try {
            const response = await axios.get('/api/tenants/active');
            setTenants(response.data);
            if (response.data.length > 0) {
                setSelectedTenant(response.data[0].tenantId);
            }
        } catch (err) {
            setError('Failed to fetch tenants');
        }
    };

    const fetchServices = async () => {
        try {
            const response = await axios.get(`/api/services/tenant/${selectedTenant}/enabled`);
            setServices(response.data);
            setSelectedService('');
            setSelectedSubService('');
        } catch (err) {
            setError('Failed to fetch services');
        }
    };

    const fetchSubServices = async () => {
        try {
            const response = await axios.get(`/api/services/tenant/${selectedTenant}/service/${selectedService}/subservices`);
            setSubServices(response.data);
            setSelectedSubService('');
        } catch (err) {
            setSubServices([]);
        }
    };

    const fetchAlertConfigs = async () => {
        try {
            const response = await axios.get(`/api/alerts/configurations/tenant/${selectedTenant}`);
            setAlertConfigs(response.data);
        } catch (err) {
            setError('Failed to fetch alert configurations');
        }
    };

    const fetchAlertHistory = async () => {
        try {
            setLoading(true);
            const response = await axios.get(`/api/alerts/history/tenant/${selectedTenant}`);
            setAlertHistory(response.data);
        } catch (err) {
            setError('Failed to fetch alert history');
        } finally {
            setLoading(false);
        }
    };

    const handleConfigInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setConfigFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleConfigSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingConfig) {
                await axios.put(`/api/alerts/configurations/${editingConfig.id}`, configFormData);
            } else {
                await axios.post('/api/alerts/configurations', configFormData);
            }
            setShowConfigForm(false);
            setEditingConfig(null);
            resetConfigForm();
            fetchAlertConfigs();
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to save alert configuration');
        }
    };

    const handleConfigEdit = (config) => {
        setEditingConfig(config);
        setConfigFormData({
            tenantId: config.tenantId,
            serviceName: config.serviceName || '',
            subServiceName: config.subServiceName || '',
            alertType: config.alertType,
            alertDurationMinutes: config.alertDurationMinutes,
            enabled: config.enabled,
            emailRecipients: config.emailRecipients || '',
            notificationChannels: config.notificationChannels || ''
        });
        setShowConfigForm(true);
    };

    const handleConfigDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this alert configuration?')) {
            try {
                await axios.delete(`/api/alerts/configurations/${id}`);
                fetchAlertConfigs();
            } catch (err) {
                setError('Failed to delete alert configuration');
            }
        }
    };

    const handleAcknowledgeAlert = async (id) => {
        try {
            await axios.put(`/api/alerts/history/${id}/acknowledge?acknowledgedBy=user`);
            fetchAlertHistory();
        } catch (err) {
            setError('Failed to acknowledge alert');
        }
    };

    const resetConfigForm = () => {
        setConfigFormData({
            tenantId: selectedTenant,
            serviceName: selectedService,
            subServiceName: selectedSubService,
            alertType: 'CUT_OFF_MISSED',
            alertDurationMinutes: 60,
            enabled: true,
            emailRecipients: '',
            notificationChannels: ''
        });
        setError(null);
    };

    const getAlertLevelClass = (level) => {
        switch (level) {
            case 'CRITICAL': return 'critical';
            case 'ERROR': return 'error';
            case 'WARNING': return 'warning';
            case 'INFO': return 'info';
            default: return 'info';
        }
    };

    if (tenants.length === 0) {
        return <div className="loading">No tenants available</div>;
    }

    return (
        <div className="alert-management">
            <div className="header">
                <h2>Alert Management</h2>
                <div className="header-controls">
                    <select 
                        value={selectedTenant} 
                        onChange={(e) => setSelectedTenant(e.target.value)}
                        className="tenant-selector"
                    >
                        {tenants.map(tenant => (
                            <option key={tenant.tenantId} value={tenant.tenantId}>
                                {tenant.tenantName} ({tenant.tenantId})
                            </option>
                        ))}
                    </select>
                    <div className="tab-buttons">
                        <button 
                            className={`tab-btn ${activeTab === 'configurations' ? 'active' : ''}`}
                            onClick={() => setActiveTab('configurations')}
                        >
                            Configurations
                        </button>
                        <button 
                            className={`tab-btn ${activeTab === 'history' ? 'active' : ''}`}
                            onClick={() => setActiveTab('history')}
                        >
                            Alert History
                        </button>
                    </div>
                </div>
            </div>

            {error && (
                <div className="error-message">
                    {error}
                    <button onClick={() => setError(null)}>×</button>
                </div>
            )}

            {activeTab === 'configurations' && (
                <div className="configurations-tab">
                    <div className="tab-header">
                        <h3>Alert Configurations</h3>
                        <button 
                            className="btn btn-primary" 
                            onClick={() => setShowConfigForm(true)}
                            disabled={showConfigForm}
                        >
                            Add Configuration
                        </button>
                    </div>

                    {showConfigForm && (
                        <div className="form-overlay">
                            <div className="form-container">
                                <h3>{editingConfig ? 'Edit Alert Configuration' : 'Add Alert Configuration'}</h3>
                                <form onSubmit={handleConfigSubmit}>
                                    <div className="form-group">
                                        <label htmlFor="serviceName">Service</label>
                                        <select
                                            id="serviceName"
                                            name="serviceName"
                                            value={configFormData.serviceName}
                                            onChange={handleConfigInputChange}
                                        >
                                            <option value="">All Services</option>
                                            {services.map(service => (
                                                <option key={service.serviceName} value={service.serviceName}>
                                                    {service.serviceName}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="subServiceName">Sub-Service</label>
                                        <select
                                            id="subServiceName"
                                            name="subServiceName"
                                            value={configFormData.subServiceName}
                                            onChange={handleConfigInputChange}
                                        >
                                            <option value="">All Sub-Services</option>
                                            {subServices.map(subService => (
                                                <option key={subService} value={subService}>
                                                    {subService}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="alertType">Alert Type *</label>
                                        <select
                                            id="alertType"
                                            name="alertType"
                                            value={configFormData.alertType}
                                            onChange={handleConfigInputChange}
                                            required
                                        >
                                            {alertTypes.map(type => (
                                                <option key={type.value} value={type.value}>
                                                    {type.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="alertDurationMinutes">Alert Duration (minutes) *</label>
                                        <input
                                            type="number"
                                            id="alertDurationMinutes"
                                            name="alertDurationMinutes"
                                            value={configFormData.alertDurationMinutes}
                                            onChange={handleConfigInputChange}
                                            min="1"
                                            required
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="emailRecipients">Email Recipients</label>
                                        <input
                                            type="text"
                                            id="emailRecipients"
                                            name="emailRecipients"
                                            value={configFormData.emailRecipients}
                                            onChange={handleConfigInputChange}
                                            placeholder="email1@example.com, email2@example.com"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label className="checkbox-label">
                                            <input
                                                type="checkbox"
                                                name="enabled"
                                                checked={configFormData.enabled}
                                                onChange={handleConfigInputChange}
                                            />
                                            Enabled
                                        </label>
                                    </div>

                                    <div className="form-actions">
                                        <button type="submit" className="btn btn-primary">
                                            {editingConfig ? 'Update' : 'Create'}
                                        </button>
                                        <button type="button" className="btn btn-secondary" onClick={() => {
                                            setShowConfigForm(false);
                                            setEditingConfig(null);
                                            resetConfigForm();
                                        }}>
                                            Cancel
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}

                    <div className="configurations-list">
                        {alertConfigs.length === 0 ? (
                            <div className="no-data">No alert configurations found</div>
                        ) : (
                            <table className="configurations-table">
                                <thead>
                                    <tr>
                                        <th>Service</th>
                                        <th>Sub-Service</th>
                                        <th>Alert Type</th>
                                        <th>Duration</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {alertConfigs.map(config => (
                                        <tr key={config.id}>
                                            <td>{config.serviceName || 'All'}</td>
                                            <td>{config.subServiceName || 'All'}</td>
                                            <td>{alertTypes.find(t => t.value === config.alertType)?.label}</td>
                                            <td>{config.alertDurationMinutes} min</td>
                                            <td>
                                                <span className={`status ${config.enabled ? 'enabled' : 'disabled'}`}>
                                                    {config.enabled ? 'Active' : 'Inactive'}
                                                </span>
                                            </td>
                                            <td className="actions">
                                                <button
                                                    className="btn btn-sm btn-secondary"
                                                    onClick={() => handleConfigEdit(config)}
                                                >
                                                    Edit
                                                </button>
                                                <button
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => handleConfigDelete(config.id)}
                                                >
                                                    Delete
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {activeTab === 'history' && (
                <div className="history-tab">
                    <div className="tab-header">
                        <h3>Alert History</h3>
                    </div>

                    <div className="history-list">
                        {loading ? (
                            <div className="loading">Loading alert history...</div>
                        ) : alertHistory.length === 0 ? (
                            <div className="no-data">No alert history found</div>
                        ) : (
                            <table className="history-table">
                                <thead>
                                    <tr>
                                        <th>Date/Time</th>
                                        <th>Service</th>
                                        <th>Alert Type</th>
                                        <th>Level</th>
                                        <th>Message</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {alertHistory.map(alert => (
                                        <tr key={alert.id}>
                                            <td>{new Date(alert.sentAt).toLocaleString()}</td>
                                            <td>
                                                {alert.serviceName || 'All'}
                                                {alert.subServiceName && <br />}
                                                {alert.subServiceName && <small>{alert.subServiceName}</small>}
                                            </td>
                                            <td>{alertTypes.find(t => t.value === alert.alertType)?.label}</td>
                                            <td>
                                                <span className={`alert-level ${getAlertLevelClass(alert.alertLevel)}`}>
                                                    {alert.alertLevel}
                                                </span>
                                            </td>
                                            <td className="alert-message">{alert.alertMessage}</td>
                                            <td>
                                                {alert.acknowledgedAt ? (
                                                    <span className="acknowledged">
                                                        Acknowledged<br />
                                                        <small>{new Date(alert.acknowledgedAt).toLocaleString()}</small>
                                                    </span>
                                                ) : (
                                                    <span className="unacknowledged">Unacknowledged</span>
                                                )}
                                            </td>
                                            <td className="actions">
                                                {!alert.acknowledgedAt && (
                                                    <button
                                                        className="btn btn-sm btn-primary"
                                                        onClick={() => handleAcknowledgeAlert(alert.id)}
                                                    >
                                                        Acknowledge
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default AlertManagement;
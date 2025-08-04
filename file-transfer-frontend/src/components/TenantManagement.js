import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './TenantManagement.css';

const TenantManagement = () => {
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [editingTenant, setEditingTenant] = useState(null);
    const [formData, setFormData] = useState({
        tenantId: '',
        tenantName: '',
        timezone: 'UTC',
        enabled: true
    });

    const timezones = [
        'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
        'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Asia/Tokyo', 'Asia/Shanghai',
        'Australia/Sydney', 'Pacific/Auckland'
    ];

    useEffect(() => {
        fetchTenants();
    }, []);

    const fetchTenants = async () => {
        try {
            setLoading(true);
            const response = await axios.get('/api/tenants');
            setTenants(response.data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch tenants');
            console.error('Error fetching tenants:', err);
        } finally {
            setLoading(false);
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
        try {
            if (editingTenant) {
                await axios.put(`/api/tenants/${editingTenant.id}`, formData);
            } else {
                await axios.post('/api/tenants', formData);
            }
            setShowForm(false);
            setEditingTenant(null);
            setFormData({
                tenantId: '',
                tenantName: '',
                timezone: 'UTC',
                enabled: true
            });
            fetchTenants();
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to save tenant');
            console.error('Error saving tenant:', err);
        }
    };

    const handleEdit = (tenant) => {
        setEditingTenant(tenant);
        setFormData({
            tenantId: tenant.tenantId,
            tenantName: tenant.tenantName,
            timezone: tenant.timezone,
            enabled: tenant.enabled
        });
        setShowForm(true);
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this tenant?')) {
            try {
                await axios.delete(`/api/tenants/${id}`);
                fetchTenants();
            } catch (err) {
                setError('Failed to delete tenant');
                console.error('Error deleting tenant:', err);
            }
        }
    };

    const handleToggleStatus = async (tenant) => {
        try {
            await axios.put(`/api/tenants/${tenant.id}`, {
                ...tenant,
                enabled: !tenant.enabled
            });
            fetchTenants();
        } catch (err) {
            setError('Failed to update tenant status');
            console.error('Error updating tenant status:', err);
        }
    };

    const resetForm = () => {
        setShowForm(false);
        setEditingTenant(null);
        setFormData({
            tenantId: '',
            tenantName: '',
            timezone: 'UTC',
            enabled: true
        });
        setError(null);
    };

    if (loading) {
        return <div className="loading">Loading tenants...</div>;
    }

    return (
        <div className="tenant-management">
            <div className="header">
                <h2>Tenant Management</h2>
                <button 
                    className="btn btn-primary" 
                    onClick={() => setShowForm(true)}
                    disabled={showForm}
                >
                    Add New Tenant
                </button>
            </div>

            {error && (
                <div className="error-message">
                    {error}
                    <button onClick={() => setError(null)}>×</button>
                </div>
            )}

            {showForm && (
                <div className="form-overlay">
                    <div className="form-container">
                        <h3>{editingTenant ? 'Edit Tenant' : 'Add New Tenant'}</h3>
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label htmlFor="tenantId">Tenant ID *</label>
                                <input
                                    type="text"
                                    id="tenantId"
                                    name="tenantId"
                                    value={formData.tenantId}
                                    onChange={handleInputChange}
                                    required
                                    disabled={editingTenant}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="tenantName">Tenant Name *</label>
                                <input
                                    type="text"
                                    id="tenantName"
                                    name="tenantName"
                                    value={formData.tenantName}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="timezone">Timezone *</label>
                                <select
                                    id="timezone"
                                    name="timezone"
                                    value={formData.timezone}
                                    onChange={handleInputChange}
                                    required
                                >
                                    {timezones.map(tz => (
                                        <option key={tz} value={tz}>{tz}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label className="checkbox-label">
                                    <input
                                        type="checkbox"
                                        name="enabled"
                                        checked={formData.enabled}
                                        onChange={handleInputChange}
                                    />
                                    Enabled
                                </label>
                            </div>

                            <div className="form-actions">
                                <button type="submit" className="btn btn-primary">
                                    {editingTenant ? 'Update' : 'Create'}
                                </button>
                                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <div className="tenants-list">
                {tenants.length === 0 ? (
                    <div className="no-data">No tenants found</div>
                ) : (
                    <table className="tenants-table">
                        <thead>
                            <tr>
                                <th>Tenant ID</th>
                                <th>Name</th>
                                <th>Timezone</th>
                                <th>Status</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {tenants.map(tenant => (
                                <tr key={tenant.id} className={!tenant.enabled ? 'disabled' : ''}>
                                    <td>{tenant.tenantId}</td>
                                    <td>{tenant.tenantName}</td>
                                    <td>{tenant.timezone}</td>
                                    <td>
                                        <span className={`status ${tenant.enabled ? 'enabled' : 'disabled'}`}>
                                            {tenant.enabled ? 'Active' : 'Inactive'}
                                        </span>
                                    </td>
                                    <td>{new Date(tenant.createdAt).toLocaleDateString()}</td>
                                    <td className="actions">
                                        <button
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => handleEdit(tenant)}
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="btn btn-sm btn-warning"
                                            onClick={() => handleToggleStatus(tenant)}
                                        >
                                            {tenant.enabled ? 'Disable' : 'Enable'}
                                        </button>
                                        <button
                                            className="btn btn-sm btn-danger"
                                            onClick={() => handleDelete(tenant.id)}
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
    );
};

export default TenantManagement;
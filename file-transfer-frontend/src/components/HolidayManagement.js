import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './HolidayManagement.css';

const HolidayManagement = () => {
    const [holidays, setHolidays] = useState([]);
    const [tenants, setTenants] = useState([]);
    const [selectedTenant, setSelectedTenant] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [editingHoliday, setEditingHoliday] = useState(null);
    const [formData, setFormData] = useState({
        tenantId: '',
        holidayDate: '',
        holidayName: '',
        description: ''
    });

    useEffect(() => {
        fetchTenants();
    }, []);

    useEffect(() => {
        if (selectedTenant) {
            fetchHolidays();
        }
    }, [selectedTenant]);

    const fetchTenants = async () => {
        try {
            const response = await axios.get('/api/tenants/active');
            setTenants(response.data);
            if (response.data.length > 0) {
                setSelectedTenant(response.data[0].tenantId);
            }
        } catch (err) {
            setError('Failed to fetch tenants');
            console.error('Error fetching tenants:', err);
        }
    };

    const fetchHolidays = async () => {
        if (!selectedTenant) return;
        
        try {
            setLoading(true);
            const response = await axios.get(`/api/holidays/tenant/${selectedTenant}`);
            setHolidays(response.data);
            setError(null);
        } catch (err) {
            setError('Failed to fetch holidays');
            console.error('Error fetching holidays:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingHoliday) {
                await axios.put(`/api/holidays/${editingHoliday.id}`, formData);
            } else {
                await axios.post('/api/holidays', formData);
            }
            setShowForm(false);
            setEditingHoliday(null);
            setFormData({
                tenantId: selectedTenant,
                holidayDate: '',
                holidayName: '',
                description: ''
            });
            fetchHolidays();
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to save holiday');
            console.error('Error saving holiday:', err);
        }
    };

    const handleEdit = (holiday) => {
        setEditingHoliday(holiday);
        setFormData({
            tenantId: holiday.tenantId,
            holidayDate: holiday.holidayDate,
            holidayName: holiday.holidayName,
            description: holiday.description || ''
        });
        setShowForm(true);
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this holiday?')) {
            try {
                await axios.delete(`/api/holidays/${id}`);
                fetchHolidays();
            } catch (err) {
                setError('Failed to delete holiday');
                console.error('Error deleting holiday:', err);
            }
        }
    };

    const resetForm = () => {
        setShowForm(false);
        setEditingHoliday(null);
        setFormData({
            tenantId: selectedTenant,
            holidayDate: '',
            holidayName: '',
            description: ''
        });
        setError(null);
    };

    const handleTenantChange = (e) => {
        setSelectedTenant(e.target.value);
    };

    if (tenants.length === 0) {
        return <div className="loading">No tenants available</div>;
    }

    return (
        <div className="holiday-management">
            <div className="header">
                <h2>Holiday Management</h2>
                <div className="header-controls">
                    <select 
                        value={selectedTenant} 
                        onChange={handleTenantChange}
                        className="tenant-selector"
                    >
                        {tenants.map(tenant => (
                            <option key={tenant.tenantId} value={tenant.tenantId}>
                                {tenant.tenantName} ({tenant.tenantId})
                            </option>
                        ))}
                    </select>
                    <button 
                        className="btn btn-primary" 
                        onClick={() => setShowForm(true)}
                        disabled={showForm || !selectedTenant}
                    >
                        Add Holiday
                    </button>
                </div>
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
                        <h3>{editingHoliday ? 'Edit Holiday' : 'Add New Holiday'}</h3>
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label htmlFor="tenantId">Tenant *</label>
                                <select
                                    id="tenantId"
                                    name="tenantId"
                                    value={formData.tenantId}
                                    onChange={handleInputChange}
                                    required
                                    disabled={editingHoliday}
                                >
                                    {tenants.map(tenant => (
                                        <option key={tenant.tenantId} value={tenant.tenantId}>
                                            {tenant.tenantName} ({tenant.tenantId})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label htmlFor="holidayDate">Holiday Date *</label>
                                <input
                                    type="date"
                                    id="holidayDate"
                                    name="holidayDate"
                                    value={formData.holidayDate}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="holidayName">Holiday Name *</label>
                                <input
                                    type="text"
                                    id="holidayName"
                                    name="holidayName"
                                    value={formData.holidayName}
                                    onChange={handleInputChange}
                                    required
                                    placeholder="e.g., New Year's Day"
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="description">Description</label>
                                <textarea
                                    id="description"
                                    name="description"
                                    value={formData.description}
                                    onChange={handleInputChange}
                                    rows="3"
                                    placeholder="Optional description of the holiday"
                                />
                            </div>

                            <div className="form-actions">
                                <button type="submit" className="btn btn-primary">
                                    {editingHoliday ? 'Update' : 'Create'}
                                </button>
                                <button type="button" className="btn btn-secondary" onClick={resetForm}>
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <div className="holidays-list">
                {loading ? (
                    <div className="loading">Loading holidays...</div>
                ) : holidays.length === 0 ? (
                    <div className="no-data">No holidays found for this tenant</div>
                ) : (
                    <table className="holidays-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Holiday Name</th>
                                <th>Description</th>
                                <th>Created</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {holidays.map(holiday => (
                                <tr key={holiday.id}>
                                    <td>
                                        <strong>{new Date(holiday.holidayDate).toLocaleDateString()}</strong>
                                        <br />
                                        <small>{new Date(holiday.holidayDate).toLocaleDateString('en-US', { weekday: 'long' })}</small>
                                    </td>
                                    <td>{holiday.holidayName}</td>
                                    <td>{holiday.description || '-'}</td>
                                    <td>{new Date(holiday.createdAt).toLocaleDateString()}</td>
                                    <td className="actions">
                                        <button
                                            className="btn btn-sm btn-secondary"
                                            onClick={() => handleEdit(holiday)}
                                        >
                                            Edit
                                        </button>
                                        <button
                                            className="btn btn-sm btn-danger"
                                            onClick={() => handleDelete(holiday.id)}
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

export default HolidayManagement;
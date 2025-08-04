import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Tabs, Tab, Box } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
import SettingsIcon from '@mui/icons-material/Settings';
import BuildIcon from '@mui/icons-material/Build';
import SecurityIcon from '@mui/icons-material/Security';
import BusinessIcon from '@mui/icons-material/Business';
import EventIcon from '@mui/icons-material/Event';
import NotificationsIcon from '@mui/icons-material/Notifications';

export const Navigation = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleChange = (event, newValue) => {
    navigate(newValue);
  };

  return (
    <Box sx={{ borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
      <Tabs value={location.pathname} onChange={handleChange} aria-label="navigation tabs">
        <Tab 
          icon={<DashboardIcon />} 
          iconPosition="start" 
          label="Dashboard" 
          value="/" 
        />
        <Tab 
          icon={<TransferWithinAStationIcon />} 
          iconPosition="start" 
          label="File Transfers" 
          value="/transfers" 
        />
        <Tab 
          icon={<BuildIcon />} 
          iconPosition="start" 
          label="Service Config" 
          value="/service-config" 
        />
        <Tab 
          icon={<SecurityIcon />} 
          iconPosition="start" 
          label="SSO Config" 
          value="/sso-config" 
        />
        <Tab 
          icon={<SettingsIcon />} 
          iconPosition="start" 
          label="Service Management" 
          value="/services" 
        />
        <Tab 
          icon={<BusinessIcon />} 
          iconPosition="start" 
          label="Tenants" 
          value="/tenants" 
        />
        <Tab 
          icon={<EventIcon />} 
          iconPosition="start" 
          label="Holidays" 
          value="/holidays" 
        />
        <Tab 
          icon={<NotificationsIcon />} 
          iconPosition="start" 
          label="Alerts" 
          value="/alerts" 
        />
      </Tabs>
    </Box>
  );
};
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Tabs, Tab, Box } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
import SettingsIcon from '@mui/icons-material/Settings';

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
          icon={<SettingsIcon />} 
          iconPosition="start" 
          label="Service Management" 
          value="/services" 
        />
      </Tabs>
    </Box>
  );
};
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
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ScheduleIcon from '@mui/icons-material/Schedule';
import SchemaIcon from '@mui/icons-material/Schema';
import AssessmentIcon from '@mui/icons-material/Assessment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CompressIcon from '@mui/icons-material/Compress';
import SecurityIcon from '@mui/icons-material/Security';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import AccountTreeIcon from '@mui/icons-material/AccountTree';

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
        <Tab 
          icon={<AccountTreeIcon />} 
          iconPosition="start" 
          label="Sub-Services" 
          value="/sub-services" 
        />
        <Tab 
          icon={<ScheduleIcon />} 
          iconPosition="start" 
          label="Cut-off Extensions" 
          value="/cutoff-extensions" 
        />
        <Tab 
          icon={<SchemaIcon />} 
          iconPosition="start" 
          label="Shared Schemas" 
          value="/shared-schemas" 
        />
        <Tab 
          icon={<AssessmentIcon />} 
          iconPosition="start" 
          label="EOT Validation" 
          value="/eot-validation" 
        />
        <Tab 
          icon={<CheckCircleIcon />} 
          iconPosition="start" 
          label="ACK/NACK" 
          value="/ack-nack" 
        />
        <Tab 
          icon={<CompressIcon />} 
          iconPosition="start" 
          label="Compression" 
          value="/compression" 
        />
        <Tab 
          icon={<SecurityIcon />} 
          iconPosition="start" 
          label="HSM Security" 
          value="/hsm" 
        />
        <Tab 
          icon={<AnalyticsIcon />} 
          iconPosition="start" 
          label="Content Analysis" 
          value="/content-analysis" 
        />
        <Tab 
          icon={<LocalOfferIcon />} 
          iconPosition="start" 
          label="File Tags" 
          value="/file-tags" 
        />
        <Tab 
          icon={<AccountTreeIcon />} 
          iconPosition="start" 
          label="Workflows" 
          value="/workflows" 
        />
      </Tabs>
    </Box>
  );
};
import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  BottomNavigation,
  BottomNavigationAction,
  Paper,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  IconButton,
  Box,
  Typography,
  useMediaQuery,
  useTheme as useMuiTheme,
  Divider,
  Collapse,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Menu as MenuIcon,
  Close as CloseIcon,
  TransferWithinAStation as TransferIcon,
  Settings as SettingsIcon,
  Assessment as AssessmentIcon,
  ExpandLess,
  ExpandMore,
  Build as BuildIcon,
  Security as SecurityIcon,
  Business as BusinessIcon,
  Event as EventIcon,
  Notifications as NotificationsIcon,
  AccountTree as AccountTreeIcon,
  Schedule as ScheduleIcon,
  Schema as SchemaIcon,
} from '@mui/icons-material';
import { useTheme } from '../../theme/themeProvider';
import ThemeToggle from './ThemeToggle';

const MobileNavigation = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark } = useTheme();
  const muiTheme = useMuiTheme();
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('md'));
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expandedSections, setExpandedSections] = useState({});

  // Primary navigation items for bottom navigation
  const primaryNavItems = [
    { label: 'Dashboard', icon: <DashboardIcon />, path: '/' },
    { label: 'Transfers', icon: <TransferIcon />, path: '/transfers' },
    { label: 'Setup', icon: <SettingsIcon />, action: 'menu' },
    { label: 'Reports', icon: <AssessmentIcon />, path: '/eot-validation' },
  ];

  // All navigation items organized by sections
  const navigationSections = [
    {
      title: 'Core Operations',
      items: [
        { label: 'Dashboard', icon: <DashboardIcon />, path: '/' },
        { label: 'File Transfers', icon: <TransferIcon />, path: '/transfers' },
        { label: 'EOT Validation', icon: <AssessmentIcon />, path: '/eot-validation' },
      ],
    },
    {
      title: 'Configuration',
      items: [
        { label: 'Service Config', icon: <BuildIcon />, path: '/service-config' },
        { label: 'SSO Config', icon: <SecurityIcon />, path: '/sso-config' },
        { label: 'Service Management', icon: <SettingsIcon />, path: '/services' },
        { label: 'Sub-Services', icon: <AccountTreeIcon />, path: '/sub-services' },
        { label: 'Shared Schemas', icon: <SchemaIcon />, path: '/shared-schemas' },
      ],
    },
    {
      title: 'Management',
      items: [
        { label: 'Tenants', icon: <BusinessIcon />, path: '/tenants' },
        { label: 'Holidays', icon: <EventIcon />, path: '/holidays' },
        { label: 'Alerts', icon: <NotificationsIcon />, path: '/alerts' },
        { label: 'Cut-off Extensions', icon: <ScheduleIcon />, path: '/cutoff-extensions' },
      ],
    },
  ];

  const handleNavigation = (item) => {
    if (item.action === 'menu') {
      setDrawerOpen(true);
    } else if (item.path) {
      navigate(item.path);
      setDrawerOpen(false);
    }
  };

  const toggleSection = (sectionTitle) => {
    setExpandedSections(prev => ({
      ...prev,
      [sectionTitle]: !prev[sectionTitle]
    }));
  };

  const getCurrentValue = () => {
    const primaryItem = primaryNavItems.find(item => item.path === location.pathname);
    return primaryItem ? primaryNavItems.indexOf(primaryItem) : 2; // Default to Setup
  };

  if (!isMobile) {
    return null; // Use regular navigation on desktop
  }

  return (
    <>
      {/* Bottom Navigation for Mobile */}
      <Paper
        sx={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          zIndex: 1000,
          background: isDark
            ? 'rgba(26, 46, 26, 0.95)'
            : 'rgba(248, 253, 248, 0.95)',
          backdropFilter: 'blur(20px)',
          borderTop: isDark
            ? '1px solid rgba(129, 199, 132, 0.2)'
            : '1px solid rgba(46, 125, 50, 0.2)',
        }}
        elevation={8}
      >
        <BottomNavigation
          value={getCurrentValue()}
          onChange={(event, newValue) => {
            const item = primaryNavItems[newValue];
            handleNavigation(item);
          }}
          sx={{
            background: 'transparent',
            '& .MuiBottomNavigationAction-root': {
              color: isDark ? 'rgba(232, 245, 232, 0.6)' : 'rgba(27, 94, 32, 0.6)',
              '&.Mui-selected': {
                color: isDark ? '#81c784' : '#2e7d32',
              },
            },
          }}
        >
          {primaryNavItems.map((item, index) => (
            <BottomNavigationAction
              key={index}
              label={item.label}
              icon={item.icon}
              sx={{
                minWidth: 'auto',
                '& .MuiBottomNavigationAction-label': {
                  fontSize: '0.75rem',
                  fontWeight: 500,
                },
              }}
            />
          ))}
        </BottomNavigation>
      </Paper>

      {/* Full Navigation Drawer */}
      <Drawer
        anchor="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        PaperProps={{
          sx: {
            width: 320,
            maxWidth: '85vw',
            background: isDark
              ? 'rgba(13, 27, 13, 0.98)'
              : 'rgba(248, 253, 248, 0.98)',
            backdropFilter: 'blur(20px)',
            border: isDark
              ? '1px solid rgba(129, 199, 132, 0.2)'
              : '1px solid rgba(46, 125, 50, 0.2)',
          },
        }}
      >
        {/* Header */}
        <Box
          sx={{
            p: 3,
            background: isDark
              ? 'linear-gradient(135deg, rgba(129, 199, 132, 0.1) 0%, rgba(165, 214, 167, 0.1) 100%)'
              : 'linear-gradient(135deg, rgba(46, 125, 50, 0.1) 0%, rgba(76, 175, 80, 0.1) 100%)',
            borderBottom: isDark
              ? '1px solid rgba(129, 199, 132, 0.2)'
              : '1px solid rgba(46, 125, 50, 0.2)',
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" color="primary" fontWeight={600}>
              File Transfer System
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <ThemeToggle />
              <IconButton onClick={() => setDrawerOpen(false)} color="primary">
                <CloseIcon />
              </IconButton>
            </Box>
          </Box>
          <Typography variant="body2" color="text.secondary">
            Navigate to any section
          </Typography>
        </Box>

        {/* Navigation Sections */}
        <List sx={{ p: 0 }}>
          {navigationSections.map((section) => (
            <React.Fragment key={section.title}>
              <ListItem
                button
                onClick={() => toggleSection(section.title)}
                sx={{
                  py: 2,
                  px: 3,
                  background: isDark
                    ? 'rgba(26, 46, 26, 0.5)'
                    : 'rgba(232, 245, 232, 0.5)',
                  borderBottom: isDark
                    ? '1px solid rgba(74, 92, 74, 0.3)'
                    : '1px solid rgba(200, 230, 201, 0.5)',
                }}
              >
                <ListItemText
                  primary={
                    <Typography variant="subtitle2" fontWeight={600} color="primary">
                      {section.title}
                    </Typography>
                  }
                />
                {expandedSections[section.title] ? (
                  <ExpandLess color="primary" />
                ) : (
                  <ExpandMore color="primary" />
                )}
              </ListItem>

              <Collapse in={expandedSections[section.title]} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {section.items.map((item) => (
                    <ListItem
                      key={item.path}
                      button
                      onClick={() => handleNavigation(item)}
                      selected={location.pathname === item.path}
                      sx={{
                        pl: 4,
                        py: 1.5,
                        '&.Mui-selected': {
                          background: isDark
                            ? 'rgba(129, 199, 132, 0.15)'
                            : 'rgba(46, 125, 50, 0.15)',
                          borderRight: `3px solid ${isDark ? '#81c784' : '#2e7d32'}`,
                        },
                        '&:hover': {
                          background: isDark
                            ? 'rgba(129, 199, 132, 0.08)'
                            : 'rgba(46, 125, 50, 0.08)',
                        },
                      }}
                    >
                      <ListItemIcon
                        sx={{
                          minWidth: 40,
                          color: location.pathname === item.path
                            ? (isDark ? '#81c784' : '#2e7d32')
                            : 'text.secondary',
                        }}
                      >
                        {item.icon}
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Typography
                            variant="body2"
                            fontWeight={location.pathname === item.path ? 600 : 400}
                            color={location.pathname === item.path ? 'primary' : 'text.primary'}
                          >
                            {item.label}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              </Collapse>
            </React.Fragment>
          ))}
        </List>

        {/* Footer */}
        <Box
          sx={{
            mt: 'auto',
            p: 3,
            borderTop: isDark
              ? '1px solid rgba(129, 199, 132, 0.2)'
              : '1px solid rgba(46, 125, 50, 0.2)',
            background: isDark
              ? 'rgba(26, 46, 26, 0.3)'
              : 'rgba(232, 245, 232, 0.3)',
          }}
        >
          <Typography variant="caption" color="text.secondary" align="center" display="block">
            File Transfer Management System
          </Typography>
          <Typography variant="caption" color="text.secondary" align="center" display="block">
            Mobile Optimized Interface
          </Typography>
        </Box>
      </Drawer>
    </>
  );
};

export default MobileNavigation;
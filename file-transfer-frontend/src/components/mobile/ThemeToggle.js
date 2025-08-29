import React, { useState } from 'react';
import {
  IconButton,
  Menu,
  MenuItem,
  Tooltip,
  Box,
  Typography,
  Switch,
  FormControlLabel,
  Divider,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  Brightness6 as ThemeIcon,
  LightMode as LightModeIcon,
  DarkMode as DarkModeIcon,
  Schedule as AutoIcon,
  Palette as PaletteIcon,
} from '@mui/icons-material';
import { useTheme } from '../../theme/themeProvider';

const ThemeToggle = () => {
  const { mode, toggleTheme, setAutoTheme, isDark } = useTheme();
  const [anchorEl, setAnchorEl] = useState(null);
  const [autoMode, setAutoMode] = useState(!localStorage.getItem('themeMode'));

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleToggleTheme = () => {
    setAutoMode(false);
    toggleTheme();
    handleClose();
  };

  const handleAutoMode = () => {
    setAutoMode(true);
    setAutoTheme();
    handleClose();
  };

  const getCurrentIcon = () => {
    if (autoMode) return <AutoIcon />;
    return isDark ? <DarkModeIcon /> : <LightModeIcon />;
  };

  const getCurrentLabel = () => {
    if (autoMode) return 'Auto Theme';
    return isDark ? 'Dark Mode' : 'Light Mode';
  };

  return (
    <>
      <Tooltip title={getCurrentLabel()}>
        <IconButton
          onClick={handleClick}
          color="inherit"
          sx={{
            background: isDark 
              ? 'rgba(129, 199, 132, 0.1)' 
              : 'rgba(46, 125, 50, 0.1)',
            backdropFilter: 'blur(10px)',
            border: isDark 
              ? '1px solid rgba(129, 199, 132, 0.2)' 
              : '1px solid rgba(46, 125, 50, 0.2)',
            '&:hover': {
              background: isDark 
                ? 'rgba(129, 199, 132, 0.2)' 
                : 'rgba(46, 125, 50, 0.2)',
            },
          }}
        >
          {getCurrentIcon()}
        </IconButton>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{
          sx: {
            mt: 1,
            minWidth: 220,
            borderRadius: 2,
            boxShadow: isDark 
              ? '0 8px 32px rgba(0, 0, 0, 0.4)' 
              : '0 8px 32px rgba(46, 125, 50, 0.15)',
            backdropFilter: 'blur(20px)',
          },
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <PaletteIcon sx={{ mr: 1, color: 'primary.main' }} />
            <Typography variant="h6" color="primary">
              Theme Settings
            </Typography>
          </Box>
          
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Choose your preferred theme for day and night usage
          </Typography>
        </Box>

        <Divider />

        <MenuItem
          onClick={handleAutoMode}
          selected={autoMode}
          sx={{
            py: 1.5,
            '&.Mui-selected': {
              background: isDark 
                ? 'rgba(129, 199, 132, 0.1)' 
                : 'rgba(46, 125, 50, 0.1)',
            },
          }}
        >
          <ListItemIcon>
            <AutoIcon color="primary" />
          </ListItemIcon>
          <ListItemText
            primary="Auto Theme"
            secondary="Light during day (6AM-6PM), Dark at night"
          />
        </MenuItem>

        <MenuItem
          onClick={() => {
            setAutoMode(false);
            if (isDark) toggleTheme();
            handleClose();
          }}
          selected={!autoMode && !isDark}
          sx={{
            py: 1.5,
            '&.Mui-selected': {
              background: isDark 
                ? 'rgba(129, 199, 132, 0.1)' 
                : 'rgba(46, 125, 50, 0.1)',
            },
          }}
        >
          <ListItemIcon>
            <LightModeIcon sx={{ color: '#2e7d32' }} />
          </ListItemIcon>
          <ListItemText
            primary="Light Mode"
            secondary="Bright theme for daytime use"
          />
        </MenuItem>

        <MenuItem
          onClick={() => {
            setAutoMode(false);
            if (!isDark) toggleTheme();
            handleClose();
          }}
          selected={!autoMode && isDark}
          sx={{
            py: 1.5,
            '&.Mui-selected': {
              background: isDark 
                ? 'rgba(129, 199, 132, 0.1)' 
                : 'rgba(46, 125, 50, 0.1)',
            },
          }}
        >
          <ListItemIcon>
            <DarkModeIcon sx={{ color: '#81c784' }} />
          </ListItemIcon>
          <ListItemText
            primary="Dark Mode"
            secondary="Easy on eyes for nighttime use"
          />
        </MenuItem>

        <Divider sx={{ my: 1 }} />
        
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="body2" color="text.secondary">
              Current: {getCurrentLabel()}
            </Typography>
            <Box
              sx={{
                width: 24,
                height: 24,
                borderRadius: '50%',
                background: isDark
                  ? 'linear-gradient(135deg, #81c784 0%, #a5d6a7 100%)'
                  : 'linear-gradient(135deg, #2e7d32 0%, #4caf50 100%)',
                border: '2px solid',
                borderColor: 'background.paper',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
              }}
            />
          </Box>
        </Box>
      </Menu>
    </>
  );
};

export default ThemeToggle;
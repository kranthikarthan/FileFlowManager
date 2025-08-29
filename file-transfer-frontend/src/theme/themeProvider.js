import React, { createContext, useContext, useState, useEffect } from 'react';
import { createTheme, ThemeProvider as MuiThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// Theme context
const ThemeContext = createContext();

// Green color palettes for day and night themes
const greenPalettes = {
  light: {
    primary: {
      main: '#2e7d32',      // Forest green
      light: '#60ad5e',     // Light forest green
      dark: '#005005',      // Dark forest green
      contrastText: '#ffffff'
    },
    secondary: {
      main: '#4caf50',      // Material green
      light: '#81c784',     // Light material green
      dark: '#388e3c',      // Dark material green
      contrastText: '#ffffff'
    },
    success: {
      main: '#66bb6a',      // Success green
      light: '#98ee99',     // Light success green
      dark: '#338a3e',      // Dark success green
      contrastText: '#000000'
    },
    background: {
      default: '#f8fdf8',   // Very light green tint
      paper: '#ffffff',
      accent: '#e8f5e8'     // Light green accent
    },
    text: {
      primary: '#1b5e20',   // Dark green for text
      secondary: '#2e7d32', // Medium green for secondary text
      disabled: '#a5d6a7'   // Light green for disabled
    }
  },
  dark: {
    primary: {
      main: '#81c784',      // Light green for dark theme
      light: '#b2fab4',     // Very light green
      dark: '#519657',      // Medium green
      contrastText: '#000000'
    },
    secondary: {
      main: '#a5d6a7',      // Light material green
      light: '#d7ffd9',     // Very light material green
      dark: '#75a478',      // Medium material green
      contrastText: '#000000'
    },
    success: {
      main: '#4caf50',      // Standard green
      light: '#81c784',     // Light green
      dark: '#388e3c',      // Dark green
      contrastText: '#ffffff'
    },
    background: {
      default: '#0d1b0d',   // Very dark green
      paper: '#1a2e1a',     // Dark green paper
      accent: '#2e4a2e'     // Medium dark green accent
    },
    text: {
      primary: '#e8f5e8',   // Light green for text
      secondary: '#c8e6c9', // Medium light green for secondary
      disabled: '#4a5c4a'   // Dark green for disabled
    }
  }
};

// Create theme configurations
const createCustomTheme = (mode) => {
  const palette = greenPalettes[mode];
  
  return createTheme({
    palette: {
      mode,
      ...palette,
      error: {
        main: mode === 'light' ? '#d32f2f' : '#f44336',
        light: mode === 'light' ? '#ef5350' : '#e57373',
        dark: mode === 'light' ? '#c62828' : '#d32f2f',
      },
      warning: {
        main: mode === 'light' ? '#ed6c02' : '#ff9800',
        light: mode === 'light' ? '#ff9800' : '#ffb74d',
        dark: mode === 'light' ? '#e65100' : '#f57c00',
      },
      info: {
        main: mode === 'light' ? '#0288d1' : '#29b6f6',
        light: mode === 'light' ? '#03a9f4' : '#4fc3f7',
        dark: mode === 'light' ? '#01579b' : '#0277bd',
      },
      divider: mode === 'light' ? '#c8e6c9' : '#4a5c4a',
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      h1: {
        fontWeight: 600,
        fontSize: '2.5rem',
        lineHeight: 1.2,
        color: palette.text.primary,
      },
      h2: {
        fontWeight: 600,
        fontSize: '2rem',
        lineHeight: 1.3,
        color: palette.text.primary,
      },
      h3: {
        fontWeight: 600,
        fontSize: '1.75rem',
        lineHeight: 1.4,
        color: palette.text.primary,
      },
      h4: {
        fontWeight: 600,
        fontSize: '1.5rem',
        lineHeight: 1.4,
        color: palette.text.primary,
      },
      h5: {
        fontWeight: 600,
        fontSize: '1.25rem',
        lineHeight: 1.5,
        color: palette.text.primary,
      },
      h6: {
        fontWeight: 600,
        fontSize: '1rem',
        lineHeight: 1.5,
        color: palette.text.primary,
      },
      body1: {
        fontSize: '1rem',
        lineHeight: 1.6,
        color: palette.text.primary,
      },
      body2: {
        fontSize: '0.875rem',
        lineHeight: 1.6,
        color: palette.text.secondary,
      },
      button: {
        textTransform: 'none',
        fontWeight: 600,
        fontSize: '0.875rem',
      },
    },
    shape: {
      borderRadius: 12,
    },
    shadows: mode === 'light' ? [
      'none',
      '0px 2px 4px rgba(46, 125, 50, 0.05)',
      '0px 4px 8px rgba(46, 125, 50, 0.08)',
      '0px 6px 12px rgba(46, 125, 50, 0.1)',
      '0px 8px 16px rgba(46, 125, 50, 0.12)',
      '0px 10px 20px rgba(46, 125, 50, 0.14)',
      '0px 12px 24px rgba(46, 125, 50, 0.16)',
      '0px 14px 28px rgba(46, 125, 50, 0.18)',
      '0px 16px 32px rgba(46, 125, 50, 0.2)',
      '0px 18px 36px rgba(46, 125, 50, 0.22)',
      '0px 20px 40px rgba(46, 125, 50, 0.24)',
      '0px 22px 44px rgba(46, 125, 50, 0.26)',
      '0px 24px 48px rgba(46, 125, 50, 0.28)',
      '0px 26px 52px rgba(46, 125, 50, 0.3)',
      '0px 28px 56px rgba(46, 125, 50, 0.32)',
      '0px 30px 60px rgba(46, 125, 50, 0.34)',
      '0px 32px 64px rgba(46, 125, 50, 0.36)',
      '0px 34px 68px rgba(46, 125, 50, 0.38)',
      '0px 36px 72px rgba(46, 125, 50, 0.4)',
      '0px 38px 76px rgba(46, 125, 50, 0.42)',
      '0px 40px 80px rgba(46, 125, 50, 0.44)',
      '0px 42px 84px rgba(46, 125, 50, 0.46)',
      '0px 44px 88px rgba(46, 125, 50, 0.48)',
      '0px 46px 92px rgba(46, 125, 50, 0.5)',
      '0px 48px 96px rgba(46, 125, 50, 0.52)',
    ] : [
      'none',
      '0px 2px 4px rgba(0, 0, 0, 0.2)',
      '0px 4px 8px rgba(0, 0, 0, 0.25)',
      '0px 6px 12px rgba(0, 0, 0, 0.3)',
      '0px 8px 16px rgba(0, 0, 0, 0.35)',
      '0px 10px 20px rgba(0, 0, 0, 0.4)',
      '0px 12px 24px rgba(0, 0, 0, 0.45)',
      '0px 14px 28px rgba(0, 0, 0, 0.5)',
      '0px 16px 32px rgba(0, 0, 0, 0.55)',
      '0px 18px 36px rgba(0, 0, 0, 0.6)',
      '0px 20px 40px rgba(0, 0, 0, 0.65)',
      '0px 22px 44px rgba(0, 0, 0, 0.7)',
      '0px 24px 48px rgba(0, 0, 0, 0.75)',
      '0px 26px 52px rgba(0, 0, 0, 0.8)',
      '0px 28px 56px rgba(0, 0, 0, 0.85)',
      '0px 30px 60px rgba(0, 0, 0, 0.9)',
      '0px 32px 64px rgba(0, 0, 0, 0.95)',
      '0px 34px 68px rgba(0, 0, 0, 1)',
      '0px 36px 72px rgba(0, 0, 0, 1)',
      '0px 38px 76px rgba(0, 0, 0, 1)',
      '0px 40px 80px rgba(0, 0, 0, 1)',
      '0px 42px 84px rgba(0, 0, 0, 1)',
      '0px 44px 88px rgba(0, 0, 0, 1)',
      '0px 46px 92px rgba(0, 0, 0, 1)',
      '0px 48px 96px rgba(0, 0, 0, 1)',
    ],
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            padding: '12px 24px',
            boxShadow: 'none',
            '&:hover': {
              boxShadow: mode === 'light' 
                ? '0px 4px 12px rgba(46, 125, 50, 0.15)' 
                : '0px 4px 12px rgba(129, 199, 132, 0.3)',
            },
          },
          contained: {
            background: mode === 'light' 
              ? `linear-gradient(135deg, ${palette.primary.main} 0%, ${palette.secondary.main} 100%)`
              : `linear-gradient(135deg, ${palette.primary.main} 0%, ${palette.secondary.main} 100%)`,
            '&:hover': {
              background: mode === 'light'
                ? `linear-gradient(135deg, ${palette.primary.dark} 0%, ${palette.secondary.dark} 100%)`
                : `linear-gradient(135deg, ${palette.primary.light} 0%, ${palette.secondary.light} 100%)`,
            },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            border: mode === 'light' ? '1px solid #e8f5e8' : '1px solid #4a5c4a',
            backdropFilter: 'blur(10px)',
            background: mode === 'light' 
              ? 'rgba(255, 255, 255, 0.9)'
              : 'rgba(26, 46, 26, 0.9)',
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            border: mode === 'light' ? '1px solid #e8f5e8' : '1px solid #4a5c4a',
          },
        },
      },
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: 12,
              '& fieldset': {
                borderColor: mode === 'light' ? '#c8e6c9' : '#4a5c4a',
              },
              '&:hover fieldset': {
                borderColor: palette.primary.main,
              },
              '&.Mui-focused fieldset': {
                borderColor: palette.primary.main,
                borderWidth: 2,
              },
            },
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            fontWeight: 500,
          },
          colorPrimary: {
            background: mode === 'light'
              ? `linear-gradient(135deg, ${palette.primary.light} 0%, ${palette.secondary.light} 100%)`
              : `linear-gradient(135deg, ${palette.primary.main} 0%, ${palette.secondary.main} 100%)`,
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            background: mode === 'light'
              ? `linear-gradient(135deg, ${palette.primary.main} 0%, ${palette.secondary.main} 100%)`
              : `linear-gradient(135deg, ${palette.background.paper} 0%, ${palette.background.accent} 100%)`,
            backdropFilter: 'blur(20px)',
          },
        },
      },
      MuiTabs: {
        styleOverrides: {
          root: {
            background: mode === 'light' 
              ? 'rgba(248, 253, 248, 0.8)'
              : 'rgba(26, 46, 26, 0.8)',
            backdropFilter: 'blur(10px)',
            borderRadius: 12,
          },
          indicator: {
            background: `linear-gradient(135deg, ${palette.primary.main} 0%, ${palette.secondary.main} 100%)`,
            height: 3,
            borderRadius: 2,
          },
        },
      },
      MuiStepIcon: {
        styleOverrides: {
          root: {
            '&.Mui-active': {
              color: palette.primary.main,
            },
            '&.Mui-completed': {
              color: palette.success.main,
            },
          },
        },
      },
    },
  });
};

// Theme Provider Component
export const CustomThemeProvider = ({ children }) => {
  const [mode, setMode] = useState(() => {
    // Auto-detect based on time of day or user preference
    const savedMode = localStorage.getItem('themeMode');
    if (savedMode) {
      return savedMode;
    }
    
    // Auto-detect based on time (6 AM - 6 PM = light, 6 PM - 6 AM = dark)
    const hour = new Date().getHours();
    return (hour >= 6 && hour < 18) ? 'light' : 'dark';
  });

  const theme = createCustomTheme(mode);

  // Auto theme switching based on time
  useEffect(() => {
    const checkTime = () => {
      const savedMode = localStorage.getItem('themeMode');
      if (!savedMode) { // Only auto-switch if user hasn't manually set preference
        const hour = new Date().getHours();
        const autoMode = (hour >= 6 && hour < 18) ? 'light' : 'dark';
        if (autoMode !== mode) {
          setMode(autoMode);
        }
      }
    };

    // Check every hour
    const interval = setInterval(checkTime, 3600000);
    return () => clearInterval(interval);
  }, [mode]);

  const toggleTheme = () => {
    const newMode = mode === 'light' ? 'dark' : 'light';
    setMode(newMode);
    localStorage.setItem('themeMode', newMode);
  };

  const setAutoTheme = () => {
    localStorage.removeItem('themeMode');
    const hour = new Date().getHours();
    const autoMode = (hour >= 6 && hour < 18) ? 'light' : 'dark';
    setMode(autoMode);
  };

  const value = {
    mode,
    toggleTheme,
    setAutoTheme,
    isDark: mode === 'dark',
    isLight: mode === 'light',
    colors: greenPalettes[mode],
  };

  return (
    <ThemeContext.Provider value={value}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};

// Hook to use theme context
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a CustomThemeProvider');
  }
  return context;
};

export default CustomThemeProvider;
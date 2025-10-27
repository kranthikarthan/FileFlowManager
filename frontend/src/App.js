import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SnackbarProvider } from 'notistack';
import { QueryClient, QueryClientProvider } from 'react-query';
import { MsalProvider } from '@azure/msal-react';
import { PublicClientApplication } from '@azure/msal-browser';

import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Services from './pages/Services';
import FileTransactions from './pages/FileTransactions';
import AdminPanel from './pages/AdminPanel';
import { msalConfig } from './services/authConfig';
import { AuthenticatedTemplate, UnauthenticatedTemplate } from '@azure/msal-react';
import LoginPage from './pages/LoginPage';

// Create MSAL instance
const msalInstance = new PublicClientApplication(msalConfig);

// Create query client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Create Material-UI theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    h4: {
      fontWeight: 600,
    },
    h5: {
      fontWeight: 600,
    },
  },
});

function App() {
  return (
    <MsalProvider instance={msalInstance}>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <SnackbarProvider maxSnack={3} anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
            <Router>
              <AuthenticatedTemplate>
                <Layout>
                  <Routes>
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/services" element={<Services />} />
                    <Route path="/file-transactions" element={<FileTransactions />} />
                    <Route path="/admin" element={<AdminPanel />} />
                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                  </Routes>
                </Layout>
              </AuthenticatedTemplate>
              <UnauthenticatedTemplate>
                <LoginPage />
              </UnauthenticatedTemplate>
            </Router>
          </SnackbarProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </MsalProvider>
  );
}

export default App;
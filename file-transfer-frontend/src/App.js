import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Container, Box } from '@mui/material';
import { Navigation } from './components/Navigation';
import { FileTransferList } from './components/FileTransferList';
import { ServiceManagement } from './components/ServiceManagement';
import { Dashboard } from './components/Dashboard';

function App() {
  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            File Transfer Management System
          </Typography>
        </Toolbar>
      </AppBar>
      
      <Navigation />
      
      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transfers" element={<FileTransferList />} />
          <Route path="/services" element={<ServiceManagement />} />
        </Routes>
      </Container>
    </Box>
  );
}

export default App;
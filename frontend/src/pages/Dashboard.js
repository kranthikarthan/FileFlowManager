import React from 'react';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

// Mock data - replace with actual API calls
const fileStatusData = [
  { name: 'Processed', value: 450, color: '#4caf50' },
  { name: 'Processing', value: 23, color: '#ff9800' },
  { name: 'Failed', value: 12, color: '#f44336' },
  { name: 'Pending', value: 8, color: '#2196f3' },
];

const dailyVolumeData = [
  { day: 'Mon', files: 120 },
  { day: 'Tue', files: 98 },
  { day: 'Wed', files: 134 },
  { day: 'Thu', files: 87 },
  { day: 'Fri', files: 156 },
  { day: 'Sat', files: 45 },
  { day: 'Sun', files: 23 },
];

const recentAlerts = [
  { service: 'INV001', message: 'EOT not received before cutoff', time: '2 mins ago', severity: 'error' },
  { service: 'PAY002', message: 'File validation failed', time: '15 mins ago', severity: 'warning' },
  { service: 'ORD003', message: 'Service running normally', time: '1 hour ago', severity: 'info' },
];

export default function Dashboard() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      
      <Grid container spacing={3}>
        {/* Summary Cards */}
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Files Today
              </Typography>
              <Typography variant="h4" component="h2">
                493
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Successfully Processed
              </Typography>
              <Typography variant="h4" component="h2" color="success.main">
                450
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Failed Files
              </Typography>
              <Typography variant="h4" component="h2" color="error.main">
                12
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Services
              </Typography>
              <Typography variant="h4" component="h2">
                24
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* File Status Chart */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              File Status Distribution
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={fileStatusData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {fileStatusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Daily Volume Chart */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Daily File Volume (This Week)
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={dailyVolumeData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="files" fill="#1976d2" />
              </BarChart>
            </ResponsiveContainer>
          </Paper>
        </Grid>

        {/* Recent Alerts */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Alerts
            </Typography>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Service</TableCell>
                    <TableCell>Message</TableCell>
                    <TableCell>Time</TableCell>
                    <TableCell>Severity</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recentAlerts.map((alert, index) => (
                    <TableRow key={index}>
                      <TableCell>{alert.service}</TableCell>
                      <TableCell>{alert.message}</TableCell>
                      <TableCell>{alert.time}</TableCell>
                      <TableCell>
                        <Typography
                          variant="body2"
                          color={
                            alert.severity === 'error' ? 'error.main' :
                            alert.severity === 'warning' ? 'warning.main' : 'info.main'
                          }
                        >
                          {alert.severity.toUpperCase()}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
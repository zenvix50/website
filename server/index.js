import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

import connectDB from './config/db.js';

import releasesRouter from './routes/releases.js';
import downloadsRouter from './routes/downloads.js';
import subscribeRouter from './routes/subscribe.js';

import errorHandler from './middleware/errorHandler.js';

dotenv.config();

// MongoDB Connect
connectDB();

const app = express();

// __dirname Fix for ES Modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Middlewares
app.use(helmet());

app.use(cors({
  origin: '*',
  credentials: true
}));

app.use(morgan('dev'));
app.use(express.json());

// API Test Route
app.get('/api', (req, res) => {
  res.send('ZenviX API Running');
});

// API Routes
app.use('/api/releases', releasesRouter);
app.use('/api/downloads', downloadsRouter);
app.use('/api/subscribe', subscribeRouter);

// ================================
// FRONTEND BUILD FILES
// ================================

const clientPath = path.join(__dirname, '../client/dist');

app.use(express.static(clientPath));

// React Routes Support
app.get('*', (req, res) => {
  res.sendFile(path.join(clientPath, 'index.html'));
});

// Error Middleware
app.use(errorHandler);

// Railway PORT
const PORT = process.env.PORT || 5000;

// Start Server
const server = app.listen(PORT, () => {
  console.log(`ZenviX API running on port ${PORT}`);
});

// Error Handling
server.on('error', (err) => {
  console.error('Server Error:', err);
});

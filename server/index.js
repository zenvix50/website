import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';
<<<<<<< HEAD
=======
import path from 'path';
import { fileURLToPath } from 'url';

>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63
import connectDB from './config/db.js';

import releasesRouter from './routes/releases.js';
import downloadsRouter from './routes/downloads.js';
import subscribeRouter from './routes/subscribe.js';

import errorHandler from './middleware/errorHandler.js';

dotenv.config();

<<<<<<< HEAD
// Connect MongoDB
=======
// MongoDB Connect
>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63
connectDB();

const app = express();

<<<<<<< HEAD
=======
// __dirname Fix for ES Modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63
// Middlewares
app.use(helmet());

app.use(cors({
  origin: '*',
  credentials: true
}));

app.use(morgan('dev'));
app.use(express.json());

<<<<<<< HEAD
// Test Route
app.get('/', (req, res) => {
  res.send('ZenviX API Running');
});

// Routes
=======
// API Test Route
app.get('/api', (req, res) => {
  res.send('ZenviX API Running');
});

// API Routes
>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63
app.use('/api/releases', releasesRouter);
app.use('/api/downloads', downloadsRouter);
app.use('/api/subscribe', subscribeRouter);

<<<<<<< HEAD
=======
// ================================
// FRONTEND BUILD FILES
// ================================

const clientPath = path.join(__dirname, '../client/dist');

app.use(express.static(clientPath));

// React Routes Support
app.get('*', (req, res) => {
  res.sendFile(path.join(clientPath, 'index.html'));
});

>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63
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
<<<<<<< HEAD
});
=======
});
>>>>>>> 1803d81d78bf4c490906779263f193a00f186b63

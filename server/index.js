import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';
import connectDB from './config/db.js';

import releasesRouter from './routes/releases.js';
import downloadsRouter from './routes/downloads.js';
import subscribeRouter from './routes/subscribe.js';

import errorHandler from './middleware/errorHandler.js';

dotenv.config();

// Connect MongoDB
connectDB();

const app = express();

// Middlewares
app.use(helmet());

app.use(cors({
  origin: '*',
  credentials: true
}));

app.use(morgan('dev'));
app.use(express.json());

// Test Route
app.get('/', (req, res) => {
  res.send('ZenviX API Running');
});

// Routes
app.use('/api/releases', releasesRouter);
app.use('/api/downloads', downloadsRouter);
app.use('/api/subscribe', subscribeRouter);

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
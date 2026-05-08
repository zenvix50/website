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
connectDB();

const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.CLIENT_URL }));
app.use(morgan('dev'));
app.use(express.json());

app.use('/api/releases', releasesRouter);
app.use('/api/downloads', downloadsRouter);
app.use('/api/subscribe', subscribeRouter);

app.use(errorHandler);

const PORT = process.env.PORT || 5000;
const server = app.listen(PORT, () => console.log(`ZenviX API running on port ${PORT}`));

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Port ${PORT} is already in use. Kill the process or change PORT in .env`);
    process.exit(1);
  } else {
    throw err;
  }
});

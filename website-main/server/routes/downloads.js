import express from 'express';
import rateLimit from 'express-rate-limit';
import { trackDownload } from '../controllers/downloadsController.js';

const limiter = rateLimit({ windowMs: 60_000, max: 10 });
const router = express.Router();

router.post('/track', limiter, trackDownload);

export default router;

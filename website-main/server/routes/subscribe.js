import express from 'express';
import rateLimit from 'express-rate-limit';
import { subscribe } from '../controllers/subscribeController.js';

const limiter = rateLimit({ windowMs: 60_000, max: 3 });
const router = express.Router();

router.post('/', limiter, subscribe);

export default router;

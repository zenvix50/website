import express from 'express';
import { getLatestRelease, getAllReleases, syncFromGitHub } from '../controllers/releasesController.js';

const router = express.Router();

router.get('/latest', getLatestRelease);
router.get('/', getAllReleases);
router.post('/sync', syncFromGitHub);

export default router;

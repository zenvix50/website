import Download from '../models/Download.js';
import Release from '../models/Release.js';

export const trackDownload = async (req, res) => {
  try {
    const { version, os, format } = req.body;
    if (!version || !os || !format) {
      return res.status(400).json({ message: 'version, os, and format are required' });
    }

    await Download.create({
      version, os, format,
      ip: req.ip,
      userAgent: req.headers['user-agent'],
    });

    await Release.findOneAndUpdate({ version }, { $inc: { downloadCount: 1 } });

    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
};

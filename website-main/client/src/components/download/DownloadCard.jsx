import { useState } from 'react';
import { motion } from 'framer-motion';
import axios from 'axios';
import { Download, Shield } from 'lucide-react';
import { formatBytes } from '../../utils/formatBytes';
import ChecksumModal from './ChecksumModal';

export default function DownloadCard({ asset, version, isRecommended }) {
  const [showChecksum, setShowChecksum] = useState(false);

  const handleDownload = async () => {
    try {
      await axios.post('/api/downloads/track', { version, os: asset.os, format: asset.format });
    } catch (_) {}
    if (asset.url) window.open(asset.url, '_blank');
  };

  const osIcons = {
    windows: '🪟',
    macos: '🍎',
    'linux-deb': '🐧',
    'linux-rpm': '🐧',
    'linux-appimage': '🐧',
  };

  return (
    <>
      <motion.div
        whileHover={{ y: -4, boxShadow: '0 0 30px #e8342a30' }}
        className={`p-6 rounded-lg bg-bg-card border transition-all ${isRecommended ? 'border-accent' : 'border-border hover:border-accent'}`}
      >
        {isRecommended && (
          <div className="mb-3 text-xs font-mono text-accent">★ Recommended for your system</div>
        )}
        <div className="flex items-start justify-between mb-4">
          <div>
            <div className="text-3xl mb-2">{osIcons[asset.os] || '💻'}</div>
            <h3 className="text-white font-semibold">{asset.label}</h3>
            <span className="text-xs font-mono text-text-muted bg-bg-surface px-2 py-0.5 rounded mt-1 inline-block">{asset.arch}</span>
          </div>
          {asset.size && (
            <span className="text-sm text-text-muted">{formatBytes(asset.size)}</span>
          )}
        </div>

        <button
          onClick={handleDownload}
          className="w-full flex items-center justify-center gap-2 py-3 bg-accent text-white rounded-lg hover:bg-accent-hover transition-all font-medium"
        >
          <Download size={18} />
          Download {asset.format}
        </button>

        <button
          onClick={() => setShowChecksum(true)}
          className="mt-3 w-full flex items-center justify-center gap-2 py-2 text-xs text-text-muted hover:text-text-secondary transition-colors"
        >
          <Shield size={14} />
          Verify SHA-256
        </button>
      </motion.div>

      {showChecksum && (
        <ChecksumModal
          checksum={asset.checksum}
          filename={asset.label}
          onClose={() => setShowChecksum(false)}
        />
      )}
    </>
  );
}

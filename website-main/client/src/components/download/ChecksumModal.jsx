import { X } from 'lucide-react';

export default function ChecksumModal({ checksum, filename, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm" onClick={onClose}>
      <div className="bg-bg-card border border-border rounded-lg p-6 max-w-lg w-full mx-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-white font-semibold">SHA-256 Checksum</h3>
          <button onClick={onClose} className="text-text-muted hover:text-white"><X size={20} /></button>
        </div>
        <p className="text-text-secondary text-sm mb-3">{filename}</p>
        <code className="block p-3 bg-bg-surface border border-border rounded font-mono text-xs text-accent break-all">
          {checksum || 'Checksum not available'}
        </code>
      </div>
    </div>
  );
}

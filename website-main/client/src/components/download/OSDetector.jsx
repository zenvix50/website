import { useOSDetect } from '../../hooks/useOSDetect';

const osLabels = {
  windows: 'Windows',
  macos: 'macOS',
  linux: 'Linux',
  unknown: 'your system',
};

export default function OSDetector() {
  const os = useOSDetect();
  return (
    <div className="inline-flex items-center gap-2 px-4 py-2 bg-accent/10 border border-accent/30 rounded-lg text-sm text-accent font-mono">
      Recommended for your system: {osLabels[os]}
    </div>
  );
}

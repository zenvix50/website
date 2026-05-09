export default function ServiceTag({ emoji, name, version, port, status = 'running' }) {
  return (
    <div className="flex items-center gap-3 p-4 rounded-lg bg-bg-card border border-border hover:border-accent transition-all hover:scale-105 group">
      <span className="text-3xl">{emoji}</span>
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <span className="font-medium text-text-primary">{name}</span>
          <span className={`w-2 h-2 rounded-full ${status === 'running' ? 'bg-green-400 animate-pulse-glow' : 'bg-text-muted'}`} />
        </div>
        <div className="flex items-center gap-2 text-xs text-text-secondary mt-1">
          <span>{version}</span>
          {port && <span className="text-yellow-400">:{port}</span>}
        </div>
      </div>
    </div>
  );
}

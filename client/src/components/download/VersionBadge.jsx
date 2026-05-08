export default function VersionBadge({ version, isLatest }) {
  return (
    <div className="flex items-center gap-2">
      <span className="px-3 py-1 bg-bg-surface border border-border rounded font-mono text-sm text-text-primary">
        {version}
      </span>
      {isLatest && (
        <span className="px-2 py-1 bg-accent/10 border border-accent text-accent text-xs rounded font-mono">
          Latest
        </span>
      )}
    </div>
  );
}

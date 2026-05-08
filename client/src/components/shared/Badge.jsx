export default function Badge({ color = 'gray', children }) {
  const colors = {
    red: 'bg-accent/10 text-accent border-accent',
    green: 'bg-green-500/10 text-green-400 border-green-500',
    yellow: 'bg-yellow-500/10 text-yellow-400 border-yellow-500',
    blue: 'bg-blue-500/10 text-blue-400 border-blue-500',
    gray: 'bg-text-muted/10 text-text-secondary border-text-muted',
  };

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-mono border rounded-full ${colors[color]}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${color === 'red' ? 'bg-accent' : color === 'green' ? 'bg-green-400' : 'bg-text-muted'}`} />
      {children}
    </span>
  );
}

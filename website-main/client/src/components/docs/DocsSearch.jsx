import { Search } from 'lucide-react';

export default function DocsSearch({ value, onChange }) {
  return (
    <div className="relative">
      <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
      <input
        type="text"
        placeholder="Search documentation..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full pl-9 pr-4 py-2 bg-bg-card border border-border rounded-lg text-sm text-text-primary placeholder-text-muted outline-none focus:border-accent transition-colors"
      />
    </div>
  );
}

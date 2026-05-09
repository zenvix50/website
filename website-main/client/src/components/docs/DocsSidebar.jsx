import { useState } from 'react';
import { Search, ChevronDown, ChevronRight } from 'lucide-react';

const sections = [
  {
    title: 'Getting Started',
    items: ['Installation (Windows)', 'Installation (macOS)', 'Installation (Linux)', 'First Launch', 'Quick Start'],
  },
  {
    title: 'Services',
    items: ['MySQL Manager', 'PostgreSQL Manager', 'Redis Manager', 'Nginx Manager', 'Tomcat Manager', 'H2 Database', 'Memcached Manager'],
  },
  {
    title: 'Features',
    items: ['Dashboard Overview', 'Logs & Monitoring', 'Task Scheduler', 'Docker Integration', 'Security Scanner', 'JVM Profiler', 'Plugin System'],
  },
  {
    title: 'Configuration',
    items: ['Config Files', 'Environment Profiles', 'Service Groups'],
  },
  {
    title: 'API Reference',
    items: ['REST API (port 7799)', 'Plugin SDK'],
  },
  { title: 'FAQ', items: [] },
  { title: 'Changelog', items: [] },
];

export default function DocsSidebar({ active, onSelect }) {
  const [query, setQuery] = useState('');
  const [expanded, setExpanded] = useState({ 'Getting Started': true });

  const toggle = (title) => setExpanded((prev) => ({ ...prev, [title]: !prev[title] }));

  const filtered = sections
    .map((s) => ({
      ...s,
      items: s.items.filter((i) => i.toLowerCase().includes(query.toLowerCase())),
    }))
    .filter((s) => !query || s.items.length > 0 || s.title.toLowerCase().includes(query.toLowerCase()));

  return (
    <aside className="w-64 shrink-0">
      <div className="sticky top-20">
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            placeholder="Search docs..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-bg-card border border-border rounded-lg text-sm text-text-primary placeholder-text-muted outline-none focus:border-accent transition-colors"
          />
        </div>

        <nav className="space-y-1">
          {filtered.map(({ title, items }) => {
            const isLeaf = items.length === 0;
            return (
              <div key={title}>
                <button
                  onClick={() => isLeaf ? onSelect(title) : toggle(title)}
                  className={`w-full flex items-center justify-between px-3 py-2 text-sm font-medium transition-colors ${
                    isLeaf && active === title ? 'text-accent' : 'text-text-secondary hover:text-white'
                  }`}
                >
                  {title}
                  {!isLeaf && (expanded[title] ? <ChevronDown size={14} /> : <ChevronRight size={14} />)}
                </button>
                {!isLeaf && expanded[title] && items.map((item) => (
                  <button
                    key={item}
                    onClick={() => onSelect(item)}
                    className={`w-full text-left px-6 py-1.5 text-sm transition-colors ${
                      active === item ? 'text-accent' : 'text-text-muted hover:text-text-secondary'
                    }`}
                  >
                    {item}
                  </button>
                ))}
              </div>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}

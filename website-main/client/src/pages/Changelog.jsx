import { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { motion } from 'framer-motion';
import axios from 'axios';
import { Link } from 'react-router-dom';
import PageWrapper from '../components/layout/PageWrapper';
import { ExternalLink, Download } from 'lucide-react';

const fallbackReleases = [
  {
    version: '1.0.0',
    tagName: 'v1.0.0',
    releasedAt: new Date('2025-05-01'),
    isLatest: true,
    githubUrl: 'https://github.com',
    changelog: `## 🔴 Breaking Changes\n- Initial release — no breaking changes\n\n## ✅ New Features\n- Unified service dashboard\n- One-click start/stop for MySQL, PostgreSQL, Redis, Nginx, Tomcat\n- Real-time CPU/RAM monitoring\n- Log streaming with search and filter\n- Task scheduler with visual Cron editor\n- Docker integration\n- JVM profiler via JMX\n- Security scanner\n- Plugin system via Java SPI\n- Smart port conflict resolver\n- Environment profiles (Dev/Staging/Prod)\n- Keyboard command palette (Ctrl+P)\n\n## 🐛 Bug Fixes\n- N/A (initial release)\n\n## ⚡ Performance\n- Optimized service polling with configurable intervals\n- Lazy-loaded dashboard panels`,
  },
];

function renderChangelog(text) {
  if (!text) return null;

  const elements = [];
  let currentListItems = [];

  const flushList = () => {
    if (currentListItems.length > 0) {
      elements.push(
        <ul key={`ul-${elements.length}`} className="space-y-1 list-disc list-inside mb-4">
          {currentListItems}
        </ul>
      );
      currentListItems = [];
    }
  };

  text.split('\n').forEach((line, i) => {
    if (line.startsWith('## ')) {
      flushList();
      elements.push(
        <h3 key={i} className="text-lg font-semibold text-text-primary mt-6 mb-3">
          {line.replace('## ', '')}
        </h3>
      );
    } else if (line.startsWith('### ')) {
      flushList();
      elements.push(
        <h4 key={i} className="text-base font-semibold text-text-primary mt-4 mb-2">
          {line.replace('### ', '')}
        </h4>
      );
    } else if (line.startsWith('- ')) {
      currentListItems.push(
        <li key={i} className="text-text-secondary">{line.replace('- ', '')}</li>
      );
    } else if (line.trim()) {
      flushList();
      elements.push(<p key={i} className="text-text-secondary mb-2">{line}</p>);
    }
  });

  flushList();
  return elements;
}

export default function Changelog() {
  const [releases, setReleases] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get('/api/releases')
      .then(({ data }) => { setReleases(data.length ? data : fallbackReleases); setLoading(false); })
      .catch(() => { setReleases(fallbackReleases); setLoading(false); });
  }, []);

  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — Dev Environment Manager | Changelog</title>
        <meta name="description" content="zenviX version history and release notes." />
      </Helmet>

      <div className="max-w-4xl mx-auto px-4 py-20">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mb-16">
          <h1 className="text-5xl font-display font-bold text-text-primary">Changelog</h1>
          <p className="mt-4 text-text-secondary">All notable changes to zenviX.</p>
        </motion.div>

        {loading ? (
          <div className="text-text-muted text-center py-20">Loading releases...</div>
        ) : (
          <div className="relative">
            <div className="absolute left-4 top-0 bottom-0 w-px bg-border" />

            <div className="space-y-12">
              {releases.map((release, i) => (
                <motion.div
                  key={release.version}
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="relative pl-12"
                >
                  <div className={`absolute left-0 w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-bold ${release.isLatest ? 'border-accent bg-accent/20 text-accent' : 'border-border bg-bg-card text-text-muted'}`}>
                    v
                  </div>

                  <div className="p-6 bg-bg-card border border-border rounded-lg">
                    <div className="flex flex-wrap items-center gap-3 mb-4">
                      <span className="text-xl font-display font-bold text-text-primary">{release.tagName || `v${release.version}`}</span>
                      {release.isLatest && (
                        <span className="px-2 py-0.5 bg-accent/10 border border-accent text-accent text-xs rounded font-mono">Latest</span>
                      )}
                      {release.releasedAt && (
                        <span className="text-text-muted text-sm">
                          {new Date(release.releasedAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
                        </span>
                      )}
                    </div>

                    <div className="prose prose-invert max-w-none">
                      {renderChangelog(release.changelog)}
                    </div>

                    <div className="mt-6 flex gap-3">
                      {release.githubUrl && (
                        <a
                          href={release.githubUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-2 text-sm text-text-secondary hover:text-accent transition-colors"
                        >
                          <ExternalLink size={14} />
                          GitHub Release
                        </a>
                      )}
                      <Link
                        to="/download"
                        className="flex items-center gap-2 text-sm text-text-secondary hover:text-accent transition-colors"
                      >
                        <Download size={14} />
                        Download
                      </Link>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PageWrapper>
  );
}

import { Link } from 'react-router-dom';
import { Github, Twitter } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="bg-bg-surface border-t border-border">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Col 1 */}
          <div>
            <div className="flex items-center gap-2 font-display text-xl font-bold mb-3">
              <span className="w-8 h-8 rounded bg-accent flex items-center justify-center text-white font-black text-lg">Z</span>
              <span className="text-text-primary">zenviX</span>
            </div>
            <p className="text-text-secondary text-sm mb-4">Local dev, unified.</p>
            <div className="flex gap-3">
              <a href="https://github.com" target="_blank" rel="noopener noreferrer" className="text-text-muted hover:text-white transition-colors">
                <Github size={20} />
              </a>
              <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="text-text-muted hover:text-white transition-colors">
                <Twitter size={20} />
              </a>
            </div>
          </div>

          {/* Col 2 */}
          <div>
            <h4 className="text-text-primary font-medium mb-4">Links</h4>
            <ul className="space-y-2 text-sm text-text-secondary">
              {[['Home', '/'], ['Download', '/download'], ['Features', '/features'], ['Docs', '/docs'], ['Changelog', '/changelog']].map(([label, to]) => (
                <li key={to}><Link to={to} className="hover:text-accent transition-colors">{label}</Link></li>
              ))}
            </ul>
          </div>

          {/* Col 3 */}
          <div>
            <h4 className="text-text-primary font-medium mb-4">Community</h4>
            <ul className="space-y-2 text-sm text-text-secondary">
              <li><a href="https://github.com/issues" target="_blank" rel="noopener noreferrer" className="hover:text-accent transition-colors">GitHub Issues</a></li>
              <li><a href="https://github.com/discussions" target="_blank" rel="noopener noreferrer" className="hover:text-accent transition-colors">Discussions</a></li>
            </ul>
            <h4 className="text-text-primary font-medium mt-6 mb-3">Built with</h4>
            <div className="flex flex-wrap gap-2 text-xs font-mono text-text-muted">
              {['Java 21', 'JavaFX', 'Spring Boot'].map(t => (
                <span key={t} className="px-2 py-1 border border-border rounded">{t}</span>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-10 pt-6 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-text-muted">
          <span>© 2025 zenviX. Free &amp; Open Source.</span>
          <span className="px-2 py-1 border border-border rounded font-mono text-xs">v1.0.0</span>
        </div>
      </div>
    </footer>
  );
}

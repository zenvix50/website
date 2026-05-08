import { motion } from 'framer-motion';
import { useScrollAnimation } from '../../hooks/useScrollAnimation';

export default function DashboardMockup() {
  const { ref, isVisible } = useScrollAnimation();

  return (
    <section ref={ref} className="py-20 bg-bg-surface">
      <div className="max-w-6xl mx-auto px-4">
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={isVisible ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.8 }}
          className="rounded-lg overflow-hidden border border-border shadow-2xl"
        >
          {/* Window chrome */}
          <div className="bg-bg-surface px-4 py-3 flex items-center gap-2 border-b border-border">
            <div className="flex gap-2">
              <span className="w-3 h-3 rounded-full bg-red-500" />
              <span className="w-3 h-3 rounded-full bg-yellow-500" />
              <span className="w-3 h-3 rounded-full bg-green-500" />
            </div>
            <span className="ml-4 text-sm font-medium text-text-primary">zenviX — Dev Environment Manager</span>
          </div>

          {/* Dashboard content */}
          <div className="bg-bg-card p-6">
            {/* Top bar */}
            <div className="flex items-center justify-between mb-6 pb-4 border-b border-border">
              <input
                type="text"
                placeholder="Search services..."
                className="px-4 py-2 bg-bg-surface border border-border rounded text-sm text-text-primary placeholder-text-muted outline-none"
                readOnly
              />
              <div className="flex gap-4 text-xs font-mono text-text-secondary">
                <span>CPU: <span className="text-accent">12%</span></span>
                <span>RAM: <span className="text-accent">4.2GB</span></span>
                <span>HEAP: <span className="text-accent">512MB</span></span>
              </div>
            </div>

            {/* Stat cards */}
            <div className="grid grid-cols-4 gap-4 mb-6">
              {[
                { label: 'Running', value: '3', color: 'text-green-400' },
                { label: 'Stopped', value: '7', color: 'text-text-muted' },
                { label: 'Alerts', value: '1', color: 'text-yellow-400' },
                { label: 'Uptime', value: '99.8%', color: 'text-accent' },
              ].map(({ label, value, color }, i) => (
                <div key={i} className="p-4 bg-bg-surface border border-border rounded">
                  <div className={`text-2xl font-bold ${color}`}>{value}</div>
                  <div className="text-xs text-text-muted mt-1">{label}</div>
                </div>
              ))}
            </div>

            {/* Services table */}
            <div className="border border-border rounded overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-bg-surface border-b border-border">
                  <tr className="text-left text-text-muted">
                    <th className="px-4 py-2 font-medium">Service</th>
                    <th className="px-4 py-2 font-medium">Status</th>
                    <th className="px-4 py-2 font-medium">Port</th>
                    <th className="px-4 py-2 font-medium">CPU</th>
                  </tr>
                </thead>
                <tbody className="font-mono text-xs">
                  {[
                    { name: 'MySQL', status: 'RUNNING', port: '3306', cpu: '0.8%', statusColor: 'text-green-400' },
                    { name: 'Redis', status: 'RUNNING', port: '6379', cpu: '0.2%', statusColor: 'text-green-400' },
                    { name: 'Nginx', status: 'STOPPED', port: '--', cpu: '--', statusColor: 'text-text-muted' },
                  ].map((row, i) => (
                    <tr key={i} className="border-b border-border/50">
                      <td className="px-4 py-3 text-text-primary">{row.name}</td>
                      <td className={`px-4 py-3 ${row.statusColor}`}>● {row.status}</td>
                      <td className="px-4 py-3 text-yellow-400">{row.port}</td>
                      <td className="px-4 py-3 text-text-secondary">{row.cpu}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Mini log panel */}
            <div className="mt-4 p-3 bg-bg-surface border border-border rounded font-mono text-xs text-text-muted">
              <div>[14:32:01] MySQL started successfully</div>
              <div>[14:32:05] Redis connected on port 6379</div>
              <div>[14:32:08] Nginx configuration loaded</div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

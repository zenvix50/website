import { Helmet } from 'react-helmet-async';
import { motion } from 'framer-motion';
import PageWrapper from '../components/layout/PageWrapper';
import { Check } from 'lucide-react';

const featureGroups = [
  {
    id: 'A',
    title: 'Service Manager',
    icon: '⚙️',
    desc: 'Full lifecycle control for every local service. Start, stop, and restart with automatic retry logic, port scanning, and health check polling. zenviX monitors each process and alerts you when a service goes down.',
    features: ['One-click start/stop/restart', 'Automatic retry on failure', 'Port conflict detection', 'Health check polling', 'Process isolation'],
  },
  {
    id: 'B',
    title: 'Real-Time Dashboard',
    icon: '📊',
    desc: 'A live overview of your entire dev stack. CPU and RAM usage per service, system-wide metrics, and live counters — all in one dark, professional dashboard.',
    features: ['Per-service CPU/RAM graphs', 'System-wide metrics', 'Live counters', 'Uptime tracking', 'Alert notifications'],
  },
  {
    id: 'C',
    title: 'Log Streaming',
    icon: '📝',
    desc: 'Tail logs from any service in real time. Filter by log level, search across output, and export logs for debugging. Syntax highlighting makes log reading fast.',
    features: ['Real-time log tailing', 'Filter by log level', 'Full-text search', 'Log export', 'Syntax highlighting'],
  },
  {
    id: 'D',
    title: 'Embedded Terminal',
    icon: '💻',
    desc: 'A full terminal emulator built directly into zenviX. Run commands, manage services via CLI, and interact with your dev environment without leaving the app.',
    features: ['Full terminal emulator', 'Multi-tab support', 'Command history', 'Shell integration', 'Custom themes'],
  },
  {
    id: 'E',
    title: 'Task Scheduler',
    icon: '⏰',
    desc: 'Schedule recurring tasks with a visual Cron editor. Set up automated backups, build jobs, and health reports without writing cron syntax manually.',
    features: ['Visual Cron editor', 'Backup automation', 'Build job scheduling', 'Health reports', 'Email notifications'],
  },
  {
    id: 'F',
    title: 'Docker Integration',
    icon: '🐋',
    desc: 'Manage docker-compose stacks alongside native services from the same dashboard. Start, stop, and inspect containers without switching tools.',
    features: ['docker-compose support', 'Container lifecycle control', 'Volume management', 'Network inspection', 'Image management'],
  },
  {
    id: 'G',
    title: 'JVM Profiler',
    icon: '☕',
    desc: 'Deep JVM insights via JMX. Monitor heap usage, thread count, garbage collection stats, and class loading — essential for Java service debugging.',
    features: ['Heap monitoring', 'Thread count tracking', 'GC statistics', 'Class loading stats', 'JMX integration'],
  },
  {
    id: 'H',
    title: 'Security Scanner',
    icon: '🔒',
    desc: 'Proactive security checks for your local environment. Port firewall analysis, process isolation verification, SSL certificate checks, and JVM security profiling.',
    features: ['Port firewall analysis', 'Process isolation checks', 'SSL certificate validation', 'JVM security profiling', 'Vulnerability alerts'],
  },
  {
    id: 'I',
    title: 'Plugin System',
    icon: '🧩',
    desc: 'Extend zenviX with custom service plugins using the Java SPI interface. 30+ service templates are built-in, and the community plugin registry grows daily.',
    features: ['Java SPI interface', '30+ built-in templates', 'Community registry', 'Hot-reload plugins', 'Plugin SDK'],
  },
  {
    id: 'J',
    title: 'Smart Port Conflict Resolver',
    icon: '🔌',
    desc: 'When a port is already in use, zenviX shows you exactly which PID owns it and offers to kill the conflicting process — no manual lsof or netstat needed.',
    features: ['PID identification', 'One-click process kill', 'Port history', 'Conflict prevention', 'Auto-reassign ports'],
  },
  {
    id: 'K',
    title: 'Environment Profiles',
    icon: '🌍',
    desc: 'Switch between Dev, Staging, and Production profiles with a single click. Each profile stores its own service configuration, environment variables, and startup order.',
    features: ['Dev/Staging/Prod profiles', 'Per-profile config', 'Environment variables', 'Startup order control', 'Profile export/import'],
  },
  {
    id: 'L',
    title: 'Keyboard Command Palette',
    icon: '⌨️',
    desc: 'A Ctrl+P spotlight-style command palette to trigger any action in zenviX without touching the mouse. Search services, run commands, and navigate instantly.',
    features: ['Ctrl+P shortcut', 'Fuzzy search', 'Service quick-start', 'Action shortcuts', 'Custom keybindings'],
  },
];

export default function Features() {
  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — Dev Environment Manager | Features</title>
        <meta name="description" content="Explore all zenviX features: service manager, log streaming, Docker integration, scheduler, and more." />
      </Helmet>

      <div className="max-w-6xl mx-auto px-4 py-20">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="text-center mb-20">
          <h1 className="text-5xl md:text-6xl font-display font-bold text-white">
            Built for developers who care<br />about their workflow.
          </h1>
          <p className="mt-6 text-xl text-text-secondary max-w-2xl mx-auto">
            Every feature in zenviX is designed to eliminate friction from your local development environment.
          </p>
        </motion.div>

        <div className="space-y-20">
          {featureGroups.map(({ id, title, icon, desc, features }, i) => (
            <motion.div
              key={id}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className={`flex flex-col ${i % 2 === 0 ? 'md:flex-row' : 'md:flex-row-reverse'} gap-12 items-center`}
            >
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-4xl">{icon}</span>
                  <h2 className="text-3xl font-display font-bold text-white">{title}</h2>
                </div>
                <p className="text-text-secondary mb-6 leading-relaxed">{desc}</p>
                <ul className="space-y-2">
                  {features.map((f) => (
                    <li key={f} className="flex items-center gap-3 text-text-secondary">
                      <Check size={16} className="text-accent shrink-0" />
                      {f}
                    </li>
                  ))}
                </ul>
              </div>

              <div className="flex-1 w-full">
                <div className="p-6 bg-bg-card border border-border rounded-lg hover:border-accent transition-all">
                  <div className="flex items-center gap-2 mb-4">
                    <div className="flex gap-1.5">
                      <span className="w-3 h-3 rounded-full bg-red-500" />
                      <span className="w-3 h-3 rounded-full bg-yellow-500" />
                      <span className="w-3 h-3 rounded-full bg-green-500" />
                    </div>
                    <span className="text-xs font-mono text-text-muted">zenviX — {title}</span>
                  </div>
                  <div className="h-32 bg-bg-surface rounded flex items-center justify-center">
                    <span className="text-6xl">{icon}</span>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </PageWrapper>
  );
}

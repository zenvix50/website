import { motion } from 'framer-motion';
import { useScrollAnimation } from '../../hooks/useScrollAnimation';
import { Monitor, Zap, Activity, FileText, Container, Clock, Shield, Puzzle } from 'lucide-react';

const features = [
  { icon: Monitor, title: 'Unified Dashboard', desc: 'Monitor CPU, RAM, and port usage for every service in a single dark dashboard.' },
  { icon: Zap, title: 'One-Click Control', desc: 'Start, stop, and restart MySQL, Nginx, Redis, and more without touching the terminal.' },
  { icon: Activity, title: 'Real-Time Metrics', desc: 'Live CPU and memory graphs per service. Identify bottlenecks instantly.' },
  { icon: FileText, title: 'Log Streaming', desc: 'Tail logs from any service in real time with search, filter, and syntax highlighting.' },
  { icon: Container, title: 'Docker Integration', desc: 'Manage docker-compose stacks alongside native services from the same dashboard.' },
  { icon: Clock, title: 'Task Scheduler', desc: 'Schedule backups, builds, and health checks with a visual Cron editor.' },
  { icon: Shield, title: 'Security Scanner', desc: 'Port conflict detection, process isolation, firewall checks, and JVM profiling.' },
  { icon: Puzzle, title: 'Plugin System', desc: 'Extend zenviX with custom service plugins using the Java SPI interface.' },
];

export default function FeaturesSection() {
  const { ref, isVisible } = useScrollAnimation();

  return (
    <section ref={ref} className="py-20 bg-bg-surface">
      <div className="max-w-7xl mx-auto px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={isVisible ? { opacity: 1, y: 0 } : {}}
          className="text-center mb-12"
        >
          <h2 className="text-4xl md:text-5xl font-display font-bold text-white">Everything You Need to Dev Faster</h2>
        </motion.div>

        <motion.div
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
          initial="hidden"
          animate={isVisible ? 'visible' : 'hidden'}
          variants={{ visible: { transition: { staggerChildren: 0.08 } } }}
        >
          {features.map(({ icon: Icon, title, desc }, i) => (
            <motion.div
              key={i}
              variants={{ hidden: { opacity: 0, y: 20 }, visible: { opacity: 1, y: 0 } }}
              className="p-6 rounded-lg bg-bg-card border border-border hover:border-accent transition-all group"
            >
              <div className="w-12 h-12 rounded-full bg-accent/10 flex items-center justify-center mb-4 group-hover:bg-accent/20 transition-colors">
                <Icon className="text-accent" size={24} />
              </div>
              <h3 className="text-xl font-semibold text-white mb-2">{title}</h3>
              <p className="text-text-secondary text-sm">{desc}</p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

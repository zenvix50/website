import { motion } from 'framer-motion';
import { Download, Github } from 'lucide-react';
import Button from '../shared/Button';
import Badge from '../shared/Badge';
import GradientText from '../shared/GradientText';

export default function HeroSection() {
  return (
    <section className="relative min-h-screen flex items-center justify-center grid-bg scanline-overlay overflow-hidden">
      {/* Floating dots */}
      {[...Array(5)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute w-2 h-2 rounded-full bg-accent"
          style={{ left: `${20 + i * 15}%`, top: `${30 + i * 10}%` }}
          animate={{ y: [0, -12, 0] }}
          transition={{ duration: 6, repeat: Infinity, delay: i * 0.5 }}
        />
      ))}

      <div className="relative z-10 max-w-5xl mx-auto px-4 text-center">
        <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <Badge color="red">zenviX v1.0</Badge>
        </motion.div>

        <motion.h1
          className="mt-8 font-display text-6xl md:text-8xl font-bold leading-tight"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
        >
          Your Local Dev<br />Environment.<br />
          <GradientText className="drop-shadow-[0_0_30px_#e8342a80]">Unified.</GradientText>
        </motion.h1>

        <motion.p
          className="mt-6 text-lg md:text-xl text-text-secondary max-w-3xl mx-auto"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          Start, stop, and monitor every local service — MySQL, Postgres, Redis, Nginx, Tomcat — from one professional dashboard.
          No terminal. No scripts. Just control.
        </motion.p>

        <motion.div
          className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <Button variant="primary" size="lg" icon={<Download size={20} />} href="/download">
            Download for Windows
          </Button>
          <Button variant="ghost" size="lg" icon={<Github size={20} />} onClick={() => window.open('https://github.com', '_blank')}>
            View on GitHub
          </Button>
        </motion.div>

        <motion.div
          className="mt-8 text-sm text-text-muted font-mono"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.8 }}
        >
          10+ Services · Cross-Platform · Java 21 · Free &amp; Open Source
        </motion.div>
      </div>
    </section>
  );
}

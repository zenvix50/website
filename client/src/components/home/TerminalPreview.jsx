import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

const lines = [
  { cmd: '$ zenvix start mysql', output: '[  OK  ] MySQL 8.0.36 started on port 3306 (PID 14291)' },
  { cmd: '$ zenvix start redis', output: '[  OK  ] Redis 7.2.4 started on port 6379 (PID 14305)' },
  { cmd: '$ zenvix status', output: 'SERVICE          STATUS    PORT    PID     CPU    RAM\nmysql            RUNNING   3306    14291   0.8%   128MB\nredis            RUNNING   6379    14305   0.2%    45MB\nnginx            STOPPED   --      --       --      --' },
  { cmd: '$ zenvix start nginx', output: '[  OK  ] Nginx 1.25 started on port 80 (PID 14318)' },
];

export default function TerminalPreview() {
  const [visibleCount, setVisibleCount] = useState(1);

  useEffect(() => {
    if (visibleCount >= lines.length) return;
    const timer = setTimeout(() => setVisibleCount((v) => v + 1), 2500);
    return () => clearTimeout(timer);
  }, [visibleCount]);

  return (
    <section className="py-20 bg-bg-primary">
      <div className="max-w-4xl mx-auto px-4">
        <div className="rounded-lg overflow-hidden border border-border shadow-2xl">
          <div className="bg-bg-surface px-4 py-3 flex items-center gap-2 border-b border-border">
            <div className="flex gap-2">
              <span className="w-3 h-3 rounded-full bg-red-500" />
              <span className="w-3 h-3 rounded-full bg-yellow-500" />
              <span className="w-3 h-3 rounded-full bg-green-500" />
            </div>
            <span className="ml-4 text-sm font-mono text-text-muted">zenviX — Terminal</span>
          </div>

          <div className="bg-bg-card p-6 font-mono text-sm min-h-[300px]">
            <AnimatePresence>
              {lines.slice(0, visibleCount).map((line, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3 }}
                >
                  <div className="text-cyan-400 mb-1">{line.cmd}</div>
                  <div className="text-green-400 mb-4 whitespace-pre-line">{line.output}</div>
                </motion.div>
              ))}
            </AnimatePresence>
            <span className="inline-block w-2 h-4 bg-accent animate-blink" />
          </div>
        </div>
      </div>
    </section>
  );
}

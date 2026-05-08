import { useState } from 'react';
import { motion } from 'framer-motion';
import axios from 'axios';
import Button from '../shared/Button';
import { Download, Star, ArrowRight } from 'lucide-react';

export default function CTASection() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState('');

  const handleSubscribe = async (e) => {
    e.preventDefault();
    try {
      const { data } = await axios.post('/api/subscribe', { email });
      setStatus(data.message);
      setEmail('');
    } catch (err) {
      setStatus('Error subscribing. Please try again.');
    }
  };

  return (
    <section className="py-20 bg-gradient-to-b from-bg-primary to-bg-surface relative overflow-hidden">
      <div className="absolute inset-0 bg-accent/5 bg-[radial-gradient(circle_at_50%_50%,#e8342a20,transparent_70%)]" />
      
      <div className="relative z-10 max-w-4xl mx-auto px-4 text-center">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-4xl md:text-5xl font-display font-bold text-white"
        >
          Start Managing Your Dev Stack Today.
        </motion.h2>
        
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="mt-4 text-xl text-text-secondary"
        >
          Free. Open source. One download.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.2 }}
          className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Button variant="primary" size="lg" icon={<Download size={20} />} href="/download">
            Download Now
          </Button>
          <Button variant="outline" size="lg" icon={<Star size={20} />} onClick={() => window.open('https://github.com', '_blank')}>
            Star on GitHub
          </Button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.3 }}
          className="mt-16"
        >
          <p className="text-text-secondary mb-4">Get notified on new releases</p>
          <form onSubmit={handleSubscribe} className="flex flex-col sm:flex-row items-center justify-center gap-3 max-w-md mx-auto">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="your@email.com"
              className="w-full px-4 py-3 bg-bg-card border border-border rounded-lg text-text-primary placeholder-text-muted outline-none focus:border-accent transition-colors"
              required
            />
            <button
              type="submit"
              className="w-full sm:w-auto px-6 py-3 bg-accent text-white rounded-lg hover:bg-accent-hover transition-all flex items-center justify-center gap-2"
            >
              Notify Me <ArrowRight size={16} />
            </button>
          </form>
          {status && <p className="mt-3 text-sm text-accent">{status}</p>}
        </motion.div>
      </div>
    </section>
  );
}

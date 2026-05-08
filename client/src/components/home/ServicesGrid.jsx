import { motion } from 'framer-motion';
import ServiceTag from '../shared/ServiceTag';
import { useScrollAnimation } from '../../hooks/useScrollAnimation';

const services = [
  { emoji: '🐬', name: 'MySQL', version: '8.0.36', port: '3306' },
  { emoji: '🐘', name: 'PostgreSQL', version: '15.4', port: '5432' },
  { emoji: '🔴', name: 'Redis', version: '7.2.4', port: '6379' },
  { emoji: '🌿', name: 'Nginx', version: '1.25', port: '80' },
  { emoji: '🐱', name: 'Tomcat', version: '10.1.18', port: '8080' },
  { emoji: '💠', name: 'H2 Database', version: '2.2.224', port: '9092' },
  { emoji: '🔥', name: 'Memcached', version: '1.6.23', port: '11211' },
  { emoji: '📦', name: 'Maven', version: '3.9.6', port: null },
  { emoji: '🏗️', name: 'Gradle', version: '8.6', port: null },
  { emoji: '☕', name: 'JDK 21 LTS', version: 'Runtime', port: null },
];

const containerVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.05 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0 },
};

export default function ServicesGrid() {
  const { ref, isVisible } = useScrollAnimation();

  return (
    <section ref={ref} className="py-20 bg-bg-primary">
      <div className="max-w-7xl mx-auto px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={isVisible ? { opacity: 1, y: 0 } : {}}
          className="text-center mb-12"
        >
          <h2 className="text-4xl md:text-5xl font-display font-bold text-white">Every Service You Need</h2>
          <p className="mt-4 text-lg text-text-secondary">Pre-configured and ready to run on first launch.</p>
        </motion.div>

        <motion.div
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4"
          initial="hidden"
          animate={isVisible ? 'visible' : 'hidden'}
          variants={containerVariants}
        >
          {services.map((service, i) => (
            <motion.div
              key={i}
              variants={itemVariants}
            >
              <ServiceTag {...service} />
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

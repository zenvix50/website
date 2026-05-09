import { Helmet } from 'react-helmet-async';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import PageWrapper from '../components/layout/PageWrapper';

export default function NotFound() {
  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — 404 Not Found</title>
      </Helmet>
      <div className="min-h-[80vh] flex items-center justify-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center"
        >
          <div className="text-8xl font-display font-bold text-accent mb-4">404</div>
          <h1 className="text-3xl font-display font-bold text-white mb-4">Page Not Found</h1>
          <p className="text-text-secondary mb-8">The page you're looking for doesn't exist.</p>
          <Link
            to="/"
            className="px-6 py-3 bg-accent text-white rounded-lg hover:bg-accent-hover transition-all"
          >
            Back to Home
          </Link>
        </motion.div>
      </div>
    </PageWrapper>
  );
}

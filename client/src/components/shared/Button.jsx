import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

export default function Button({ variant = 'primary', size = 'md', icon, children, href, onClick, loading, className = '' }) {
  const baseClass = 'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200 rounded-lg';
  
  const variants = {
    primary: 'bg-accent text-white hover:bg-accent-hover shadow-[0_0_20px_#e8342a40]',
    ghost: 'border border-border text-text-secondary hover:border-accent hover:text-white',
    outline: 'border border-accent text-accent hover:bg-accent hover:text-white',
  };

  const sizes = {
    sm: 'px-4 py-2 text-sm',
    md: 'px-6 py-3 text-base',
    lg: 'px-8 py-4 text-lg',
  };

  const classes = `${baseClass} ${variants[variant]} ${sizes[size]} ${className}`;

  const content = loading ? <span className="animate-spin">⏳</span> : <>{icon && <span>{icon}</span>}{children}</>;

  if (href) {
    const MotionLink = motion(Link);
    return (
      <MotionLink to={href} className={classes} whileTap={{ scale: 0.97 }} whileHover={{ scale: 1.02 }}>
        {content}
      </MotionLink>
    );
  }

  return (
    <motion.button
      onClick={onClick}
      className={classes}
      whileTap={{ scale: 0.97 }}
      whileHover={{ scale: 1.02 }}
    >
      {content}
    </motion.button>
  );
}

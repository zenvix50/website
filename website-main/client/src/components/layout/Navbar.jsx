import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, Download, Sun, Moon } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const navLinks = [
  { label: 'Features', to: '/features' },
  { label: 'Download', to: '/download' },
  { label: 'Docs', to: '/docs' },
  { label: 'Changelog', to: '/changelog' },
];

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { theme, toggleTheme } = useTheme();

  return (
    <header className={`sticky top-0 z-50 h-16 border-b transition-colors duration-300 backdrop-blur-md ${
      theme === 'dark'
        ? 'bg-[#0a0a0b]/90 border-[#2a2a35]'
        : 'bg-white/90 border-[#e5e5eb]'
    }`}>
      <div className="max-w-7xl mx-auto px-4 h-full flex items-center justify-between">
        
        {/* Logo */}
        <Link
          to="/"
          className="flex items-center gap-2 font-display text-xl font-bold"
        >
          <span className={`w-8 h-8 rounded flex items-center justify-center font-black text-lg ${theme === 'dark' ? 'bg-accent text-white' : 'bg-black text-white'}`}>
            Z
          </span>
          <span className={theme === 'dark' ? 'text-white' : 'text-black'}>zenviX</span>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-8">
          {navLinks.map(({ label, to }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `text-sm transition-colors ${
                  isActive
                    ? 'text-accent'
                    : 'text-text-secondary hover:text-text-primary'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Desktop Right */}
        <div className="hidden md:flex items-center gap-3">
          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            className="flex items-center justify-center w-10 h-10 rounded-lg border border-border hover:border-accent transition-all text-text-secondary hover:text-text-primary"
            aria-label="Toggle theme"
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-2 text-sm text-text-secondary border border-border rounded-lg hover:border-accent hover:text-white transition-all"
          >
            <span>GitHub ★</span>
          </a>

          <Link
            to="/download"
            className="flex items-center gap-2 px-4 py-2 text-sm bg-accent text-white rounded-lg hover:bg-accent-hover transition-all shadow-[0_0_20px_#e8342a40]"
          >
            <Download size={16} />
            Download
          </Link>
        </div>

        {/* Mobile Hamburger */}
        <div className="md:hidden flex items-center gap-2">
          {/* Mobile Theme Toggle */}
          <button
            onClick={toggleTheme}
            className="flex items-center justify-center w-10 h-10 rounded-lg border border-border hover:border-accent transition-all text-text-secondary hover:text-text-primary"
            aria-label="Toggle theme"
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          <button
            className="text-text-secondary hover:text-white"
            onClick={() => setOpen(!open)}
            aria-label="Toggle menu"
          >
            {open ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className={`md:hidden absolute top-16 left-0 right-0 border-b px-4 py-6 flex flex-col gap-4 transition-colors duration-300 ${
              theme === 'dark'
                ? 'bg-[#111114] border-[#2a2a35]'
                : 'bg-[#f9f9f9] border-[#e5e5eb]'
            }`}
          >
            {navLinks.map(({ label, to }) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `text-base py-2 border-b border-border/50 ${
                    isActive ? 'text-accent' : 'text-text-secondary'
                  }`
                }
              >
                {label}
              </NavLink>
            ))}

            <Link
              to="/download"
              onClick={() => setOpen(false)}
              className="mt-2 flex items-center justify-center gap-2 px-4 py-3 bg-accent text-white rounded-lg"
            >
              <Download size={16} />
              Download
            </Link>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
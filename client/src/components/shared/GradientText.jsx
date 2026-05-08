export default function GradientText({ children, className = '' }) {
  return (
    <span className={`bg-gradient-to-r from-accent to-accent-hover bg-clip-text text-transparent ${className}`}>
      {children}
    </span>
  );
}

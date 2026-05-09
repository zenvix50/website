import AnimatedCounter from '../shared/AnimatedCounter';

const stats = [
  { value: 10, suffix: '+', label: 'Supported Services' },
  { value: 3, suffix: '', label: 'OS Platforms' },
  { value: 40, suffix: '+', label: 'Built-in Modules' },
  { value: 100, suffix: '%', label: 'Free & Open Source' },
];

export default function StatsBar() {
  return (
    <section className="bg-bg-surface border-y border-border py-12">
      <div className="max-w-7xl mx-auto px-4 grid grid-cols-2 md:grid-cols-4 gap-8">
        {stats.map(({ value, suffix, label }, i) => (
          <div key={i} className="text-center">
            <div className="text-4xl md:text-5xl font-display font-bold text-accent">
              <AnimatedCounter target={value} suffix={suffix} />
            </div>
            <div className="mt-2 text-sm text-text-secondary">{label}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

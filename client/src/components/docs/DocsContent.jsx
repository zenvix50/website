import CodeBlock from '../shared/CodeBlock';

const docs = {
  'Quick Start': {
    title: 'Quick Start',
    content: () => (
      <div className="space-y-6">
        <p className="text-text-secondary">ZenviX is a local developer environment manager. Download and launch the installer.</p>
        <p className="text-text-secondary">On first launch, zenviX will scan for installed services and auto-configure them.</p>
        <h2 className="text-2xl font-display font-bold text-white mt-8">Starting a Service</h2>
        <CodeBlock code={`Click the "Services" tab → click the ▶ Start button next to any service.`} language="text" />
        <h2 className="text-2xl font-display font-bold text-white mt-8">Requirements</h2>
        <ul className="list-disc list-inside text-text-secondary space-y-2">
          <li>Java 21+</li>
          <li>Windows 10+, macOS 12+, or Ubuntu 20.04+</li>
          <li>4GB RAM minimum</li>
        </ul>
      </div>
    ),
  },
  'Installation (Windows)': {
    title: 'Installation — Windows',
    content: () => (
      <div className="space-y-6">
        <p className="text-text-secondary">Download the <code className="text-accent font-mono">.exe</code> installer from the Download page.</p>
        <CodeBlock code={`1. Run ZenviX-Setup-1.0.0.exe\n2. Follow the installation wizard\n3. Launch zenviX from the Start Menu`} language="text" />
        <p className="text-text-secondary">Ensure Java 21 is installed and available in your PATH.</p>
        <CodeBlock code={`java -version\n# Should output: openjdk version "21.x.x"`} language="bash" />
      </div>
    ),
  },
  'Installation (macOS)': {
    title: 'Installation — macOS',
    content: () => (
      <div className="space-y-6">
        <p className="text-text-secondary">Download the <code className="text-accent font-mono">.dmg</code> file for macOS (Universal — supports both Intel and Apple Silicon).</p>
        <CodeBlock code={`1. Open ZenviX-1.0.0.dmg\n2. Drag zenviX.app to Applications\n3. Launch from Applications or Spotlight`} language="text" />
      </div>
    ),
  },
  'Installation (Linux)': {
    title: 'Installation — Linux',
    content: () => (
      <div className="space-y-6">
        <p className="text-text-secondary">Choose the package format for your distribution:</p>
        <CodeBlock code={`# Debian/Ubuntu\nsudo dpkg -i zenvix_1.0.0_amd64.deb\n\n# Red Hat/Fedora\nsudo rpm -i zenvix-1.0.0.x86_64.rpm\n\n# AppImage (portable)\nchmod +x ZenviX-1.0.0.AppImage\n./ZenviX-1.0.0.AppImage`} language="bash" />
      </div>
    ),
  },
};

const defaultDoc = {
  title: 'Documentation',
  content: () => (
    <div className="space-y-4">
      <p className="text-text-secondary">Select a topic from the sidebar to get started.</p>
    </div>
  ),
};

export default function DocsContent({ active }) {
  const doc = docs[active] || defaultDoc;
  const Content = doc.content;

  return (
    <article className="flex-1 min-w-0">
      <h1 className="text-4xl font-display font-bold text-white mb-8">{doc.title}</h1>
      <Content />
    </article>
  );
}

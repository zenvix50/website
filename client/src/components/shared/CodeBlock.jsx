import { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { atomDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Copy, Check } from 'lucide-react';

export default function CodeBlock({ code, language = 'bash', filename }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative rounded-lg overflow-hidden border border-border bg-bg-card">
      {filename && (
        <div className="px-4 py-2 bg-bg-surface border-b border-border text-xs font-mono text-text-muted">
          {filename}
        </div>
      )}
      <button
        onClick={handleCopy}
        className="absolute top-3 right-3 p-2 rounded bg-bg-surface/80 hover:bg-bg-surface text-text-secondary hover:text-white transition-colors z-10"
      >
        {copied ? <Check size={16} /> : <Copy size={16} />}
      </button>
      <SyntaxHighlighter language={language} style={atomDark} customStyle={{ margin: 0, background: 'transparent' }}>
        {code}
      </SyntaxHighlighter>
    </div>
  );
}

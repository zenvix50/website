import { Helmet } from 'react-helmet-async';
import { motion } from 'framer-motion';
import PageWrapper from '../components/layout/PageWrapper';
import OSDetector from '../components/download/OSDetector';
import DownloadCard from '../components/download/DownloadCard';
import VersionBadge from '../components/download/VersionBadge';
import { useLatestRelease } from '../hooks/useLatestRelease';
import { useOSDetect } from '../hooks/useOSDetect';
import CodeBlock from '../components/shared/CodeBlock';

const fallbackAssets = [
  { os: 'windows', label: 'Windows Installer (.exe)', format: '.exe', arch: 'x64', size: 0, checksum: '', url: '#' },
  { os: 'macos', label: 'macOS (.dmg) — Universal', format: '.dmg', arch: 'arm64 + x64', size: 0, checksum: '', url: '#' },
  { os: 'linux-deb', label: 'Debian/Ubuntu (.deb)', format: '.deb', arch: 'x64', size: 0, checksum: '', url: '#' },
  { os: 'linux-rpm', label: 'Red Hat/Fedora (.rpm)', format: '.rpm', arch: 'x64', size: 0, checksum: '', url: '#' },
  { os: 'linux-appimage', label: 'AppImage (portable)', format: '.AppImage', arch: 'x64', size: 0, checksum: '', url: '#' },
];

export default function Download() {
  const { release, loading } = useLatestRelease();
  const os = useOSDetect();
  const assets = release?.assets?.length ? release.assets : fallbackAssets;
  const version = release?.version || '1.0.0';

  return (
    <PageWrapper>
      <Helmet>
        <title>zenviX — Dev Environment Manager | Download</title>
        <meta name="description" content="Download zenviX for your operating system. Available for Windows, macOS, Debian, RPM, and AppImage." />
      </Helmet>

      <div className="max-w-6xl mx-auto px-4 py-20">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="text-center mb-12">
          <h1 className="text-5xl font-display font-bold text-white mb-4">Download zenviX</h1>
          <div className="flex items-center justify-center gap-4 flex-wrap">
            <VersionBadge version={`v${version}`} isLatest />
            {release?.releasedAt && (
              <span className="text-text-muted text-sm">
                Released {new Date(release.releasedAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
              </span>
            )}
            {release?.downloadCount > 0 && (
              <span className="text-text-muted text-sm">{release.downloadCount.toLocaleString()} downloads</span>
            )}
          </div>
          <div className="mt-4">
            <OSDetector />
          </div>
        </motion.div>

        {loading ? (
          <div className="text-center text-text-muted py-20">Loading release info...</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
            {assets.map((asset, i) => (
              <DownloadCard
                key={i}
                asset={asset}
                version={version}
                isRecommended={
                  (os === 'windows' && asset.os === 'windows') ||
                  (os === 'macos' && asset.os === 'macos') ||
                  (os === 'linux' && asset.os === 'linux-deb')
                }
              />
            ))}
          </div>
        )}

        {/* Other Installation Methods */}
        <div className="mb-12">
          <h2 className="text-2xl font-display font-bold text-white mb-6">Other Installation Methods</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { title: 'Manual (JAR)', desc: 'Download the ZIP archive and run:', code: 'java -jar zenvix.jar' },
              { title: 'GitHub Releases', desc: 'All releases available on GitHub:', code: 'github.com/yourusername/zenvix/releases' },
              { title: 'Build from Source', desc: 'Clone and build with Maven:', code: 'mvn clean package -DskipTests' },
            ].map(({ title, desc, code }, i) => (
              <div key={i} className="p-5 bg-bg-card border border-border rounded-lg">
                <h3 className="text-white font-semibold mb-2">{title}</h3>
                <p className="text-text-secondary text-sm mb-3">{desc}</p>
                <CodeBlock code={code} language="bash" />
              </div>
            ))}
          </div>
        </div>

        {/* System Requirements */}
        <div className="mb-12 p-6 bg-bg-card border border-border rounded-lg">
          <h2 className="text-2xl font-display font-bold text-white mb-6">System Requirements</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h3 className="text-accent font-semibold mb-3">Minimum</h3>
              <ul className="space-y-2 text-text-secondary text-sm">
                <li>Java 21+</li>
                <li>Windows 10 / macOS 12 / Ubuntu 20.04</li>
                <li>4GB RAM</li>
                <li>500MB disk space</li>
              </ul>
            </div>
            <div>
              <h3 className="text-accent font-semibold mb-3">Recommended</h3>
              <ul className="space-y-2 text-text-secondary text-sm">
                <li>Java 21 LTS</li>
                <li>Windows 11 / macOS 14 / Ubuntu 22.04</li>
                <li>16GB RAM</li>
                <li>SSD storage</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Verify Download */}
        <div className="p-6 bg-bg-card border border-border rounded-lg">
          <h2 className="text-2xl font-display font-bold text-white mb-6">Verify Your Download</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <h3 className="text-white font-semibold mb-2">Windows</h3>
              <CodeBlock code={`Get-FileHash ZenviX-Setup.exe -Algorithm SHA256`} language="bash" />
            </div>
            <div>
              <h3 className="text-white font-semibold mb-2">macOS / Linux</h3>
              <CodeBlock code={`shasum -a 256 ZenviX.dmg`} language="bash" />
            </div>
            <div>
              <h3 className="text-white font-semibold mb-2">Linux (sha256sum)</h3>
              <CodeBlock code={`sha256sum zenvix_1.0.0_amd64.deb`} language="bash" />
            </div>
          </div>
        </div>
      </div>
    </PageWrapper>
  );
}

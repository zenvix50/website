import Release from '../models/Release.js';
import axios from 'axios';

export const getLatestRelease = async (req, res) => {
  try {
    const release = await Release.findOne({ isLatest: true });
    if (!release) return res.status(404).json({ message: 'No release found' });
    res.json(release);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
};

export const getAllReleases = async (req, res) => {
  try {
    const releases = await Release.find().sort({ releasedAt: -1 });
    res.json(releases);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
};

export const syncFromGitHub = async (req, res) => {
  try {
    const { data } = await axios.get(
      `https://api.github.com/repos/${process.env.GITHUB_OWNER}/${process.env.GITHUB_REPO}/releases`,
      { headers: { Authorization: `token ${process.env.GITHUB_TOKEN}` } }
    );

    await Release.updateMany({}, { isLatest: false });

    for (const gh of data) {
      const assets = gh.assets.map(asset => ({
        label: asset.label || asset.name,
        url: asset.browser_download_url,
        size: asset.size,
        format: '.' + asset.name.split('.').pop(),
        os: detectOS(asset.name),
        arch: detectArch(asset.name),
        checksum: '',
      }));

      await Release.findOneAndUpdate(
        { tagName: gh.tag_name },
        {
          version: gh.tag_name.replace('v', ''),
          tagName: gh.tag_name,
          releasedAt: new Date(gh.published_at),
          changelog: gh.body || '',
          assets,
          githubUrl: gh.html_url,
          isLatest: gh === data[0],
        },
        { upsert: true, new: true }
      );
    }

    res.json({ message: `Synced ${data.length} releases` });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
};

function detectOS(filename) {
  if (filename.endsWith('.exe') || filename.includes('windows')) return 'windows';
  if (filename.endsWith('.dmg') || filename.includes('mac')) return 'macos';
  if (filename.endsWith('.deb')) return 'linux-deb';
  if (filename.endsWith('.rpm')) return 'linux-rpm';
  if (filename.endsWith('.AppImage')) return 'linux-appimage';
  return 'unknown';
}

function detectArch(filename) {
  if (filename.includes('arm64') || filename.includes('aarch64')) return 'arm64';
  if (filename.includes('universal')) return 'universal';
  return 'x64';
}

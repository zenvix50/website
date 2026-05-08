import { useEffect, useState } from 'react';
import axios from 'axios';
import { useReleaseStore } from '../store/releaseStore';

export function useLatestRelease() {
  const { latestRelease, setLatestRelease } = useReleaseStore();
  const [loading, setLoading] = useState(!latestRelease);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (latestRelease) {
      setLoading(false);
      return;
    }
    axios.get('/api/releases/latest')
      .then(({ data }) => { setLatestRelease(data); setLoading(false); })
      .catch((err) => { setError(err.message); setLoading(false); });
  }, [latestRelease]);

  return { release: latestRelease, loading, error };
}

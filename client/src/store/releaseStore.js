import { create } from 'zustand';

export const useReleaseStore = create((set) => ({
  latestRelease: null,
  allReleases: [],
  setLatestRelease: (release) => set({ latestRelease: release }),
  setAllReleases: (releases) => set({ allReleases: releases }),
}));

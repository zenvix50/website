import { detectOS } from '../utils/detectOS.js';

export function useOSDetect() {
  return detectOS();
}

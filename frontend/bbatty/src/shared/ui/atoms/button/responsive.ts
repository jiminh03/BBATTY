// shared/ui/responsive.ts
import { useWindowDimensions, PixelRatio } from 'react-native';

export function clamp(n: number, min: number, max: number) {
  'worklet'; // (RN Skia/gesture 쓸 때도 안전)
  return Math.max(min, Math.min(max, n));
}

/** 기준 360dp → 기기 너비 기준 스케일. (0.85 ~ 1.25로 클램프) */
export function useScale() {
  const { width } = useWindowDimensions();
  return clamp(width / 360, 0.85, 1.25);
}

/** rem 단위처럼 쓰기 (기본 16 * scale) */
export function useRem() {
  const scale = useScale();
  return (n: number) => n * 16 * scale;
}

/** 접근성 폰트스케일 반영한 dp → px 변환 (아이콘/보더엔 굳이 필요X) */
export function sp(dp: number) {
  return PixelRatio.getFontScale() * dp;
}

/** 최소 터치 타겟(머티리얼 가이드 48dp) 스케일 적용, 44~56 범위 */
export function useMinTouch() {
  const scale = useScale();
  return clamp(48 * scale, 44, 56);
}

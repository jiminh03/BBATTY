import { useEffect, useRef } from 'react';
import { AppState, AppStateStatus } from 'react-native';

export const useAppState = (
  onChange?: (status: AppStateStatus) => void,
  onForeground?: () => void,
  onBackground?: () => void
) => {
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (appState.current.match(/inactive|background/) && nextAppState === 'active') {
        onForeground?.();
      }

      if (appState.current === 'active' && nextAppState.match(/inactive|background/)) {
        onBackground?.();
      }
      appState.current = nextAppState;
      onChange?.(nextAppState);
    });

    return () => subscription.remove();
  }, [onChange, onForeground, onBackground]);
};

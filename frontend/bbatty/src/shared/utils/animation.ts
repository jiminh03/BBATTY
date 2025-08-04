import { Animated, Easing, LayoutAnimation, UIManager, Platform } from 'react-native';
import { useRef, useEffect, useCallback } from 'react';

// 안드로이드 LayoutAnimation 활성화
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

// 애니메이션 타이밍 상수
export const ANIMATION_DURATION = {
  FAST: 200,
  NORMAL: 300,
  SLOW: 500,
  VERY_SLOW: 800,
} as const;

// Easing 프리셋
export const EASING_PRESETS = {
  smooth: Easing.bezier(0.25, 0.1, 0.25, 1),
  smoothOut: Easing.bezier(0, 0, 0.2, 1),
  smoothIn: Easing.bezier(0.42, 0, 1, 1),
  sharp: Easing.bezier(0.4, 0, 0.6, 1),
  bounce: Easing.bounce,
  elastic: Easing.elastic(1),
} as const;

// 애니메이션 값 훅
export const useAnimatedValue = (initialValue: number = 0) => {
  const animatedValue = useRef(new Animated.Value(initialValue)).current;
  return animatedValue;
};

// 애니메이션 스타일 훅
export const useAnimatedStyle = <T extends Record<string, any>>(
  styleFactory: (animatedValue: Animated.Value) => T,
  initialValue: number = 0
): [T, Animated.Value] => {
  const animatedValue = useAnimatedValue(initialValue);
  const style = styleFactory(animatedValue);
  return [style, animatedValue];
};

// 페이드 애니메이션
export const fadeAnimation = {
  fadeIn: (
    animatedValue: Animated.Value,
    options?: {
      duration?: number;
      delay?: number;
      useNativeDriver?: boolean;
    }
  ) => {
    const { duration = ANIMATION_DURATION.NORMAL, delay = 0, useNativeDriver = true } = options || {};

    return Animated.timing(animatedValue, {
      toValue: 1,
      duration,
      delay,
      easing: EASING_PRESETS.smoothOut,
      useNativeDriver,
    });
  },

  fadeOut: (
    animatedValue: Animated.Value,
    options?: {
      duration?: number;
      delay?: number;
      useNativeDriver?: boolean;
    }
  ) => {
    const { duration = ANIMATION_DURATION.NORMAL, delay = 0, useNativeDriver = true } = options || {};

    return Animated.timing(animatedValue, {
      toValue: 0,
      duration,
      delay,
      easing: EASING_PRESETS.smoothIn,
      useNativeDriver,
    });
  },
};

// 스케일 애니메이션
export const scaleAnimation = {
  scaleIn: (
    animatedValue: Animated.Value,
    options?: {
      duration?: number;
      fromValue?: number;
      toValue?: number;
      easing?: (value: number) => number;
    }
  ) => {
    const {
      duration = ANIMATION_DURATION.NORMAL,
      fromValue = 0,
      toValue = 1,
      easing = EASING_PRESETS.bounce,
    } = options || {};

    animatedValue.setValue(fromValue);

    return Animated.timing(animatedValue, {
      toValue,
      duration,
      easing,
      useNativeDriver: true,
    });
  },

  pulse: (
    animatedValue: Animated.Value,
    options?: {
      minScale?: number;
      maxScale?: number;
      duration?: number;
    }
  ) => {
    const { minScale = 0.95, maxScale = 1.05, duration = ANIMATION_DURATION.SLOW } = options || {};

    return Animated.loop(
      Animated.sequence([
        Animated.timing(animatedValue, {
          toValue: maxScale,
          duration: duration / 2,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(animatedValue, {
          toValue: minScale,
          duration: duration / 2,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    );
  },
};

// 슬라이드 애니메이션
export const slideAnimation = {
  slideInFromRight: (
    animatedValue: Animated.Value,
    screenWidth: number,
    options?: {
      duration?: number;
      delay?: number;
    }
  ) => {
    const { duration = ANIMATION_DURATION.NORMAL, delay = 0 } = options || {};

    animatedValue.setValue(screenWidth);

    return Animated.timing(animatedValue, {
      toValue: 0,
      duration,
      delay,
      easing: EASING_PRESETS.smoothOut,
      useNativeDriver: true,
    });
  },

  slideInFromBottom: (
    animatedValue: Animated.Value,
    screenHeight: number,
    options?: {
      duration?: number;
      delay?: number;
    }
  ) => {
    const { duration = ANIMATION_DURATION.NORMAL, delay = 0 } = options || {};

    animatedValue.setValue(screenHeight);

    return Animated.timing(animatedValue, {
      toValue: 0,
      duration,
      delay,
      easing: EASING_PRESETS.smoothOut,
      useNativeDriver: true,
    });
  },
};

// 회전 애니메이션
export const rotateAnimation = {
  rotate360: (
    animatedValue: Animated.Value,
    options?: {
      duration?: number;
      loop?: boolean;
    }
  ) => {
    const { duration = ANIMATION_DURATION.SLOW, loop = false } = options || {};

    const animation = Animated.timing(animatedValue, {
      toValue: 1,
      duration,
      easing: Easing.linear,
      useNativeDriver: true,
    });

    return loop ? Animated.loop(animation) : animation;
  },

  swing: (
    animatedValue: Animated.Value,
    options?: {
      duration?: number;
      angle?: number;
    }
  ) => {
    const { duration = ANIMATION_DURATION.NORMAL, angle = 15 } = options || {};

    return Animated.sequence([
      Animated.timing(animatedValue, {
        toValue: angle,
        duration: duration / 4,
        easing: Easing.out(Easing.ease),
        useNativeDriver: true,
      }),
      Animated.timing(animatedValue, {
        toValue: -angle,
        duration: duration / 2,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: true,
      }),
      Animated.timing(animatedValue, {
        toValue: 0,
        duration: duration / 4,
        easing: Easing.out(Easing.ease),
        useNativeDriver: true,
      }),
    ]);
  },
};

// 병렬 애니메이션
export const createParallelAnimation = (
  animations: Animated.CompositeAnimation[],
  options?: {
    stopTogether?: boolean;
  }
) => {
  return Animated.parallel(animations, options);
};

// 순차 애니메이션
export const createSequenceAnimation = (animations: Animated.CompositeAnimation[]) => {
  return Animated.sequence(animations);
};

// 스태거 애니메이션 (순차적 딜레이)
export const createStaggerAnimation = (animations: Animated.CompositeAnimation[], delay: number = 100) => {
  return Animated.stagger(delay, animations);
};

// 스프링 애니메이션
export const springAnimation = (
  animatedValue: Animated.Value,
  toValue: number,
  options?: {
    friction?: number;
    tension?: number;
    speed?: number;
    bounciness?: number;
  }
) => {
  const { friction = 7, tension = 40, speed = 12, bounciness = 8 } = options || {};

  return Animated.spring(animatedValue, {
    toValue,
    friction,
    tension,
    speed,
    bounciness,
    useNativeDriver: true,
  });
};

// Layout Animation 프리셋
export const layoutAnimationPresets = {
  easeInEaseOut: LayoutAnimation.Presets.easeInEaseOut,
  linear: LayoutAnimation.Presets.linear,
  spring: LayoutAnimation.Presets.spring,

  // 커스텀 프리셋
  smooth: {
    duration: ANIMATION_DURATION.NORMAL,
    create: {
      type: LayoutAnimation.Types.easeInEaseOut,
      property: LayoutAnimation.Properties.opacity,
    },
    update: {
      type: LayoutAnimation.Types.easeInEaseOut,
    },
    delete: {
      type: LayoutAnimation.Types.easeInEaseOut,
      property: LayoutAnimation.Properties.opacity,
    },
  },
};

// Layout Animation 헬퍼
export const configureNextLayoutAnimation = (
  preset: keyof typeof layoutAnimationPresets = 'smooth',
  onAnimationDidEnd?: () => void
) => {
  LayoutAnimation.configureNext(layoutAnimationPresets[preset], onAnimationDidEnd);
};

// 애니메이션 인터폴레이션 헬퍼
export const createInterpolation = (
  animatedValue: Animated.Value,
  config: {
    inputRange: number[];
    outputRange: number[] | string[];
    extrapolate?: 'extend' | 'identity' | 'clamp';
  }
) => {
  return animatedValue.interpolate(config);
};

// 진동 애니메이션
export const shakeAnimation = (
  animatedValue: Animated.Value,
  options?: {
    duration?: number;
    intensity?: number;
  }
) => {
  const { duration = 500, intensity = 10 } = options || {};
  const numberOfShakes = 4;
  const shakeDuration = duration / numberOfShakes;

  const sequence: Animated.CompositeAnimation[] = [];

  for (let i = 0; i < numberOfShakes; i++) {
    sequence.push(
      Animated.timing(animatedValue, {
        toValue: i % 2 === 0 ? intensity : -intensity,
        duration: shakeDuration / 2,
        useNativeDriver: true,
      })
    );
  }

  sequence.push(
    Animated.timing(animatedValue, {
      toValue: 0,
      duration: shakeDuration / 2,
      useNativeDriver: true,
    })
  );

  return Animated.sequence(sequence);
};

// 애니메이션 타이밍 훅
export const useTimingAnimation = (
  toValue: number,
  options?: {
    duration?: number;
    delay?: number;
    easing?: (value: number) => number;
    initialValue?: number;
    autoStart?: boolean;
  }
) => {
  const {
    duration = ANIMATION_DURATION.NORMAL,
    delay = 0,
    easing = Easing.inOut(Easing.ease),
    initialValue = 0,
    autoStart = true,
  } = options || {};

  const animatedValue = useAnimatedValue(initialValue);

  const startAnimation = useCallback(() => {
    Animated.timing(animatedValue, {
      toValue,
      duration,
      delay,
      easing,
      useNativeDriver: true,
    }).start();
  }, [animatedValue, toValue, duration, delay, easing]);

  useEffect(() => {
    if (autoStart) {
      startAnimation();
    }
  }, [autoStart, startAnimation]);

  return [animatedValue, startAnimation] as const;
};

// 제스처 애니메이션 헬퍼
export const createPanResponderAnimation = (animatedValueX: Animated.Value, animatedValueY: Animated.Value) => {
  return {
    onPanResponderMove: Animated.event(
      [
        null,
        {
          dx: animatedValueX,
          dy: animatedValueY,
        },
      ],
      { useNativeDriver: false }
    ),
    onPanResponderRelease: () => {
      Animated.parallel([
        Animated.spring(animatedValueX, {
          toValue: 0,
          useNativeDriver: true,
        }),
        Animated.spring(animatedValueY, {
          toValue: 0,
          useNativeDriver: true,
        }),
      ]).start();
    },
  };
};

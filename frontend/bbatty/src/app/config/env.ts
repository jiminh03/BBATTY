import Constants from 'expo-constants';

interface EnvConfig {
  KAKAO_NATIVE_APP_KEY: string;
  // 추가 환경 변수들
}

/*
 * 환경 변수 설정
 * expo-constants를 통해 app.json/app.config.js의 extra 필드에서 값을 가져옴
 */
const ENV: EnvConfig = {
  KAKAO_NATIVE_APP_KEY: Constants.expoConfig?.extra?.kakaoNativeAppKey || '',
};

// 필수 환경 변수 검증
const validateEnv = () => {
  const requiredKeys: (keyof EnvConfig)[] = ['KAKAO_NATIVE_APP_KEY'];

  for (const key of requiredKeys) {
    if (!ENV[key]) {
      console.error(`환경 변수 ${key}가 설정되지 않았습니다.`);
      // 개발 환경에서는 경고만, 프로덕션에서는 에러 throw
      if (__DEV__) {
        console.warn(`개발 환경: ${key} 기본값 사용`);
      } else {
        throw new Error(`필수 환경 변수 ${key}가 누락되었습니다.`);
      }
    }
  }
};

validateEnv();

export default ENV;

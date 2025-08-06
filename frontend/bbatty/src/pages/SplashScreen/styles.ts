import { StyleSheet, Dimensions } from 'react-native';

const { width, height } = Dimensions.get('window');

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    position: 'relative',
  },

  // 배경 패턴
  backgroundPattern: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    overflow: 'hidden',
  },

  stitchLine: {
    position: 'absolute',
    left: -width,
    width: width * 2,
    height: 3,
    backgroundColor: '#FFB3B3',
    opacity: 0.3,
  },

  // 메인 콘텐츠
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },

  titleContainer: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },

  titleText: {
    fontSize: 72,
    fontWeight: '900',
    color: '#000000',
    letterSpacing: 8,
  },

  letterContainer: {
    position: 'relative',
    marginLeft: 20,
  },

  tiText: {
    position: 'absolute',
  },

  tingText: {
    position: 'relative',
  },

  underline: {
    position: 'absolute',
    bottom: -10,
    left: 0,
    right: 0,
    height: 4,
    backgroundColor: '#000000',
  },

  subtitle: {
    marginTop: 20,
    fontSize: 16,
    color: '#666666',
    letterSpacing: 2,
  },

  // 야구공
  baseball: {
    position: 'absolute',
    top: height / 2 - 60,
    left: -100,
    width: 60,
    height: 60,
    backgroundColor: '#FFFFFF',
    borderRadius: 30,
    borderWidth: 2,
    borderColor: '#E0E0E0',
    justifyContent: 'center',
    alignItems: 'center',

    // 그림자 효과
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },

  baseballStitch: {
    position: 'absolute',
    width: 40,
    height: 2,
    backgroundColor: '#FF4444',
    transform: [{ rotate: '45deg' }],
  },

  baseballStitchVertical: {
    transform: [{ rotate: '-45deg' }],
  },

  // 로그인 버튼
  loginButtonContainer: {
    position: 'absolute',
    bottom: 60,
    left: 20,
    right: 20,
  },

  loginButton: {
    flexDirection: 'row',
    backgroundColor: '#FEE500',
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',

    // 그림자 효과
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },

  kakaoIcon: {
    fontSize: 20,
    marginRight: 8,
  },

  loginButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#000000',
  },
});

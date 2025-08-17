import { StyleSheet, Dimensions } from 'react-native';

const { width, height } = Dimensions.get('window');

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    position: 'relative',
  },

  // 야구공
  baseball: {
    position: 'absolute',
    top: height / 2 + 50,
    left: -100,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
  },

  baseballImage: {
    width: 60,
    height: 60,
    borderRadius: 30,
    borderWidth: 1,
    borderColor: '#474747ff',
    // 그림자 효과
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 1,
    shadowRadius: 1,
    elevation: 1,
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

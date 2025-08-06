// pages/teamSelect/ui/TeamGrid.styles.ts

import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  teamGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 16,
    justifyContent: 'space-between',
  },
  teamCard: {
    width: '31%',
    aspectRatio: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    marginBottom: 16,
    borderWidth: 2,
    borderColor: '#F0F0F0',
    padding: 8,
    alignItems: 'center',
    justifyContent: 'center',
    // 안드로이드 그림자
    elevation: 2,
    // iOS 그림자
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  teamCardSelected: {
    // borderColor: '#FF0000', // 동적으로 팀 컬러로 변경됨
    borderWidth: 2.5,
    backgroundColor: '#FAFAFA',
  },
  teamCardContent: {
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
  },
  teamLogoContainer: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#F8F8F8',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  teamLogoContainerSelected: {
    // backgroundColor: '#FFE8E8', // 동적으로 팀 컬러 연한 버전으로 변경
  },
  teamLogo: {
    width: 40,
    height: 40,
    resizeMode: 'contain',
  },
  teamLogoEmoji: {
    fontSize: 28,
  },
  teamName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333333',
    textAlign: 'center',
    marginTop: 4,
  },
  teamNameSelected: {
    fontWeight: 'bold',
    // color: '#FF0000', // 동적으로 팀 컬러로 변경됨
  },
  teamStadium: {
    fontSize: 10,
    color: '#999999',
    textAlign: 'center',
    marginTop: 2,
  },
});

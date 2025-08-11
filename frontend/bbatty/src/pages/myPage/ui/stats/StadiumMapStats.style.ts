import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    position: 'relative',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 12,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 2,
  },
  description: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 20,
  },
  mapContainer: {
    height: 300,
    marginBottom: 16,
  },
  koreaMap: {
    flex: 1,
    position: 'relative',
    backgroundColor: '#E3F2FD',
    borderRadius: 12,
    overflow: 'hidden',
  },
  mapBackground: {
    position: 'absolute',
    top: '10%',
    left: '25%',
    width: '50%',
    height: '80%',
    backgroundColor: '#BBDEFB',
    borderRadius: 20,
    // 한국 모양을 흉내낸 배경
  },
  stadiumMarker: {
    position: 'absolute',
    alignItems: 'center',
    transform: [{ translateX: -12 }, { translateY: -12 }],
  },
  stadiumInfo: {
    position: 'absolute',
    top: 25,
    left: -40,
    width: 80,
    backgroundColor: 'rgba(0,0,0,0.8)',
    borderRadius: 6,
    padding: 4,
    alignItems: 'center',
  },
  stadiumLabel: {
    fontSize: 8,
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
  },
  stadiumWinRate: {
    fontSize: 7,
    color: 'white',
    textAlign: 'center',
  },
});
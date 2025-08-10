import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 12,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 16,
  },
  mapContainer: {
    height: 150,
    marginBottom: 16,
  },
  mapPlaceholder: {
    flex: 1,
    backgroundColor: '#F8F9FA',
    borderRadius: 8,
    position: 'relative',
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  stadiumMarker: {
    position: 'absolute',
    width: 30,
    height: 30,
    borderRadius: 15,
    justifyContent: 'center',
    alignItems: 'center',
  },
  markerText: {
    fontSize: 8,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  stadiumList: {
    gap: 8,
  },
  stadiumItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  stadiumInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  stadiumIndicator: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 12,
  },
  stadiumName: {
    fontSize: 14,
    color: '#333333',
  },
  stadiumWinRate: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333333',
  },
});

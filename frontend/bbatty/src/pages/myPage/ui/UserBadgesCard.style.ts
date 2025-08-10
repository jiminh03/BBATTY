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
  badgeGrid: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  badgeItem: {
    alignItems: 'center',
    flex: 1,
  },
  badgeIcon: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
  },
  badgeIconText: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: 'bold',
  },
  badgeLabel: {
    fontSize: 14,
    color: '#666666',
    marginBottom: 4,
  },
  badgeValue: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
  },
});

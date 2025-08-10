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
  chartContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 160,
    paddingHorizontal: 10,
  },
  barColumn: {
    alignItems: 'center',
    marginHorizontal: 8,
    minWidth: 50,
  },
  barContainer: {
    height: 120,
    justifyContent: 'flex-end',
    marginBottom: 8,
  },
  bar: {
    width: 30,
    borderRadius: 4,
  },
  teamName: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 2,
    textAlign: 'center',
  },
  winRate: {
    fontSize: 11,
    fontWeight: '600',
    color: '#333333',
  },
});

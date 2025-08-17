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
  chartContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 180,
    paddingHorizontal: 10,
  },
  barColumn: {
    alignItems: 'center',
    marginHorizontal: 6,
    minWidth: 50,
  },
  winRateLabel: {
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  barContainer: {
    height: 120,
    justifyContent: 'flex-end',
    marginBottom: 8,
    width: 32,
  },
  bar: {
    width: '100%',
    borderRadius: 6,
    minHeight: 8,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  perfectBar: {
    borderWidth: 2,
    borderColor: '#FFD700',
  },
  teamName: {
    fontSize: 12,
    color: '#333333',
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 4,
  },
  recordText: {
    fontSize: 10,
    color: '#666666',
    textAlign: 'center',
  },
});
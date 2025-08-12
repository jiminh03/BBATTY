import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    backgroundColor: 'transparent',
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#F0F0F0',
    marginBottom: 20,
  },
  statsRow: {
    flexDirection: 'row',
    marginBottom: 6,
    flexWrap: 'wrap',
  },
  statItem: {
    fontSize: 13,
    color: '#333333',
    fontWeight: '500',
    marginRight: 10,
  },
  recordRow: {
    marginBottom: 8,
  },
  recordText: {
    fontSize: 16,
    color: '#000000',
    fontWeight: '700',
  },
});
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
  mainWinRate: {
    alignItems: 'center',
    marginBottom: 20,
  },
  overallRate: {
    fontSize: 48,
    fontWeight: 'bold',
  },
  overallLabel: {
    fontSize: 16,
    color: '#666666',
    marginTop: 4,
  },
  detailRates: {
    flexDirection: 'row',
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
  },
  rateItem: {
    flex: 1,
    alignItems: 'center',
  },
  rateNumber: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  rateLabel: {
    fontSize: 14,
    color: '#666666',
    marginTop: 4,
  },
  rateDivider: {
    width: 1,
    backgroundColor: '#E0E0E0',
    marginHorizontal: 20,
  },
});

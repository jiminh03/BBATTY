import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 12,
    flex: 1,
  },

  // 구장 카드
  stadiumCard: {
    backgroundColor: '#FAFAFA',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  stadiumInfo: {
    flex: 1,
  },
  stadiumName: {
    fontSize: 16,
    fontWeight: '700',
    color: '#333333',
  },
  winRateContainer: {
    alignItems: 'flex-end',
  },
  winRateText: {
    fontSize: 20,
    fontWeight: '800',
  },

  // 승률 바
  progressBarContainer: {
    marginBottom: 16,
  },
  progressBarBackground: {
    height: 8,
    backgroundColor: '#E0E0E0',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 4,
    minWidth: 2,
  },

  // 기록 상세
  recordContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  recordItem: {
    alignItems: 'center',
    flex: 1,
  },
  recordLabel: {
    fontSize: 11,
    color: '#999999',
    marginBottom: 2,
    fontWeight: '500',
  },
  recordValue: {
    fontSize: 13,
    fontWeight: '700',
    color: '#333333',
  },
});
import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 12,
  },
  calendarGrid: {
    marginTop: 16,
    paddingBottom: 0,
  },
  calendarRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  lastRow: {
    marginBottom: 0,
  },
  calendarCell: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    marginHorizontal: 6,
    minHeight: 120,
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
    overflow: 'hidden',
  },
  calendarHeader: {
    paddingVertical: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  calendarBody: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    paddingHorizontal: 8,
  },
  dayLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  winRate: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 6,
  },
  matches: {
    fontSize: 12,
    color: '#666666',
    textAlign: 'center',
    marginBottom: 4,
  },
  recordText: {
    fontSize: 10,
    color: '#888888',
    textAlign: 'center',
  },
});
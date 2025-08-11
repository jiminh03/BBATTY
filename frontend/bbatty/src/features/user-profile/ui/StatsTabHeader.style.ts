import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  seasonContainer: {
    marginRight: 20,
  },
  tabsContainer: {
    flexDirection: 'row',
    backgroundColor: '#F8F9FA',
    borderRadius: 8,
    padding: 2,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  tab: {
    paddingVertical: 10,
    paddingHorizontal: 24,
    alignItems: 'flex-end',
    borderRadius: 6,
    minWidth: 80,
  },
  tabText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#666666',
  },
});

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
    padding: 3,
    borderWidth: 1,
    borderColor: '#E0E0E0',
    gap: 2,
  },
  tab: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 6,
    width: 60,
    minHeight: 36,
  },
  tabText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#666666',
  },
});

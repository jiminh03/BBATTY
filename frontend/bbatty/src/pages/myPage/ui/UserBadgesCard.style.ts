import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 16,
    paddingBottom: 20,
  },
  categoryCard: {
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
  },
  badgeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  badgeItem: {
    width: '30%',
    alignItems: 'center',
    marginBottom: 20,
  },
  badgeIcon: {
    width: 50,
    height: 50,
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  emptyBadge: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#CCCCCC',
  },
  badgeDescription: {
    fontSize: 10,
    color: '#666666',
    textAlign: 'center',
    fontWeight: '500',
  },
  otherCategories: {
    flexDirection: 'column',
    gap: 16,
    marginBottom: 20,
  },
  categorySection: {
    borderRadius: 12,
    padding: 12,
    paddingBottom: 16,
  },
  categoryTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 12,
  },
  categoryBadges: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  smallBadgeItem: {
    width: '31%', // 3 badges per row (31% x 3 = 93% with gaps)
    alignItems: 'center',
    marginBottom: 12,
  },
  winsBadgeItem: {
    width: '31%', // 승리 뱃지용 (2줄 x 3개 = 6개) - 약간 더 넓게
    alignItems: 'center',
    marginBottom: 16,
  },
  smallBadgeIcon: {
    width: 32,
    height: 32,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 4,
  },
  smallEmptyBadge: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#CCCCCC',
  },
  smallBadgeDescription: {
    fontSize: 9,
    color: '#666666',
    textAlign: 'center',
    fontWeight: '500',
  },
});
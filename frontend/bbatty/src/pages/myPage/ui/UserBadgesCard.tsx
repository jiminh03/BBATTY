import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BadgeCategory } from '../../../features/user-profile/model/badgeTypes';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './UserBadgesCard.style';

interface UserBadgesCardProps {
  badgeCategories: BadgeCategory[];
}

export const UserBadgesCard: React.FC<UserBadgesCardProps> = ({ badgeCategories }) => {
  const themeColor = useThemeColor();

  const getAcquiredCount = (category: BadgeCategory) => {
    return category.badges.filter((badge) => badge.acquired).length;
  };

  const getTotalCount = (category: BadgeCategory) => {
    return category.badges.length;
  };

  const getCategoryColor = (category: BadgeCategory) => {
    switch (category.category) {
      case 'STADIUM_CONQUEST':
        return '#FFB74D'; // 주황색 - 구장정복
      case 'SEASON_WINS': 
        return '#81C784'; // 초록색 - 승리
      case 'SEASON_GAMES':
        return '#64B5F6'; // 파란색 - 직관경기
      default:
        return themeColor;
    }
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <Text style={styles.title}>나의뱃지</Text>
      
      {/* 구장정복 카테고리 */}
      {badgeCategories.filter(c => c.category === 'STADIUM_CONQUEST').map(category => (
        <View key={category.category}>
          <View style={[styles.categoryCard, { backgroundColor: getCategoryColor(category) + '10' }]}>
            <View style={styles.badgeGrid}>
              {category.badges.map((badge, index) => (
                <View key={badge.badgeType} style={styles.badgeItem}>
                  <View style={[styles.badgeIcon, 
                    badge.acquired 
                      ? { backgroundColor: getCategoryColor(category) }
                      : { backgroundColor: '#E0E0E0' }
                  ]}>
                    {badge.acquired ? (
                      <Ionicons name="checkmark" size={16} color="white" />
                    ) : (
                      <View style={styles.emptyBadge} />
                    )}
                  </View>
                  <Text style={styles.badgeDescription} numberOfLines={2}>
                    {badge.description.replace('첫 방문', '')}
                  </Text>
                </View>
              ))}
            </View>
          </View>
        </View>
      ))}

      {/* 기타 카테고리들 */}
      <View style={styles.otherCategories}>
        {badgeCategories.filter(c => c.category !== 'STADIUM_CONQUEST').map(category => {
          const categoryColor = getCategoryColor(category);
          const isWinsCategory = category.category === 'SEASON_WINS';
          
          return (
            <View key={category.category} style={[styles.categorySection, { backgroundColor: categoryColor + '10' }]}>
              <Text style={[styles.categoryTitle, { color: categoryColor }]}>{category.displayName}</Text>
              <View style={styles.categoryBadges}>
                {category.badges.map((badge, index) => (
                  <View 
                    key={badge.badgeType} 
                    style={isWinsCategory ? styles.winsBadgeItem : styles.smallBadgeItem}
                  >
                    <View style={[styles.smallBadgeIcon, 
                      badge.acquired 
                        ? { backgroundColor: categoryColor }
                        : { backgroundColor: '#E0E0E0' }
                    ]}>
                      {badge.acquired ? (
                        <Ionicons name="checkmark" size={12} color="white" />
                      ) : (
                        <View style={styles.smallEmptyBadge} />
                      )}
                    </View>
                    <Text style={styles.smallBadgeDescription} numberOfLines={1}>
                      {badge.description}
                    </Text>
                  </View>
                ))}
                
                {/* 승리 카테고리에 추가 뱃지들 (목업에 맞춰서 6개로 만들기) */}
                {isWinsCategory && category.badges.length < 6 && 
                  Array.from({ length: 6 - category.badges.length }).map((_, index) => (
                    <View key={`extra-${index}`} style={styles.winsBadgeItem}>
                      <View style={[styles.smallBadgeIcon, { backgroundColor: '#E0E0E0' }]}>
                        <View style={styles.smallEmptyBadge} />
                      </View>
                      <Text style={styles.smallBadgeDescription} numberOfLines={1}>
                        시즌 {10 + index}승 달성
                      </Text>
                    </View>
                  ))
                }
              </View>
            </View>
          );
        })}
      </View>
    </ScrollView>
  );
};

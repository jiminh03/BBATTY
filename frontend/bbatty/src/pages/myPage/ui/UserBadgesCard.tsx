import React from 'react';
import { View, Text, ScrollView } from 'react-native';
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

  return (
    <View style={styles.container}>
      <Text style={styles.title}>나의 뱃지</Text>

      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.badgeGrid}>
          {badgeCategories.map((category, index) => (
            <View key={category.category} style={styles.badgeItem}>
              <View style={[styles.badgeIcon, { backgroundColor: themeColor }]}>
                <Text style={styles.badgeIconText}>{getAcquiredCount(category)}</Text>
              </View>
              <Text style={styles.badgeLabel}>{category.displayName}</Text>
              <Text style={styles.badgeValue}>
                {getAcquiredCount(category)}/{getTotalCount(category)}
              </Text>
              {category.season && <Text /*style={styles.badgeSeason}*/>{category.season}</Text>}
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
};

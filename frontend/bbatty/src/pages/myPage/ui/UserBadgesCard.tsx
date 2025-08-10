import React from 'react';
import { View, Text } from 'react-native';
import { UserBadges } from '../../../features/user-stats/model/statsTypes';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './UserBadgesCard.style';

interface UserBadgesCardProps {
  badges: UserBadges;
}

export const UserBadgesCard: React.FC<UserBadgesCardProps> = ({ badges }) => {
  const themeColor = useThemeColor();

  const badgeItems = [
    { label: '방문구장', value: badges.visitedStadiums, unit: '곳' },
    { label: '경기', value: badges.totalMatches, unit: '경기' },
    { label: '승리', value: badges.totalWins, unit: '승' },
  ];

  return (
    <View style={styles.container}>
      <Text style={styles.title}>나의 뱃지</Text>

      <View style={styles.badgeGrid}>
        {badgeItems.map((item, index) => (
          <View key={index} style={styles.badgeItem}>
            <View style={[styles.badgeIcon, { backgroundColor: themeColor }]}>
              <Text style={styles.badgeIconText}>✓</Text>
            </View>
            <Text style={styles.badgeLabel}>{item.label}</Text>
            <Text style={styles.badgeValue}>
              {item.value}
              {item.unit}
            </Text>
          </View>
        ))}
      </View>
    </View>
  );
};

import React from 'react';
import { View, TouchableOpacity, Text } from 'react-native';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { SeasonSelector } from './SeasonSelector';
import { Season } from '../../../shared/utils/date';
import { TabType } from './ProfileTabs';
import { styles } from './StatsTabHeader.style';

interface StatsTabHeaderProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
  selectedSeason: Season;
  onSeasonChange: (season: Season) => void;
}

const statsTabs: { key: TabType; label: string }[] = [
  { key: 'badges', label: '뱃지' },
  { key: 'winrate', label: '승률' },
];

export const StatsTabHeader: React.FC<StatsTabHeaderProps> = ({
  activeTab,
  onTabChange,
  selectedSeason,
  onSeasonChange,
}) => {
  const themeColor = useThemeColor();

  return (
    <View style={styles.container}>
      {/* Season Dropdown on the left */}
      <View style={styles.seasonContainer}>
        <SeasonSelector selectedSeason={selectedSeason} onSeasonChange={onSeasonChange} />
      </View>

      {/* Stats Tabs on the right */}
      <View style={styles.tabsContainer}>
        {statsTabs.map((tab) => (
          <TouchableOpacity
            key={tab.key}
            style={[
              styles.tab,
              activeTab === tab.key && { 
                backgroundColor: '#FFFFFF',
                shadowColor: '#000',
                shadowOffset: { width: 0, height: 1 },
                shadowOpacity: 0.1,
                shadowRadius: 2,
                elevation: 2,
              }
            ]}
            onPress={() => onTabChange(tab.key)}
          >
            <Text
              style={[
                styles.tabText,
                activeTab === tab.key && { color: themeColor, fontWeight: '600' }
              ]}
            >
              {tab.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
};
import React from 'react';
import { View, TouchableOpacity, Text, ScrollView } from 'react-native';
import { Season } from '../../user-stats/model/statsTypes';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './SeasonSelector.style';

interface SeasonSelectorProps {
  selectedSeason: Season;
  onSeasonChange: (season: Season) => void;
  seasons?: Season[];
}

const DEFAULT_SEASONS: Season[] = ['전체', '2024', '2023', '2022'];

export const SeasonSelector: React.FC<SeasonSelectorProps> = ({
  selectedSeason,
  onSeasonChange,
  seasons = DEFAULT_SEASONS,
}) => {
  const themeColor = useThemeColor();

  return (
    <View style={styles.container}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        {seasons.map((season) => (
          <TouchableOpacity
            key={season}
            style={[
              styles.seasonButton,
              selectedSeason === season && {
                backgroundColor: themeColor,
                borderColor: themeColor,
              },
            ]}
            onPress={() => onSeasonChange(season)}
            activeOpacity={0.7}
          >
            <Text style={[styles.seasonText, selectedSeason === season && styles.seasonTextActive]}>{season}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
};

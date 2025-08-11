import React from 'react';
import { View, TouchableOpacity, Text, ScrollView } from 'react-native';
import { Season, generateSeasons, formatSeasonDisplay } from '../../../shared/utils/date';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './SeasonSelector.style';

interface SeasonSelectorProps {
  selectedSeason: Season;
  onSeasonChange: (season: Season) => void;
}

export const SeasonSelector: React.FC<SeasonSelectorProps> = ({ selectedSeason, onSeasonChange }) => {
  const themeColor = useThemeColor();
  const seasons = generateSeasons(); // 동적 생성

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
            <Text style={[styles.seasonText, selectedSeason === season && styles.seasonTextActive]}>
              {formatSeasonDisplay(season)}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
};

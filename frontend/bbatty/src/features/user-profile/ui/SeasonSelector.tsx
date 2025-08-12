import React, { useState, useRef, useEffect } from 'react';
import { View, TouchableOpacity, Text, ScrollView, TouchableWithoutFeedback } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Season, generateSeasons, formatSeasonDisplay } from '../../../shared/utils/date';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './SeasonSelector.style';

interface SeasonSelectorProps {
  selectedSeason: Season;
  onSeasonChange: (season: Season) => void;
}

export const SeasonSelector: React.FC<SeasonSelectorProps> = ({ selectedSeason, onSeasonChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const themeColor = useThemeColor();
  const seasons = generateSeasons(); // 동적 생성
  const containerRef = useRef<View>(null);

  const handleSelect = (season: Season) => {
    onSeasonChange(season);
    setIsOpen(false);
  };

  const handleClickOutside = () => {
    setIsOpen(false);
  };

  return (
    <>
      {isOpen && (
        <TouchableWithoutFeedback onPress={handleClickOutside}>
          <View style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 9998 }} />
        </TouchableWithoutFeedback>
      )}
      <View ref={containerRef} style={styles.container}>
        <TouchableOpacity 
          style={[styles.dropdownButton, { borderColor: themeColor }]} 
          onPress={() => setIsOpen(!isOpen)}
        >
          <Text style={styles.dropdownText}>{formatSeasonDisplay(selectedSeason)}</Text>
          <Ionicons 
            name={isOpen ? "chevron-up" : "chevron-down"} 
            size={16} 
            color={themeColor} 
          />
        </TouchableOpacity>

        {isOpen && (
          <View style={styles.dropdownList}>
            <ScrollView showsVerticalScrollIndicator={false} style={styles.scrollView}>
              {seasons.map((season) => (
                <TouchableOpacity
                  key={season}
                  style={[
                    styles.option,
                    selectedSeason === season && { backgroundColor: `${themeColor}15` }
                  ]}
                  onPress={() => handleSelect(season)}
                >
                  <Text
                    style={[
                      styles.optionText,
                      selectedSeason === season && { color: themeColor, fontWeight: '600' }
                    ]}
                  >
                    {formatSeasonDisplay(season)}
                  </Text>
                  {selectedSeason === season && (
                    <Ionicons name="checkmark" size={16} color={themeColor} />
                  )}
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        )}
      </View>
    </>
  );
};

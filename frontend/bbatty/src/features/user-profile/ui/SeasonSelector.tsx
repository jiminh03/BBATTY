import React, { useState } from 'react';
import { View, TouchableOpacity, Text, ScrollView, Modal } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Season, generateSeasons, formatSeasonDisplay } from '../../../shared/utils/date';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useAttendanceYears } from '../hooks/useProfile';
import { styles } from './SeasonSelector.style';

interface SeasonSelectorProps {
  selectedSeason: Season;
  onSeasonChange: (season: Season) => void;
  userId?: number;
}

export const SeasonSelector: React.FC<SeasonSelectorProps> = ({ selectedSeason, onSeasonChange, userId }) => {
  const [isOpen, setIsOpen] = useState(false);
  const themeColor = useThemeColor();
  
  // API로부터 직관 년도 목록 가져오기
  const { data: attendanceYears, isLoading } = useAttendanceYears(userId);
  
  // API 데이터가 있으면 사용하고, 없으면 기본 시즌 생성
  const seasons: Season[] = attendanceYears && attendanceYears.length > 0 
    ? ['total', ...attendanceYears.map(year => year as Season)]
    : generateSeasons();

  const handleSelect = (season: Season) => {
    onSeasonChange(season);
    setIsOpen(false);
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity 
        style={[styles.dropdownButton, { borderColor: themeColor }]} 
        onPress={() => setIsOpen(true)}
      >
        <Text style={styles.dropdownText}>{formatSeasonDisplay(selectedSeason)}</Text>
        <Ionicons name="chevron-down" size={16} color={themeColor} />
      </TouchableOpacity>

      <Modal visible={isOpen} transparent animationType='fade' onRequestClose={() => setIsOpen(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setIsOpen(false)}>
          <View style={styles.modalContent}>
            <ScrollView showsVerticalScrollIndicator={false}>
              {seasons.map((season, index) => (
                <TouchableOpacity
                  key={season}
                  style={[
                    styles.option,
                    selectedSeason === season && { backgroundColor: `${themeColor}15` },
                    index === seasons.length - 1 && styles.lastOption
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
                    <Ionicons name="checkmark" size={20} color={themeColor} />
                  )}
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
};

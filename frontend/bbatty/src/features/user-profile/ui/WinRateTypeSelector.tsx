// features/user-profile/ui/WinRateTypeSelector.tsx
import React, { useState } from 'react';
import { View, TouchableOpacity, Text, Modal, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './WinRateTypeSelector.style';

export type WinRateType = 'summary' | 'opponent' | 'stadium' | 'dayOfWeek';

interface WinRateTypeSelectorProps {
  selectedType: WinRateType;
  onTypeChange: (type: WinRateType) => void;
}

const winRateTypes = [
  { key: 'summary' as WinRateType, label: '홈' },
  { key: 'opponent' as WinRateType, label: '팀별' },
  { key: 'stadium' as WinRateType, label: '구장별' },
  { key: 'dayOfWeek' as WinRateType, label: '요일별' },
];

export const WinRateTypeSelector: React.FC<WinRateTypeSelectorProps> = ({ selectedType, onTypeChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const themeColor = useThemeColor();

  const selectedLabel = winRateTypes.find((type) => type.key === selectedType)?.label || '홈';

  const handleSelect = (type: WinRateType) => {
    onTypeChange(type);
    setIsOpen(false);
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={[styles.selector, { borderColor: themeColor }]} onPress={() => setIsOpen(true)}>
        <Text style={styles.selectedText}>{selectedLabel}</Text>
        <Ionicons name='chevron-down' size={20} color={themeColor} />
      </TouchableOpacity>

      <Modal visible={isOpen} transparent animationType='fade' onRequestClose={() => setIsOpen(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setIsOpen(false)}>
          <View style={styles.modalContent}>
            <ScrollView>
              {winRateTypes.map((type) => (
                <TouchableOpacity
                  key={type.key}
                  style={[styles.option, selectedType === type.key && { backgroundColor: `${themeColor}15` }]}
                  onPress={() => handleSelect(type.key)}
                >
                  <Text
                    style={[styles.optionText, selectedType === type.key && { color: themeColor, fontWeight: '600' }]}
                  >
                    {type.label}
                  </Text>
                  {selectedType === type.key && <Ionicons name='checkmark' size={20} color={themeColor} />}
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
};

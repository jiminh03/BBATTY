import React from 'react';
import { View, Text, TouchableOpacity, Animated, Image } from 'react-native';
import { Team } from '../types';
import { styles } from './TeamGrid.styles';

interface TeamGridProps {
  teams: Team[];
  onSelectTeam: (teamId: number) => void;
  selectedTeamId?: number | null;
}

export const TeamGrid: React.FC<TeamGridProps> = ({ teams, onSelectTeam, selectedTeamId }) => {
  const scaleValue = React.useRef(new Animated.Value(1)).current;

  const handlePressIn = () => {
    Animated.spring(scaleValue, {
      toValue: 0.95,
      useNativeDriver: true,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(scaleValue, {
      toValue: 1,
      friction: 3,
      tension: 40,
      useNativeDriver: true,
    }).start();
  };

  return (
    <View style={styles.teamGrid}>
      {teams.map((team) => {
        const isSelected = selectedTeamId === team.id;

        return (
          <TouchableOpacity
            key={team.id}
            style={[styles.teamCard, isSelected && styles.teamCardSelected]}
            onPress={() => onSelectTeam(team.id)}
            onPressIn={handlePressIn}
            onPressOut={handlePressOut}
            activeOpacity={0.7}
          >
            <Animated.View style={[styles.teamCardContent, { transform: [{ scale: scaleValue }] }]}>
              <View style={[styles.teamLogoContainer, isSelected && styles.teamLogoContainerSelected]}>
                {/* 실제로는 Image 컴포넌트 사용 */}
                {/* <Image source={{ uri: team.logoUrl }} style={styles.teamLogo} /> */}
                <Text style={styles.teamLogoEmoji}>{team.logo}</Text>
              </View>
              <Text style={[styles.teamName, isSelected && styles.teamNameSelected]}>
                {team.name} {/* 팀명만 표시 (예: 트윈스, 위즈) */}
              </Text>
            </Animated.View>
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

import React from 'react';
import { View, Text, TouchableOpacity, Animated, Image } from 'react-native';
import { Team } from '../../../shared/team/teamTypes';
import { styles } from './TeamGrid.styles';

interface TeamGridProps {
  teams: Team[];
  onSelectTeam: (teamId: number) => void;
  selectedTeamId?: number | null;
}

export const TeamGrid: React.FC<TeamGridProps> = ({ teams, onSelectTeam, selectedTeamId }) => {
  const scaleValue = React.useRef(new Animated.Value(1)).current;

  return (
    <View style={styles.teamGrid}>
      {teams.map((team) => {
        const isSelected = selectedTeamId === team.id;

        return (
          <TouchableOpacity
            key={team.id}
            style={[styles.teamCard, isSelected && styles.teamCardSelected]}
            onPress={() => onSelectTeam(team.id)}
            activeOpacity={0.7}
          >
            <Animated.View style={[styles.teamCardContent, { transform: [{ scale: scaleValue }] }]}>
              <View style={[styles.teamLogoContainer, isSelected && styles.teamLogoContainerSelected]}>
                <Image source={team.imagePath as any} style={styles.teamLogo} />
              </View>
              <Text style={[styles.teamName, isSelected && styles.teamNameSelected]}>{team.name}</Text>
            </Animated.View>
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

import React, { memo } from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';
import { Team } from '../../../team/teamTypes';

type Props = {
  team: Team;
  onPress: (teamId: number) => void;
};

function TeamCardBase({ team, onPress }: Props) {
  const source = typeof team.imagePath === 'number'
    ? team.imagePath
    : { uri: team.imagePath as string };

  return (
    <TouchableOpacity
      style={styles.card}
      activeOpacity={0.85}
      onPress={() => onPress(team.id)}
    >
      <View style={[styles.logoWrap, { backgroundColor: '#fff' }]}>
        <Image source={source} style={styles.logo} resizeMode="contain" />
      </View>
      <Text style={styles.name} numberOfLines={1}>
        {team.name}
      </Text>
    </TouchableOpacity>
  );
}

export const TeamCard = memo(TeamCardBase);

const styles = StyleSheet.create({
  card: {
    width: '30%',
    alignItems: 'center',
    marginVertical: 10,
  },
  logoWrap: {
    width: 70,
    height: 70,
    borderRadius: 35,
    alignItems: 'center',
    justifyContent: 'center',
    // shadow (iOS)
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowOffset: { width: 0, height: 3 },
    shadowRadius: 6,
    // shadow (Android)
    elevation: 3,
  },
  logo: { width: 46, height: 46 },
  name: { marginTop: 8, fontSize: 12, color: '#333' },
});

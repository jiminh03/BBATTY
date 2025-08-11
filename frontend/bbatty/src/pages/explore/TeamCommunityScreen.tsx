import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';

type Props = ExploreStackScreenProps<'TeamCommunity'>;

export default function TeamCommunityScreen({ navigation, route }: Props) {
  return (
    <View style={styles.placeholderContainer}>
      <Text style={styles.placeholderText}>타팀 커뮤니티 기능이 곧 제공될 예정입니다.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  placeholderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  placeholderText: {
    fontSize: 16,
    color: '#999999',
    textAlign: 'center',
  },
});
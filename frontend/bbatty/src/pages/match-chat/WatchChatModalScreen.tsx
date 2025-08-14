import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp, StackScreenProps } from '@react-navigation/stack';
import type { RootStackParamList } from '../../navigation/types';
import { MatchChatRoomScreen } from './MatchChatRoomScreen';

type NavigationProp = StackNavigationProp<RootStackParamList, 'WatchChatModal'>;
type RouteProp = StackScreenProps<RootStackParamList, 'WatchChatModal'>['route'];

export const WatchChatModalScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RouteProp>();
  const insets = useSafeAreaInsets();
  
  const { room, websocketUrl, sessionToken } = route.params;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* 채팅방 화면 (헤더 포함) */}
      <MatchChatRoomScreen 
        route={{
          params: { room, websocketUrl, sessionToken }
        } as any}
        navigation={navigation as any}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
});
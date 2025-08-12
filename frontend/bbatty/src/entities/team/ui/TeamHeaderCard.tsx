import React from 'react';
import { View, Text, Image, StyleSheet, Pressable } from 'react-native';

type Props = {
  teamLogo: string;
  teamName: string;
  rankText: string;
  recordText: string;
  onPressChat?: () => void;
  accentColor?: string; // ← 팀 색을 외부에서 주입
};

export default function TeamHeaderCard({
  teamLogo,
  teamName,
  rankText,
  recordText,
  onPressChat,
  accentColor = '#E85A5A',
}: Props) {
  return (
    <View style={[s.wrap, { backgroundColor: accentColor }]}>
      <View style={s.left}>
        <Image source={{ uri: teamLogo }} style={s.logo} />
        <View style={{ marginLeft: 12 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={s.team}>{teamName}</Text>
            <View style={s.badge}/>
          </View>
          <Text style={s.rank}>{rankText}</Text>
          <Text style={s.record}>{recordText}</Text>
        </View>
      </View>
      <Pressable style={s.chat} onPress={onPressChat}>
        <Text style={s.chatTxt}>VS 두산{'\n'}실시간 채팅방 가기</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  wrap: { padding: 30, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  left: { flexDirection: 'row', alignItems: 'center' },
  logo: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#fff' },
  team: { color: '#fff', fontSize: 18, fontWeight: '800' },
  badge: { width: 14, height: 14, borderRadius: 7, backgroundColor: '#4CD964', marginLeft: 6 },
  rank: { color: '#fff', marginTop: 2, fontWeight: '700' },
  record: { color: 'rgba(255,255,255,0.85)', marginTop: 2, fontSize: 12 },
  chat: { backgroundColor: '#fff', paddingVertical: 8, paddingHorizontal: 10, borderRadius: 10 },
  chatTxt: { color: '#222', fontSize: 12, fontWeight: '700', textAlign: 'center' },
});

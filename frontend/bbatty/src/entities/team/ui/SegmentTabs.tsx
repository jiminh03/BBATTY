import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';

type Props = {
  value: 'best' | 'all';
  onChange: (v: 'best' | 'all') => void;
  accentColor?: string; // ✅ 팀 색 전달
};

export default function SegmentTabs({ value, onChange, accentColor = '#000000ff' }: Props) {
  return (
    <View style={s.bar}>
      {(['best', 'all'] as const).map((k) => {
        const active = value === k;
        return (
          <Pressable key={k} style={s.tab} onPress={() => onChange(k)}>
            <Text style={[s.txt, active && s.activeTxt]}>{k === 'best' ? '베스트' : '전체'}</Text>
            {active && <View style={[s.ind, { backgroundColor: accentColor }]} />}
          </Pressable>
        );
      })}
    </View>
  );
}

const s = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#eee',
  },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 12 },
  txt: { color: '#888', fontWeight: '700', fontSize: 18 },
  activeTxt: { color: '#111' },
  ind: { height: 3, width: '60%', borderRadius: 2, marginTop: 8 },
});

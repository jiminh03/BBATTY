// entities/post/ui/TeamNewsSection.tsx
import React from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Dimensions,
  Linking,
  StyleProp,
  ViewStyle,
} from 'react-native';
import { useTeamNewsQuery } from '../../post/queries/usePostQueries';
import { useNavigation } from '@react-navigation/native';

const { width } = Dimensions.get('window');
const H_GAP = 12;
const ITEM_W = Math.min(300, Math.round(width * 0.78));

type Props = {
  teamId: number;
  accentColor?: string;
  limit?: number;
  style?: StyleProp<ViewStyle>; // ✅ style prop 추가
};

export default function TeamNewsSection({
  teamId,
  accentColor = '#E95F2E',
  limit = 5,
  style,
}: Props) {
  const nav = useNavigation<any>();
  const q = useTeamNewsQuery(teamId, limit);

  const openItem = (item: any) => {
    if (item?.postId) {
      nav.navigate('HomeStack', { screen: 'PostDetail', params: { postId: item.postId } });
    } else if (item?.link) {
      Linking.openURL(item.link).catch(() => {});
    }
  };

  if (q.isLoading) {
    return (
      <View style={[s.section, style]}>
        <Text style={[s.title, { color: accentColor }]}>팀 최신 뉴스</Text>
        <ActivityIndicator style={{ marginTop: 12 }} />
      </View>
    );
  }

  if (q.isError || !q.data?.length) {
    return (
      <View style={[s.section, style]}>
        <Text style={[s.title, { color: accentColor }]}>팀 최신 뉴스</Text>
        <Text style={s.empty}>지금 보여줄 뉴스가 없어요.</Text>
      </View>
    );
  }

  return (
    <View style={[s.section, style, { overflow: 'visible' }]}>
      <Text style={[s.title, { color: accentColor }]}>팀 최신 뉴스</Text>

      <FlatList
        data={q.data}
        keyExtractor={(_, i) => String(i)}
        horizontal
        showsHorizontalScrollIndicator={false}
        snapToInterval={ITEM_W + H_GAP}
        decelerationRate="fast"
        style={{ overflow: 'visible' }}
        removeClippedSubviews={false}
        contentContainerStyle={{
          paddingVertical: 8,
          paddingLeft: 16,
          paddingRight: 28,
        }}
        ItemSeparatorComponent={() => <View style={{ width: H_GAP }} />}
        renderItem={({ item }) => (
          <TouchableOpacity
            activeOpacity={0.9}
            style={[s.card, { width: ITEM_W }]}
            onPress={() => openItem(item)}
          >
            {!!item.thumbnailUrl && (
              <Image source={{ uri: item.thumbnailUrl }} style={s.thumb} resizeMode="cover" />
            )}
            <View style={s.meta}>
              <Text style={s.cardTitle}>{item.title}</Text>
              {!!item.summary && <Text style={s.summary}>{item.summary}</Text>}
              <View style={s.row}>
                {!!item.source && <Text style={s.source}>{item.source}</Text>}
                {!!item.publishedAt && <Text style={s.time}> · {fmt(item.publishedAt)}</Text>}
              </View>
            </View>
          </TouchableOpacity>
        )}
      />
    </View>
  );
}

function fmt(iso?: string) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const mm = String(d.getMinutes()).padStart(2, '0');
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${mm}`;
  } catch {
    return '';
  }
}

const s = StyleSheet.create({
  section: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, backgroundColor: '#fff' },
  title: { fontSize: 18, fontWeight: '800' },
  empty: { marginTop: 12, color: '#999' },

  card: {
    borderRadius: 14,
    backgroundColor: '#fff',
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOpacity: 0.08,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 10,
    elevation: 5,
  },
  thumb: { width: '100%', height: 108, backgroundColor: '#E9ECEF' },
  meta: { paddingHorizontal: 12, paddingVertical: 10 },
  cardTitle: { fontSize: 15, fontWeight: '700', color: '#111' },
  summary: { marginTop: 6, fontSize: 13, color: '#444', lineHeight: 18 },
  row: { flexDirection: 'row', alignItems: 'center', marginTop: 8 },
  source: { fontSize: 12, color: '#888' },
  time: { fontSize: 12, color: '#888' },
});

import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Switch, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { MyPageStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { profileApi } from '../../../features/user-profile';
import { UserPrivacySettings } from '../../../features/user-profile';
import { useTokenStore } from '../../../shared/api/token/tokenStore';
import { resetToAuth } from '../../../navigation/navigationRefs';
import { isOk } from '../../../shared/utils/result';
import { styles } from './SettingsScreen.style';
import { authApi } from '../../../features/user-auth';

type SettingsScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'Settings'>;

export default function SettingsScreen() {
  const navigation = useNavigation<SettingsScreenNavigationProp>();
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();
  const { clearTokens } = useTokenStore();
  const queryClient = useQueryClient();

  // 프라이버시 설정 조회
  const { data: privacySettings, isLoading } = useQuery({
    queryKey: ['privacySettings'],
    queryFn: async () => {
      const result = await profileApi.getPrivacySettings();
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
  });

  // 프라이버시 설정 업데이트
  const updatePrivacyMutation = useMutation({
    mutationFn: async (settings: UserPrivacySettings) => {
      const result = await profileApi.updatePrivacySettings(settings);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['privacySettings'] });
    },
    onError: (error) => {
      Alert.alert('오류', '설정 업데이트에 실패했습니다.');
    },
  });

  const updatePrivacySetting = (field: keyof UserPrivacySettings, value: boolean) => {
    if (!privacySettings) return;

    const newSettings = {
      ...privacySettings,
      [field]: value,
    };

    updatePrivacyMutation.mutate(newSettings);
  };

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃하시겠습니까?', [
      { text: '취소', style: 'cancel' },
      {
        text: '로그아웃',
        style: 'destructive',
        onPress: async () => {
          await clearTokens();
          resetToAuth();
        },
      },
    ]);
  };

  const handleDeleteAccount = () => {
    Alert.alert('회원탈퇴', '정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.', [
      { text: '취소', style: 'cancel' },
      {
        text: '탈퇴',
        style: 'destructive',
        onPress: async () => {
          const result = await authApi.deleteAccount();
          if (isOk(result)) {
            await clearTokens();
            resetToAuth();
          } else {
            Alert.alert('오류', '회원탈퇴에 실패했습니다.');
          }
        },
      },
    ]);
  };

  if (isLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>설정을 불러오는 중...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={[styles.container, { paddingTop: insets.top }]} showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
          <Ionicons name='chevron-back' size={24} color='#333333' />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>설정</Text>
        <View style={styles.headerSpacer} />
      </View>

      {/* 프로필 관리 */}
      <View style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>프로필 관리</Text>

        <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('ProfileEdit')}>
          <View style={styles.menuItemLeft}>
            <Ionicons name='person-outline' size={24} color='#666666' />
            <Text style={styles.menuLabel}>프로필 변경</Text>
          </View>
          <Ionicons name='chevron-forward' size={20} color='#CCCCCC' />
        </TouchableOpacity>
      </View>

      {/* 조회 허용 설정 */}
      <View style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>조회 허용 설정</Text>

        <View style={styles.settingItem}>
          <Text style={styles.settingLabel}>게시글 조회 허용</Text>
          <Switch
            value={privacySettings?.allowViewPosts || false}
            onValueChange={(value) => updatePrivacySetting('allowViewPosts', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
            disabled={updatePrivacyMutation.isPending}
          />
        </View>

        <View style={styles.settingItem}>
          <Text style={styles.settingLabel}>통계 조회 허용</Text>
          <Switch
            value={privacySettings?.allowViewStats || false}
            onValueChange={(value) => updatePrivacySetting('allowViewStats', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
            disabled={updatePrivacyMutation.isPending}
          />
        </View>

        <View style={styles.settingItem}>
          <Text style={styles.settingLabel}>직관기록 조회 허용</Text>
          <Switch
            value={privacySettings?.allowViewDirectViewHistory || false}
            onValueChange={(value) => updatePrivacySetting('allowViewDirectViewHistory', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
            disabled={updatePrivacyMutation.isPending}
          />
        </View>

        <Text style={styles.settingDescription}>체크 해제 시 다른 사용자가 해당 정보를 볼 수 없습니다.</Text>
      </View>

      {/* 계정 관리 */}
      <View style={styles.sectionContainer}>
        <Text style={styles.sectionTitle}>계정 관리</Text>

        <TouchableOpacity style={styles.dangerMenuItem} onPress={handleLogout}>
          <Text style={styles.dangerMenuLabel}>로그아웃</Text>
          <Ionicons name='log-out-outline' size={20} color='#F44336' />
        </TouchableOpacity>

        <TouchableOpacity style={styles.dangerMenuItem} onPress={handleDeleteAccount}>
          <Text style={styles.dangerMenuLabel}>회원탈퇴</Text>
          <Ionicons name='person-remove-outline' size={20} color='#F44336' />
        </TouchableOpacity>
      </View>

      <View style={{ height: insets.bottom + 20 }} />
    </ScrollView>
  );
}

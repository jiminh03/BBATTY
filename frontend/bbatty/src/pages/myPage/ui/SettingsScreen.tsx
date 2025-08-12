import React, { useEffect, useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Switch, Alert, BackHandler } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useFocusEffect } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import { MyPageStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useProfile, useUpdatePrivacySettings } from '../../../features/user-profile';
import { UserPrivacySettings } from '../../../features/user-profile';
import { useTokenStore } from '../../../shared/api/token/tokenStore';
import { useUserStore } from '../../../entities/user';
import { isOk } from '../../../shared/utils/result';
import { styles } from './SettingsScreen.style';
import { authApi } from '../../../features/user-auth';

type SettingsScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'Settings'>;

export default function SettingsScreen() {
  const navigation = useNavigation<SettingsScreenNavigationProp>();
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();
  const { clearTokens } = useTokenStore();
  const { reset: resetUser } = useUserStore();

  // 프로필 및 프라이버시 설정 훅 사용
  const { data: profile, isLoading: isProfileLoading } = useProfile();
  const updatePrivacyMutation = useUpdatePrivacySettings();

  // 로컬 상태로 Switch 값 관리 (깜빡임 방지)
  const [localSettings, setLocalSettings] = useState<UserPrivacySettings | null>(null);

  // 프로필 데이터가 처음 로드될 때만 로컬 상태 초기화
  useEffect(() => {
    if (profile && !localSettings) {
      setLocalSettings({
        postsPublic: profile.postsPublic,
        statsPublic: profile.statsPublic,
        attendanceRecordsPublic: profile.attendanceRecordsPublic,
      });
    }
  }, [profile, localSettings]);

  // 화면 포커스 시 하단 탭 숨기기
  useFocusEffect(
    React.useCallback(() => {
      const parent = navigation.getParent();
      if (parent) {
        parent.setOptions({
          tabBarStyle: { display: 'none' },
        });
      }

      // 화면 블러 시 하단 탭 다시 보이기
      return () => {
        if (parent) {
          parent.setOptions({
            tabBarStyle: { display: 'flex' },
          });
        }
      };
    }, [navigation])
  );

  const updatePrivacySetting = (field: keyof UserPrivacySettings, value: boolean) => {
    if (!localSettings) return;

    // API 호출 중이면 무시 (중복 호출 방지)
    if (updatePrivacyMutation.isPending) {
      console.log('API 호출 중이므로 무시');
      return;
    }

    // 즉시 로컬 상태 업데이트 (깜빡임 방지)
    const newSettings = {
      ...localSettings,
      [field]: value,
    };

    setLocalSettings(newSettings);

    updatePrivacyMutation.mutate(newSettings, {
      onError: (error) => {
        // 에러 시 로컬 상태를 원래대로 복원
        console.log('API 에러 - 상태 복원:', error);
        setLocalSettings(localSettings);
        Alert.alert('오류', '설정 업데이트에 실패했습니다.');
      },
    });
  };

  const handleLogout = () => {
    Alert.alert('로그아웃', '정말 로그아웃하시겠습니다?', [
      { text: '취소', style: 'cancel' },
      {
        text: '로그아웃',
        style: 'destructive',
        onPress: async () => {
          try {
            // 토큰 및 사용자 데이터 삭제
            await clearTokens();
            await resetUser();
            
            console.log('로그아웃 완료 - 앱이 자동으로 로그인 화면으로 전환됩니다');
          } catch (error) {
            console.error('로그아웃 실패:', error);
            Alert.alert('오류', '로그아웃 중 오류가 발생했습니다.');
          }
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
          try {
            const result = await authApi.deleteAccount();
            if (isOk(result)) {
              await clearTokens();
              await resetUser();
              
              console.log('회원탈퇴 완료 - 앱을 종료합니다');
              
              // 탈퇴 성공 알림 후 앱 종료
              Alert.alert(
                '회원탈퇴 완료', 
                '회원탈퇴가 완료되었습니다. 앱을 종료합니다.',
                [
                  {
                    text: '확인',
                    onPress: () => {
                      // 앱 종료
                      BackHandler.exitApp();
                    },
                  },
                ],
                { cancelable: false }
              );
            } else {
              Alert.alert('오류', '회원탈퇴에 실패했습니다.');
            }
          } catch (error) {
            console.error('회원탈퇴 실패:', error);
            Alert.alert('오류', '회원탈퇴 중 오류가 발생했습니다.');
          }
        },
      },
    ]);
  };

  if (isProfileLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>설정을 불러오는 중...</Text>
      </View>
    );
  }

  if (!profile) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>프로필을 찾을 수 없습니다.</Text>
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
            value={localSettings?.postsPublic || false}
            onValueChange={(value) => updatePrivacySetting('postsPublic', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
          />
        </View>

        <View style={styles.settingItem}>
          <Text style={styles.settingLabel}>통계 조회 허용</Text>
          <Switch
            value={localSettings?.statsPublic || false}
            onValueChange={(value) => updatePrivacySetting('statsPublic', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
          />
        </View>

        <View style={styles.settingItem}>
          <Text style={styles.settingLabel}>직관기록 조회 허용</Text>
          <Switch
            value={localSettings?.attendanceRecordsPublic || false}
            onValueChange={(value) => updatePrivacySetting('attendanceRecordsPublic', value)}
            trackColor={{ false: '#E0E0E0', true: themeColor }}
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

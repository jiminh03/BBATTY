import React from 'react';
import { View, Text, Image, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { UserProfile } from '../model/profileTypes';
import { BasicStats } from '../model/statsTypes';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { Format } from '../../../shared/utils/format';
import { styles } from './UserProfileHeader.style';

interface UserProfileHeaderProps {
  profile: UserProfile;
  basicStats?: BasicStats;
  isOwner: boolean;
  onBackPress?: () => void;
  onSettingsPress?: () => void;
}

export const UserProfileHeader: React.FC<UserProfileHeaderProps> = ({
  profile,
  basicStats,
  isOwner,
  onBackPress,
  onSettingsPress,
}) => {
  const themeColor = useThemeColor();

  return (
    <View style={[styles.header, { backgroundColor: themeColor }]}>
      <View style={styles.headerContent}>
        {!isOwner && (
          <TouchableOpacity style={styles.backButton} onPress={onBackPress}>
            <Ionicons name='chevron-back' size={24} color='#FFFFFF' />
          </TouchableOpacity>
        )}

        <View style={styles.profileSection}>
          <View style={styles.profileImageContainer}>
            {profile.profileImg ? (
              <Image source={{ uri: profile.profileImg }} style={styles.profileImage} />
            ) : (
              <View style={styles.profileImagePlaceholder}>
                <Ionicons name='person' size={40} color='#CCCCCC' />
              </View>
            )}
          </View>

          <View style={styles.profileInfo}>
            <Text style={styles.nickname}>{profile.nickname}</Text>
            {basicStats && <Text style={styles.winRate}>{Format.winRate.toPercent(basicStats.winRate)}% 승률</Text>}
            {profile.introduction && (
              <Text style={styles.introduction} numberOfLines={3} ellipsizeMode="tail">
                {profile.introduction}
              </Text>
            )}
          </View>
        </View>

        {isOwner && (
          <TouchableOpacity style={styles.settingsButton} onPress={onSettingsPress}>
            <Ionicons name='settings-outline' size={24} color='#FFFFFF' />
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

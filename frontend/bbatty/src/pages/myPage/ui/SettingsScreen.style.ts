// ============================================
// pages/SettingsScreen/SettingsScreen.style.ts
// ============================================

import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  backButton: {
    padding: 4,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
  },
  headerSpacer: {
    width: 32,
  },
  sectionContainer: {
    backgroundColor: '#FFFFFF',
    marginTop: 20,
    paddingVertical: 16,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    paddingHorizontal: 20,
    marginBottom: 16,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  settingLabel: {
    fontSize: 16,
    color: '#333333',
  },
  settingDescription: {
    fontSize: 12,
    color: '#999999',
    paddingHorizontal: 20,
    marginTop: 8,
    lineHeight: 16,
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  menuLabel: {
    fontSize: 16,
    color: '#333333',
    marginLeft: 12,
  },
  dangerMenuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  dangerMenuLabel: {
    fontSize: 16,
    color: '#F44336',
  },
});

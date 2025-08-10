import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 20,
  },
  backButton: {
    paddingVertical: 16,
    marginBottom: 20,
  },
  backButtonText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333333',
  },
});

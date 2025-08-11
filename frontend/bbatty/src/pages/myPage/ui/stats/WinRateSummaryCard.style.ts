import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    position: 'relative',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    marginBottom: 12,
  },
  progressCard: {
    marginTop: 12,
    position: 'relative',
  },
  progressContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  progressItem: {
    alignItems: 'center',
  },
  progressCenter: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
  },
  progressPercentage: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  progressLabel: {
    fontSize: 16,
    color: '#333333',
    marginTop: 12,
    fontWeight: '600',
  },
  circleBackground: {
    backgroundColor: 'transparent',
  },
  circleProgress: {
    overflow: 'hidden',
  },
  progressArc: {
    backgroundColor: 'transparent',
    borderTopColor: 'transparent',
    borderRightColor: 'transparent',
    borderBottomColor: 'transparent',
    borderLeftColor: 'transparent',
  },
});
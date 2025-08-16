import { StyleSheet, Platform } from 'react-native';

export const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },

  // HEADER
  header: {
    height: 90,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
  },
  backText: { color: '#fff', fontSize: 28, marginTop: 23, fontWeight: '600', width: 24 },
  headerTitle: { flex: 1, textAlign: 'left', marginTop: 25, marginLeft: 3, color: '#fff', fontSize: 18, fontWeight: '700' },

  // CONTENT
  contentWrap: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 16, // 기본 패딩만 유지
    minHeight: '100%', // 전체 높이까지 확장
  },
  label: { fontSize: 16, color: '#111', fontWeight: '700', marginBottom: 8 },
  titleInput: {
    height: 44,
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#F5F6F7',
    paddingHorizontal: 12,
    fontSize: 14,
    color: '#111',
  },
  bodyInput: {
    // minHeight 제거 - 자연스럽게 확장
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#FFFFFF',
    // padding은 RichTextEditor 내부에서 처리
    fontSize: 15,
    color: '#111',
  },

  // LEGACY IMAGE GALLERY (keeping for compatibility)
  imageGallery: {
    marginTop: 8,
    marginBottom: 8,
  },
  imageContainer: {
    marginRight: 8,
    marginBottom: 8,
    position: 'relative',
  },
  imageItem: {
    width: 80,
    height: 80,
    borderRadius: 8,
  },
  imageDeleteButton: {
    position: 'absolute',
    top: -8,
    right: -8,
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#FF3B30',
    alignItems: 'center',
    justifyContent: 'center',
  },

  // TOOLBAR
  toolbar: {
    paddingTop: 10,
    paddingBottom: 10,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-start',
    backgroundColor: '#fff',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#E3E5E7',
  },
  imageBtn: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#D7DBE0',
    backgroundColor: '#FFF',
  },
  toggleWrap: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  toggleLabel: { fontSize: 14, color: '#333' },

  errorText: { color: '#FF3B30', marginTop: 6 },

  // BOTTOM BUTTON
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: Platform.select({ ios: 34, android: 16 }), // iOS Safe Area 고려
    backgroundColor: 'white', // 배경색 추가
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#E3E5E7',
  },
  submitBtn: {
    height: 48,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitText: { color: '#fff', fontSize: 18, fontWeight: '700' },

  // LOADING
  uploadingOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.3)',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
  },

  // DRAG & DROP
  draggingImage: {
    opacity: 0.7,
    transform: [{ scale: 1.05 }],
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
});
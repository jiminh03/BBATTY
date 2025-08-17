import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    minHeight: 200, // 최소 높이만 유지
    // maxHeight 제거 - 자연스럽게 확장되도록
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#FFFFFF',
    // flex 제거 - 콘텐츠에 따라 크기 조정
  },

  editorScroll: {
    // flex 제거 - ScrollView를 제거할 예정
    paddingHorizontal: 12,
    paddingVertical: 12,
  },

  content: {
    minHeight: 180,
    // 자연스럽게 확장되도록
  },

  textSegmentContainer: {
    marginVertical: 1,
    minHeight: 24,
    borderRadius: 4,
    paddingHorizontal: 4,
    paddingVertical: 1,
    position: 'relative',
  },

  textInput: {
    fontSize: 15,
    color: '#111',
    lineHeight: 22,
    padding: 0,
    margin: 0,
    textAlignVertical: 'top',
    minHeight: 22, // textContent와 동일한 최소 높이
  },

  textBlock: {
    minHeight: 22,
    justifyContent: 'center',
    paddingVertical: 0, // textInput과 동일한 패딩
  },

  emptyTextBlock: {
    minHeight: 22, // textInput과 동일한 최소 높이
    paddingVertical: 0, // 일관성 유지
    marginVertical: 1,
  },

  textContent: {
    fontSize: 15,
    color: '#111',
    lineHeight: 22,
    minHeight: 22, // textInput과 동일한 최소 높이
  },

  imageBlock: {
    marginVertical: 6,
    position: 'relative',
    padding: 4,
  },

  imageBlockDragging: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 8,
  },

  inlineImage: {
    width: '100%',
    height: 200,
    borderRadius: 8,
    backgroundColor: '#F5F6F7',
  },

  imageDeleteButton: {
    position: 'absolute',
    top: -8,
    right: -8,
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#FF3B30',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },

  imageDeleteText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    lineHeight: 16,
  },

  placeholderText: {
    fontSize: 15,
    color: '#B9BDC1',
    lineHeight: 22,
  },

  dropZoneLine: {
    position: 'absolute',
    left: -12,
    right: -12,
    height: 4, // 조금 더 두껍게
    backgroundColor: '#007AFF',
    borderRadius: 2,
    shadowColor: '#007AFF',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.6, // 그림자 더 진하게
    shadowRadius: 4,
    elevation: 8, // 안드로이드 그림자 더 진하게
    zIndex: 1000,
    // 애니메이션 효과
    opacity: 0.9,
  },

  dropZoneLineInside: {
    height: 2,
    backgroundColor: '#007AFF',
    marginVertical: 2,
    borderRadius: 1,
    opacity: 0.8,
  },

  textBlockHighlight: {
    backgroundColor: 'rgba(0, 122, 255, 0.1)',
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(0, 122, 255, 0.3)',
  },

  // 노션 스타일 드래그 오버레이
  dragOverlay: {
    width: 200,
    height: 200,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
  },

  dragImage: {
    width: '100%',
    height: '100%',
    borderRadius: 8,
    backgroundColor: '#F5F6F7',
  },
});
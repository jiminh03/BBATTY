import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    minHeight: 280,
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#FFFFFF',
  },

  editorScroll: {
    flex: 1,
    paddingHorizontal: 12,
    paddingVertical: 12,
  },

  content: {
    minHeight: 256, // minHeight - padding
  },

  textSegmentContainer: {
    marginVertical: 2,
  },

  textInput: {
    fontSize: 15,
    color: '#111',
    lineHeight: 22,
    padding: 0,
    margin: 0,
    textAlignVertical: 'top',
  },

  textBlock: {
    minHeight: 22,
    justifyContent: 'center',
  },

  emptyTextBlock: {
    minHeight: 30,
    paddingVertical: 4,
  },

  textContent: {
    fontSize: 15,
    color: '#111',
    lineHeight: 22,
  },

  imageBlock: {
    marginVertical: 8,
    position: 'relative',
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
});
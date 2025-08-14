import React, { useState, useRef, useCallback, useMemo } from 'react';
import {
  View,
  Text,
  TextInput,
  Image,
  TouchableOpacity,
  ScrollView,
  NativeSyntheticEvent,
  TextInputSelectionChangeEventData,
  PanResponder,
  Animated,
} from 'react-native';
import { styles } from './RichTextEditor.style';

interface RichTextEditorProps {
  value: string;
  onChangeText: (text: string) => void;
  onSelectionChange?: (position: number) => void;
  onImageDelete?: (imageUrl: string) => void;
  onImageMove?: (fromImageUrl: string, toPosition: number) => void;
  placeholder?: string;
  style?: any;
}

interface ContentSegment {
  id: string;
  type: 'text' | 'image';
  content: string;
  imageUrl?: string;
  startIndex: number;
  endIndex: number;
}

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value,
  onChangeText,
  onSelectionChange,
  onImageDelete,
  onImageMove,
  placeholder = '내용을 입력해주세요.',
  style,
}) => {
  const [currentEditingSegment, setCurrentEditingSegment] = useState<number | null>(null);
  const [draggingSegmentId, setDraggingSegmentId] = useState<string | null>(null);
  const dragOffsetY = useRef(new Animated.Value(0)).current;
  const dragScale = useRef(new Animated.Value(1)).current;

  const parseContent = useCallback((text: string): ContentSegment[] => {
    const segments: ContentSegment[] = [];
    const imageRegex = /!\[image\]\(([^)]+)\)/g;
    let lastIndex = 0;
    let match;
    let segmentId = 0;

    while ((match = imageRegex.exec(text)) !== null) {
      // 이미지 앞의 텍스트
      if (match.index > lastIndex) {
        const textContent = text.substring(lastIndex, match.index);
        segments.push({
          id: `text_${segmentId++}`,
          type: 'text',
          content: textContent,
          startIndex: lastIndex,
          endIndex: match.index,
        });
      }

      // 이미지 세그먼트
      segments.push({
        id: `image_${segmentId++}`,
        type: 'image',
        content: match[0], // 전체 마크다운
        imageUrl: match[1], // URL만
        startIndex: match.index,
        endIndex: match.index + match[0].length,
      });

      lastIndex = match.index + match[0].length;
    }

    // 마지막 텍스트
    if (lastIndex < text.length) {
      const textContent = text.substring(lastIndex);
      segments.push({
        id: `text_${segmentId++}`,
        type: 'text',
        content: textContent,
        startIndex: lastIndex,
        endIndex: text.length,
      });
    }

    // 텍스트가 전혀 없거나 마지막이 이미지로 끝나는 경우 빈 텍스트 세그먼트 추가
    if (segments.length === 0 || (segments.length > 0 && segments[segments.length - 1].type === 'image')) {
      segments.push({
        id: `text_${segmentId++}`,
        type: 'text',
        content: segments.length === 0 ? text : '',
        startIndex: segments.length === 0 ? 0 : text.length,
        endIndex: text.length,
      });
    }

    return segments;
  }, []);

  const segments = useMemo(() => parseContent(value), [parseContent, value]);

  const handleSegmentTextChange = (segmentIndex: number, newText: string) => {
    const segment = segments[segmentIndex];
    if (!segment || segment.type !== 'text') return;

    // 기존 content에서 해당 세그먼트 부분을 새 텍스트로 교체
    const beforeSegment = value.substring(0, segment.startIndex);
    const afterSegment = value.substring(segment.endIndex);
    const newContent = beforeSegment + newText + afterSegment;
    
    onChangeText(newContent);
  };

  const handleImagePress = (segment: ContentSegment) => {
    // 이미지 클릭 시 이미지 다음에 새로운 텍스트 세그먼트 생성하여 편집 모드로 전환
    const imageEndIndex = segment.endIndex;
    const nextSegmentIndex = segments.findIndex(s => s.startIndex >= imageEndIndex && s.type === 'text');
    
    if (nextSegmentIndex >= 0) {
      // 다음 텍스트 세그먼트가 있으면 그것을 편집 모드로
      setCurrentEditingSegment(nextSegmentIndex);
    } else {
      // 다음 텍스트 세그먼트가 없으면 이미지 뒤에 줄바꿈을 추가하여 새로운 텍스트 공간 생성
      const newContent = value.substring(0, imageEndIndex) + '\n' + value.substring(imageEndIndex);
      onChangeText(newContent);
    }
  };

  const handleImageDeletePress = (segment: ContentSegment) => {
    if (segment.imageUrl && onImageDelete) {
      onImageDelete(segment.imageUrl);
    }
  };

  const createImagePanResponder = (segment: ContentSegment) => {
    return PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: (_, gestureState) => {
        // 세로 이동이 5px 이상일 때 드래그 시작
        return Math.abs(gestureState.dy) > 5;
      },

      onPanResponderGrant: () => {
        setDraggingSegmentId(segment.id);
        Animated.parallel([
          Animated.timing(dragScale, {
            toValue: 1.05,
            duration: 150,
            useNativeDriver: false,
          }),
        ]).start();
      },

      onPanResponderMove: (_, gestureState) => {
        dragOffsetY.setValue(gestureState.dy);
      },

      onPanResponderRelease: (_, gestureState) => {
        const dragDistance = gestureState.dy;
        const segmentHeight = 220; // 대략적인 이미지 블록 높이 (이미지 200 + 마진)
        
        // 드래그 거리에 따라 이동할 위치 계산
        const moveSteps = Math.round(dragDistance / segmentHeight);
        
        if (Math.abs(moveSteps) >= 1 && segment.imageUrl && onImageMove) {
          // 현재 이미지 위치에서 moveSteps만큼 이동
          const currentIndex = segments.findIndex(s => s.id === segment.id);
          const targetPosition = Math.max(0, Math.min(segments.length - 1, currentIndex + moveSteps));
          
          if (targetPosition !== currentIndex) {
            onImageMove(segment.imageUrl, targetPosition);
          }
        }

        // 애니메이션 리셋
        Animated.parallel([
          Animated.timing(dragOffsetY, {
            toValue: 0,
            duration: 200,
            useNativeDriver: false,
          }),
          Animated.timing(dragScale, {
            toValue: 1,
            duration: 200,
            useNativeDriver: false,
          }),
        ]).start(() => {
          setDraggingSegmentId(null);
        });
      },
    });
  };

  const renderSegment = (segment: ContentSegment, index: number) => {
    if (segment.type === 'image' && segment.imageUrl) {
      const panResponder = createImagePanResponder(segment);
      const isDragging = draggingSegmentId === segment.id;
      
      return (
        <Animated.View 
          key={segment.id} 
          style={[
            styles.imageBlock,
            isDragging && {
              transform: [
                { translateY: dragOffsetY },
                { scale: dragScale },
              ],
              zIndex: 999,
              elevation: 8,
            },
          ]}
          {...panResponder.panHandlers}
        >
          <TouchableOpacity
            onPress={() => handleImagePress(segment)}
            activeOpacity={0.8}
            disabled={isDragging}
          >
            <Image
              source={{ uri: segment.imageUrl }}
              style={styles.inlineImage}
              resizeMode="cover"
            />
          </TouchableOpacity>
          
          {/* 이미지 삭제 버튼 */}
          <TouchableOpacity
            style={styles.imageDeleteButton}
            onPress={() => handleImageDeletePress(segment)}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            disabled={isDragging}
          >
            <Text style={styles.imageDeleteText}>×</Text>
          </TouchableOpacity>
        </Animated.View>
      );
    }

    // 텍스트 세그먼트
    const isEditing = currentEditingSegment === index;
    
    return (
      <View key={segment.id} style={styles.textSegmentContainer}>
        {isEditing ? (
          <TextInput
            style={styles.textInput}
            value={segment.content}
            onChangeText={(text) => handleSegmentTextChange(index, text)}
            onBlur={() => setCurrentEditingSegment(null)}
            onSelectionChange={(e) => {
              const position = segment.startIndex + e.nativeEvent.selection.start;
              onSelectionChange?.(position);
            }}
            multiline
            textAlignVertical="top"
            autoFocus
            placeholder={segment.content ? undefined : placeholder}
          />
        ) : (
          <TouchableOpacity
            onPress={() => setCurrentEditingSegment(index)}
            style={[styles.textBlock, !segment.content && styles.emptyTextBlock]}
            activeOpacity={1}
          >
            {segment.content ? (
              <Text style={styles.textContent}>{segment.content}</Text>
            ) : (
              <Text style={styles.placeholderText}>
                {index === 0 && segments.length === 1 ? placeholder : '여기에 텍스트를 입력하세요'}
              </Text>
            )}
          </TouchableOpacity>
        )}
      </View>
    );
  };

  return (
    <View style={[styles.container, style]}>
      <ScrollView 
        style={styles.editorScroll}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.content}>
          {segments.map((segment, index) => renderSegment(segment, index))}
        </View>
      </ScrollView>
    </View>
  );
};
import React, { useState, useRef, useCallback, useMemo, useEffect } from 'react';
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
  Dimensions,
  Keyboard,
} from 'react-native';
// import * as Haptics from 'expo-haptics';
import { styles } from './RichTextEditor.style';

interface RichTextEditorProps {
  value: string;
  onChangeText: (text: string) => void;
  onSelectionChange?: (position: number) => void;
  onImageDelete?: (imageUrl: string) => void;
  onImageMove?: (fromImageUrl: string, toPosition: number) => void;
  placeholder?: string;
  style?: any;
  onDragStart?: (dragInfo: { imageUrl: string; startPosition: { x: number; y: number } }) => void;
  onDragMove?: (dragOffset: { dx: number; dy: number }) => void;
  onDragEnd?: () => void;
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
  onDragStart,
  onDragMove,
  onDragEnd,
}) => {
  const [currentEditingSegment, setCurrentEditingSegment] = useState<number | null>(null);
  const [draggingSegmentId, setDraggingSegmentId] = useState<string | null>(null);
  const [dropZoneIndex, setDropZoneIndex] = useState<number | null>(null);
  const [dropZonePosition, setDropZonePosition] = useState<'before' | 'after'>('before');

  // 드래그 상태를 ref로도 보존
  const draggingSegmentIdRef = useRef<string | null>(null);
  const dropZoneIndexRef = useRef<number | null>(null);
  const dropZonePositionRef = useRef<'before' | 'after'>('before');
  const [dragPosition, setDragPosition] = useState({ x: 0, y: 0 });
  const lastDropZoneUpdate = useRef(0);
  const dropZoneUpdateTimeout = useRef<NodeJS.Timeout | null>(null);
  // 드래그 관련 상태는 부모 컴포넌트로 이동

  const segmentRefs = useRef<{ [key: string]: View | null }>({});
  const segmentLayouts = useRef<{ [key: string]: { x: number; y: number; width: number; height: number } }>({});
  const panResponderRefs = useRef<{ [key: string]: any }>({});
  // 최신 콜백 함수들을 참조하기 위한 ref
  const dragCallbacksRef = useRef({
    onDragStart,
    onDragMove,
    onDragEnd,
  });

  // 콜백 refs 업데이트
  dragCallbacksRef.current = {
    onDragStart,
    onDragMove,
    onDragEnd,
  };
  // 스크롤 관련 제거 - 부모 ScrollView 사용

  const parseContent = useCallback((text: string): ContentSegment[] => {
    const segments: ContentSegment[] = [];
    let segmentId = 0;
    let currentIndex = 0;

    // 텍스트를 줄 단위로 분할하여 각 줄을 독립적인 블록으로 처리
    const lines = text.split('\n');

    for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      const line = lines[lineIndex];

      // 이미지 마크다운 체크
      const imageMatch = line.match(/^!\[image\]\(([^)]+)\)$/);
      if (imageMatch) {
        // 이미지 세그먼트 (한 줄 전체가 이미지)
        segments.push({
          id: `image_${segmentId++}`,
          type: 'image',
          content: line,
          imageUrl: imageMatch[1],
          startIndex: currentIndex,
          endIndex: currentIndex + line.length,
        });
      } else {
        // 텍스트 세그먼트 (각 줄이 독립적인 블록)
        segments.push({
          id: `text_${segmentId++}`,
          type: 'text',
          content: line,
          startIndex: currentIndex,
          endIndex: currentIndex + line.length,
        });
      }

      currentIndex += line.length;

      // 마지막 줄이 아니면 줄바꿈 문자 길이 추가
      if (lineIndex < lines.length - 1) {
        currentIndex += 1; // \n
      }
    }

    // 빈 세그먼트가 없으면 기본 빈 텍스트 세그먼트 추가
    if (segments.length === 0) {
      segments.push({
        id: `text_${segmentId++}`,
        type: 'text',
        content: '',
        startIndex: 0,
        endIndex: 0,
      });
    }

    return segments;
  }, []);

  const segments = useMemo(() => parseContent(value), [parseContent, value]);

  // content 변경 시 드래그 상태 정리 및 캐시 무효화
  useEffect(() => {
    // PanResponder 캐시 무효화 (세그먼트 배열이 변경되었으므로)
    panResponderRefs.current = {};

    // content가 변경되었을 때 현재 드래그 중인 세그먼트가 여전히 존재하는지 확인
    if (draggingSegmentIdRef.current) {
      const currentDraggedSegment = segments.find((s) => s.id === draggingSegmentIdRef.current);
      if (!currentDraggedSegment) {
        setDropZoneIndex(null);
        setDraggingSegmentId(null);
        draggingSegmentIdRef.current = null;
        dropZoneIndexRef.current = null;
        dropZonePositionRef.current = 'before';
      }
    }
  }, [segments]);

  // 컴포넌트 언마운트 시 timeout 정리
  useEffect(() => {
    return () => {
      if (dropZoneUpdateTimeout.current) {
        clearTimeout(dropZoneUpdateTimeout.current);
      }
    };
  }, []);

  const handleSegmentTextChange = useCallback(
    (segmentIndex: number, newText: string) => {
      const segment = segments[segmentIndex];
      if (!segment || segment.type !== 'text') return;

      const newSegments = [...segments];

      if (newText.includes('\n')) {
        // Enter 키로 새 블록 생성
        const lines = newText.split('\n');
        const firstLine = lines[0];
        const remainingLines = lines.slice(1);

        // 현재 블록은 첫 번째 줄로 업데이트
        newSegments[segmentIndex] = { ...segment, content: firstLine };

        // 나머지 줄들을 새로운 블록으로 생성
        const newTextSegments = remainingLines.map((line, index) => ({
          id: `text_${Date.now()}_${index}`,
          type: 'text' as const,
          content: line,
          startIndex: 0,
          endIndex: 0,
        }));

        // 새 블록들을 현재 위치 다음에 삽입
        newSegments.splice(segmentIndex + 1, 0, ...newTextSegments);

        const updatedContent = newSegments.map((s) => s.content).join('\n');
        onChangeText(updatedContent);

        // 포커스를 새로 생성된 다음 블록으로 이동
        setTimeout(() => {
          setCurrentEditingSegment(segmentIndex + 1);
        }, 50);
      } else {
        // 일반적인 텍스트 변경
        newSegments[segmentIndex] = { ...segment, content: newText };
        const updatedContent = newSegments.map((s) => s.content).join('\n');
        onChangeText(updatedContent);
      }
    },
    [segments, onChangeText]
  );

  const handleImagePress = (segment: ContentSegment) => {
    // 이미지 클릭 시 특별한 처리 없음 (크기 조정 로직 제거)
    // 필요시 여기에 간단한 처리만 추가
  };

  const handleImageDeletePress = (segment: ContentSegment) => {
    if (segment.imageUrl && onImageDelete) {
      onImageDelete(segment.imageUrl);
    }
  };

  // 경계 고정 상태 관리
  const [isFixedBounds, setIsFixedBounds] = useState(false);
  const isFixedBoundsRef = useRef(false);

  // 컨테이너 참조 및 스크롤 위치
  const containerRef = useRef<View>(null);
  const scrollOffsetRef = useRef(0);

  const findDropZone = useCallback(
    (touchY: number) => {
      // 드래그 중인 세그먼트 찾기
      const draggedSegmentIndex = draggingSegmentIdRef.current
        ? segments.findIndex((s) => s.id === draggingSegmentIdRef.current)
        : -1;

      // 드래그 중인 세그먼트를 제외하고 위치 계산
      const segmentPositions = [];
      let currentY = 0;

      for (let i = 0; i < segments.length; i++) {
        // 드래그 중인 세그먼트는 위치 계산에서 제외
        if (i === draggedSegmentIndex) {
          continue;
        }

        const segment = segments[i];
        const layout = segmentLayouts.current[segment.id];
        let height = 40; // 기본 텍스트 높이

        if (layout && layout.height > 0) {
          height = layout.height;
        } else if (segment.type === 'image') {
          height = 200; // 기본 이미지 높이
        }

        segmentPositions.push({
          index: i,
          originalIndex: i, // 원래 인덱스 보존
          top: currentY,
          bottom: currentY + height,
          height: height,
        });

        currentY += height;
      }

      // 터치 위치가 어느 세그먼트에 속하는지 확인
      for (const pos of segmentPositions) {
        if (touchY >= pos.top && touchY <= pos.bottom) {
          const mid = (pos.top + pos.bottom) / 2;
          if (touchY <= mid) {
            const newPosition = 'before';
            const newIndex = pos.originalIndex;

            // 상태가 변경된 경우에만 업데이트 (깜빡임 방지)
            if (dropZonePositionRef.current !== newPosition || dropZoneIndexRef.current !== newIndex) {
              setDropZonePosition(newPosition);
              setDropZoneIndex(newIndex);
              dropZonePositionRef.current = newPosition;
              dropZoneIndexRef.current = newIndex;
            }
            return newIndex;
          } else {
            const newPosition = 'after';
            const newIndex = pos.originalIndex;

            // 상태가 변경된 경우에만 업데이트 (깜빡임 방지)
            if (dropZonePositionRef.current !== newPosition || dropZoneIndexRef.current !== newIndex) {
              setDropZonePosition(newPosition);
              setDropZoneIndex(newIndex);
              dropZonePositionRef.current = newPosition;
              dropZoneIndexRef.current = newIndex;
            }
            return newIndex;
          }
        }
      }

      // 범위를 벗어난 경우
      if (touchY < 0 || segmentPositions.length === 0) {
        // 첫 번째 세그먼트 앞
        const newPosition = 'before';
        const newIndex = segmentPositions.length > 0 ? segmentPositions[0].originalIndex : 0;

        if (dropZonePositionRef.current !== newPosition || dropZoneIndexRef.current !== newIndex) {
          setDropZonePosition(newPosition);
          setDropZoneIndex(newIndex);
          dropZonePositionRef.current = newPosition;
          dropZoneIndexRef.current = newIndex;
        }
        return newIndex;
      } else {
        // 마지막 세그먼트 뒤
        const lastPos = segmentPositions[segmentPositions.length - 1];
        const newPosition = 'after';
        const newIndex = lastPos ? lastPos.originalIndex : segments.length - 1;

        if (dropZonePositionRef.current !== newPosition || dropZoneIndexRef.current !== newIndex) {
          setDropZonePosition(newPosition);
          setDropZoneIndex(newIndex);
          dropZonePositionRef.current = newPosition;
          dropZoneIndexRef.current = newIndex;
        }
        return newIndex;
      }
    },
    [segments]
  );

  const createImagePanResponder = useCallback(
    (segment: ContentSegment, segmentIndex: number) => {
      // 세그먼트 배열 변경 시 캐시 무효화
      const cacheKey = `${segment.id}_${segmentIndex}_${segments.length}`;
      if (panResponderRefs.current[cacheKey]) {
        return panResponderRefs.current[cacheKey];
      }

      const panResponder = PanResponder.create({
        onStartShouldSetPanResponder: () => true, // 드래그 모드에서만 실행되므로 true
        onStartShouldSetPanResponderCapture: () => false,
        onMoveShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponderCapture: () => false,
        onShouldBlockNativeResponder: () => true,
        onPanResponderTerminationRequest: () => false,

        onPanResponderGrant: () => {
          // Long Press에서만 호출됨
        },

        onPanResponderMove: (evt, gestureState) => {
          if (!draggingSegmentIdRef.current) return;

          const relativeY = evt.nativeEvent.locationY;
          findDropZone(relativeY);
        },

        onPanResponderRelease: () => {
          if (!draggingSegmentIdRef.current) return;

          const draggedId = draggingSegmentIdRef.current;
          const dropIndex = dropZoneIndexRef.current;
          const dropPosition = dropZonePositionRef.current || 'after';

          // 드롭 실행
          if (dropIndex !== null) {
            const draggedIndex = segments.findIndex((s) => s.id === draggedId);

            if (draggedIndex !== -1) {
              const currentPosition = draggedIndex;
              let targetPosition = dropIndex;
              if (dropPosition === 'after') {
                targetPosition = dropIndex + 1;
              }

              if (targetPosition > draggedIndex) {
                targetPosition = targetPosition - 1;
              }

              if (currentPosition !== targetPosition) {
                const newSegments = [...segments];
                const [movedSegment] = newSegments.splice(draggedIndex, 1);
                newSegments.splice(targetPosition, 0, movedSegment);
                const newContent = newSegments.map((s) => s.content).join('\n');

                // 상태 초기화 후 content 변경
                setDropZoneIndex(null);
                setDraggingSegmentId(null);
                draggingSegmentIdRef.current = null;
                dropZoneIndexRef.current = null;
                dropZonePositionRef.current = 'before';

                onChangeText(newContent);
              }
            }
          }

          // 상태 정리
          if (draggingSegmentIdRef.current) {
            setDropZoneIndex(null);
            setDraggingSegmentId(null);
            draggingSegmentIdRef.current = null;
            dropZoneIndexRef.current = null;
            dropZonePositionRef.current = 'before';
          }

          dragCallbacksRef.current.onDragEnd?.();
        },
      });

      // 캐시에 저장
      panResponderRefs.current[cacheKey] = panResponder;
      return panResponder;
    },
    [segments, findDropZone]
  ); // segments 의존성 추가

  const reorganizeSegments = (fromIndex: number, toIndex: number) => {
    if (fromIndex === toIndex) return;

    const fromSegment = segments[fromIndex];
    if (!fromSegment || fromSegment.type !== 'image') return;

    // 1. 이동할 이미지를 제거하고 새 위치에 삽입
    const newSegments = [...segments];
    const [movedSegment] = newSegments.splice(fromIndex, 1);

    // 2. 삽입 위치 계산
    const actualToIndex = toIndex > fromIndex ? toIndex - 1 : toIndex;
    let insertIndex = dropZonePosition === 'before' ? actualToIndex : actualToIndex + 1;

    // 3. 새 위치에 이미지 삽입
    newSegments.splice(insertIndex, 0, movedSegment);

    // 4. 빈 텍스트 세그먼트 제거 후 content 재구성
    const filteredSegments = newSegments.filter((segment) => {
      if (segment.type === 'image') return true;
      if (segment.type === 'text' && segment.content.trim().length > 0) return true;
      return false;
    });

    // 빈 상태면 기본 빈 텍스트 세그먼트 하나만 유지
    if (filteredSegments.length === 0 || filteredSegments.every((s) => s.type === 'image')) {
      filteredSegments.push({
        id: `text_${Date.now()}`,
        type: 'text',
        content: '',
        startIndex: 0,
        endIndex: 0,
      });
    }

    const newContent = filteredSegments.map((segment) => segment.content).join('\n');

    onChangeText(newContent);
  };

  const renderSegment = useCallback(
    (segment: ContentSegment, index: number) => {
      const isDropZoneBefore = dropZoneIndex === index && dropZonePosition === 'before';
      const isDropZoneAfter = dropZoneIndex === index && dropZonePosition === 'after';
      const isDragging = draggingSegmentId === segment.id;

      if (segment.type === 'image' && segment.imageUrl) {
        const panResponder = createImagePanResponder(segment, index);

        return (
          <View key={segment.id} style={{ position: 'relative' }}>
            {/* 드롭 존 표시 선 - before */}
            {isDropZoneBefore && draggingSegmentId && (
              <View
                style={[
                  styles.dropZoneLine,
                  {
                    top: -2,
                    zIndex: 1000,
                    backgroundColor: '#007AFF', // 하늘파랑
                  },
                ]}
              />
            )}

            <View
              ref={(ref: View | null) => {
                segmentRefs.current[segment.id] = ref;
              }}
              style={[styles.imageBlock]}
              onLayout={(event) => {
                const layout = event.nativeEvent.layout;
                segmentLayouts.current[segment.id] = layout;
              }}
              {...(draggingSegmentIdRef.current === segment.id ? panResponder.panHandlers : {})}
            >
              <TouchableOpacity
                onPress={() => handleImagePress(segment)}
                onLongPress={() => {
                  if (draggingSegmentIdRef.current) return;

                  setDraggingSegmentId(segment.id);
                  draggingSegmentIdRef.current = segment.id;

                  Keyboard.dismiss();
                  setCurrentEditingSegment(null);

                  dragCallbacksRef.current.onDragStart?.({
                    imageUrl: segment.imageUrl!,
                    startPosition: { x: 0, y: 0 },
                  });

                  setDropZonePosition('before');
                  setDropZoneIndex(index);
                  dropZonePositionRef.current = 'before';
                  dropZoneIndexRef.current = index;
                }}
                delayLongPress={400} // 0.4초로 단축
                activeOpacity={0.6} // 더 명확한 시각적 피드백
                disabled={isDragging}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                <Image
                  source={{ uri: segment.imageUrl }}
                  style={[
                    styles.inlineImage,
                    isDragging && {
                      opacity: 0.3, // 더 투명하게
                      transform: [{ scale: 0.95 }], // 살짝 작게
                    },
                  ]}
                  resizeMode='cover'
                />

                {/* 드래그 중일 때 오버레이 표시 */}
                {isDragging && (
                  <View
                    style={[
                      styles.inlineImage,
                      {
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        backgroundColor: 'rgba(0, 122, 255, 0.1)',
                        borderWidth: 2,
                        borderColor: '#007AFF',
                        borderStyle: 'dashed',
                        borderRadius: 8,
                        justifyContent: 'center',
                        alignItems: 'center',
                      },
                    ]}
                  >
                    <Text
                      style={{
                        color: '#007AFF',
                        fontSize: 16,
                        fontWeight: '600',
                        backgroundColor: 'white',
                        paddingHorizontal: 8,
                        paddingVertical: 4,
                        borderRadius: 4,
                        overflow: 'hidden',
                      }}
                    >
                      드래그 중...
                    </Text>
                  </View>
                )}
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
            </View>

            {/* 드롭 존 표시 선 - after */}
            {isDropZoneAfter && draggingSegmentId && (
              <View
                style={[
                  styles.dropZoneLine,
                  {
                    bottom: -2,
                    zIndex: 1000,
                    backgroundColor: '#007AFF', // 하늘파랑
                  },
                ]}
              />
            )}
          </View>
        );
      }

      // 텍스트 세그먼트
      const isEditing = currentEditingSegment === index;

      return (
        <View key={segment.id} style={{ position: 'relative' }}>
          {/* before 드롭 존 표시 선 */}
          {isDropZoneBefore && draggingSegmentId && (
            <View
              style={[
                styles.dropZoneLine,
                {
                  top: -2,
                  zIndex: 1000,
                  backgroundColor: '#007AFF', // 하늘파랑
                },
              ]}
            />
          )}

          <View
            ref={(ref: View | null) => {
              segmentRefs.current[segment.id] = ref;
            }}
            style={styles.textSegmentContainer}
            onLayout={(event) => {
              const layout = event.nativeEvent.layout;
              segmentLayouts.current[segment.id] = layout;
            }}
          >
            {isEditing ? (
              <TextInput
                style={styles.textInput}
                value={segment.content}
                onChangeText={(text) => handleSegmentTextChange(index, text)}
                onBlur={() => setCurrentEditingSegment(null)}
                onKeyPress={(e) => {
                  // 빈 텍스트 블록에서 Backspace: 블록 삭제 및 이전 블록으로 포커스 이동
                  if (e.nativeEvent.key === 'Backspace' && segment.content === '' && index > 0) {
                    e.preventDefault();

                    // 현재 빈 블록 삭제
                    const newSegments = segments.filter((_, i) => i !== index);
                    const newContent = newSegments.map((seg) => seg.content).join('\n');
                    onChangeText(newContent);

                    // 이전 텍스트 블록으로 포커스 이동
                    setTimeout(() => {
                      const prevIndex = index - 1;
                      if (prevIndex >= 0 && newSegments[prevIndex] && newSegments[prevIndex].type === 'text') {
                        setCurrentEditingSegment(prevIndex);
                      }
                    }, 50);
                  }
                }}
                onSelectionChange={(e) => {
                  const position = segment.startIndex + e.nativeEvent.selection.start;
                  onSelectionChange?.(position);
                }}
                multiline
                textAlignVertical='top'
                autoFocus
                placeholder=''
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
                  <Text style={[styles.textContent, { color: '#B9BDC1' }]}>내용을 입력해주세요.</Text>
                )}
              </TouchableOpacity>
            )}
          </View>

          {/* after 드롭 존 표시 선 */}
          {isDropZoneAfter && draggingSegmentId && (
            <View
              style={[
                styles.dropZoneLine,
                {
                  bottom: -2,
                  zIndex: 1000,
                  backgroundColor: '#007AFF', // 하늘파랑
                },
              ]}
            />
          )}
        </View>
      );
    },
    [
      dropZoneIndex,
      draggingSegmentId,
      dropZonePosition,
      currentEditingSegment,
      createImagePanResponder,
      handleImagePress,
      handleImageDeletePress,
      handleSegmentTextChange,
      onSelectionChange,
      styles,
      placeholder,
    ]
  );

  // 전체 에디터 영역에서 드롭존 처리
  const globalPanResponder = PanResponder.create({
    onStartShouldSetPanResponder: () => false,
    onMoveShouldSetPanResponder: () => !!draggingSegmentIdRef.current, // 드래그 중일 때만 반응
    onPanResponderMove: (evt) => {
      if (!draggingSegmentIdRef.current) return;

      const relativeY = evt.nativeEvent.locationY;
      findDropZone(relativeY);
    },
    onPanResponderRelease: () => {
      // 드롭 로직은 개별 PanResponder에서 처리
    },
  });

  return (
    <View
      ref={containerRef}
      style={[styles.container, style]}
      {...globalPanResponder.panHandlers} // 전체 영역에서 드롭존 감지
    >
      <View style={styles.editorScroll}>
        <View style={styles.content}>
          {segments.map((segment, index) => renderSegment(segment, index))}

          {/* 맨 마지막 드롭존 표시 - 이제 개별 세그먼트에서 처리됨 */}
        </View>
      </View>

      {/* 드래그 오버레이는 부모 컴포넌트에서 렌더링 */}
    </View>
  );
};

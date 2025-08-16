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
import * as Haptics from 'expo-haptics';
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
    onDragEnd
  });
  
  // 콜백 refs 업데이트
  dragCallbacksRef.current = {
    onDragStart,
    onDragMove,
    onDragEnd
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

  // 컴포넌트 언마운트 시 timeout 정리
  useEffect(() => {
    return () => {
      if (dropZoneUpdateTimeout.current) {
        clearTimeout(dropZoneUpdateTimeout.current);
      }
    };
  }, []);


  const handleSegmentTextChange = useCallback((segmentIndex: number, newText: string) => {
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
      
      const updatedContent = newSegments.map(s => s.content).join('\n');
      onChangeText(updatedContent);
      
      // 포커스를 새로 생성된 다음 블록으로 이동
      setTimeout(() => {
        setCurrentEditingSegment(segmentIndex + 1);
      }, 50);
    } else {
      // 일반적인 텍스트 변경
      newSegments[segmentIndex] = { ...segment, content: newText };
      const updatedContent = newSegments.map(s => s.content).join('\n');
      onChangeText(updatedContent);
    }
  }, [segments, onChangeText]);

  const handleImagePress = (segment: ContentSegment) => {
    // 이미지 클릭 시 특별한 처리 없음 (크기 조정 로직 제거)
    // 필요시 여기에 간단한 처리만 추가
  };

  const handleImageDeletePress = (segment: ContentSegment) => {
    if (segment.imageUrl && onImageDelete) {
      onImageDelete(segment.imageUrl);
    }
  };

  const findDropZone = useCallback((touchY: number, useFocusPosition = false) => {
    // 포커스 위치 기반 드롭존 계산
    if (useFocusPosition && currentEditingSegment !== null) {
      const targetIndex = currentEditingSegment;
      const dropPosition: 'before' | 'after' = 'before';
      
      setDropZonePosition(dropPosition);
      setDropZoneIndex(targetIndex);
      
      return targetIndex;
    }
    
    // 일관성 있는 드롭존 계산
    let bestMatch = { index: 0, position: 'before' as 'before' | 'after', distance: Infinity };
    let cumulativeY = 0;
    
    console.log('🔍 findDropZone touchY:', touchY, 'segments:', segments.length);
    
    // 각 세그먼트 경계에서의 거리 계산
    for (let i = 0; i < segments.length; i++) {
      const segment = segments[i];
      const layout = segmentLayouts.current[segment.id];
      
      // 정확한 높이 계산 (여백 포함)
      const baseHeight = segment.type === 'image' ? 216 : 32;
      const margin = segment.type === 'image' ? 16 : 2; // marginVertical
      const segmentHeight = layout ? layout.height : baseHeight;
      
      const segmentTop = cumulativeY;
      const segmentBottom = cumulativeY + segmentHeight;
      
      // 세그먼트 위쪽 경계 (before)
      const distanceToBefore = Math.abs(touchY - segmentTop);
      if (distanceToBefore < bestMatch.distance) {
        bestMatch = { index: i, position: 'before', distance: distanceToBefore };
      }
      
      // 세그먼트 아래쪽 경계 (after)
      const distanceToAfter = Math.abs(touchY - segmentBottom);
      if (distanceToAfter < bestMatch.distance) {
        bestMatch = { index: i, position: 'after', distance: distanceToAfter };
      }
      
      cumulativeY += segmentHeight;
      
      console.log(`📏 Segment ${i}:`, {
        type: segment.type,
        top: segmentTop,
        bottom: segmentBottom,
        height: segmentHeight,
        distanceToBefore,
        distanceToAfter
      });
    }
    
    // 첫 번째 세그먼트 위에 드롭하는 경우
    if (touchY < 0) {
      bestMatch = { index: 0, position: 'before', distance: Math.abs(touchY) };
    }
    
    // 마지막 세그먼트 아래에 드롭하는 경우
    if (touchY > cumulativeY) {
      bestMatch = { index: segments.length - 1, position: 'after', distance: touchY - cumulativeY };
    }
    
    // 빈 상태 처리
    if (segments.length === 0) {
      bestMatch = { index: 0, position: 'before', distance: 0 };
    }
    
    console.log('🎯 Final bestMatch:', bestMatch, 'touchY:', touchY, 'totalHeight:', cumulativeY);
    
    // 드롭존 업데이트
    setDropZonePosition(bestMatch.position);
    setDropZoneIndex(bestMatch.index);
    
    return bestMatch.index;
  }, [segments, currentEditingSegment]);

  const createImagePanResponder = useCallback((segment: ContentSegment, segmentIndex: number) => {
    // 세그먼트 배열 변경 시 캐시 무효화
    const cacheKey = `${segment.id}_${segmentIndex}_${segments.length}`;
    if (panResponderRefs.current[cacheKey]) {
      return panResponderRefs.current[cacheKey];
    }
    
    const panResponder = PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onStartShouldSetPanResponderCapture: () => true,
      onMoveShouldSetPanResponder: (_, gestureState) => {
        return Math.abs(gestureState.dx) > 3 || Math.abs(gestureState.dy) > 3;
      },
      onMoveShouldSetPanResponderCapture: (_, gestureState) => {
        return Math.abs(gestureState.dx) > 3 || Math.abs(gestureState.dy) > 3;
      },
      onShouldBlockNativeResponder: () => true,

      onPanResponderGrant: (evt) => {
        evt.persist(); // SyntheticEvent 재사용 오류 방지
        
        console.log('🟢 PanResponder Grant - Setting dragging ID:', segment.id);
        setDraggingSegmentId(segment.id);
        
        // 터치 위치 계산: 화면 좌표 기준
        const touchX = evt.nativeEvent.pageX;
        const touchY = evt.nativeEvent.pageY;
        
        // 오버레이 시작 위치 계산
        const overlaySize = 200;
        const startX = touchX - overlaySize / 2;
        const startY = touchY - overlaySize / 2;
        
        // 부모 컴포넌트에 드래그 시작 알림
        const dragStartInfo = {
          imageUrl: segment.imageUrl!,
          startPosition: { x: startX, y: startY },
        };
        dragCallbacksRef.current.onDragStart?.(dragStartInfo);
        
        // 드래그 시작 시 포커스 위치 기반으로 초기 드롭존 설정 (키보드 해제 전에)
        if (currentEditingSegment !== null) {
          findDropZone(0, true); // 포커스 위치 사용
        }
        
        // 키보드 비활성화
        Keyboard.dismiss();
        setCurrentEditingSegment(null);
        
        // 햅틱 피드백
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      },

      onPanResponderMove: (evt, gestureState) => {
        // 부모 컴포넌트에 드래그 이동 알림
        dragCallbacksRef.current.onDragMove?.({ dx: gestureState.dx, dy: gestureState.dy });
        
        // 드롭 존 계산 - 더 정확한 좌표 사용
        const now = Date.now();
        if (now - lastDropZoneUpdate.current > 50) {
          // locationY 사용하여 컴포넌트 내 상대 위치 계산
          const relativeY = evt.nativeEvent.locationY;
          
          console.log('🟡 Drag move coordinates:', {
            locationY: evt.nativeEvent.locationY,
            pageY: evt.nativeEvent.pageY,
            gestureY: gestureState.dy,
            calculatedY: relativeY
          });
          
          findDropZone(relativeY);
          lastDropZoneUpdate.current = now;
        }
      },

      onPanResponderRelease: (evt, gestureState) => {
        console.log('🔴 onPanResponderRelease:', {
          dropZoneIndex,
          segmentIndex,
          draggingSegmentId,
          dropZonePosition,
          segmentsLength: segments.length,
          currentSegmentId: segment.id
        });
        
        // 현재 드래그 중인 세그먼트 ID 보존 (상태 리셋 전에)
        const currentDraggedId = draggingSegmentId || segment.id;
        const currentDropIndex = dropZoneIndex;
        const currentDropPosition = dropZonePosition;
        
        console.log('📊 Using values:', {
          currentDraggedId,
          currentDropIndex,
          currentDropPosition
        });
        
        // 유효한 드롭 조건 확인
        if (currentDropIndex !== null && currentDraggedId) {
          console.log('🟢 Executing drop logic');
          
          const draggedSegmentIndex = segments.findIndex(s => s.id === currentDraggedId);
          
          console.log('📊 Drop details:', {
            draggedSegmentIndex,
            currentDropIndex,
            currentDropPosition,
            segmentIndex,
            isDifferentPosition: currentDropIndex !== draggedSegmentIndex
          });
          
          if (draggedSegmentIndex !== -1 && (currentDropIndex !== draggedSegmentIndex || currentDropPosition !== 'before')) {
            // 현재 segments 배열을 직접 조작
            const reorderedSegments = [...segments];
            
            // 1. 드래그된 요소 제거
            const [draggedSegment] = reorderedSegments.splice(draggedSegmentIndex, 1);
            
            // 2. 삽입 위치 계산
            let insertIndex = currentDropIndex;
            
            // 드래그된 요소가 제거되었으므로 인덱스 조정
            if (currentDropIndex > draggedSegmentIndex) {
              insertIndex = currentDropIndex - 1;
            }
            
            // before/after 처리
            if (currentDropPosition === 'after') {
              insertIndex = Math.min(insertIndex + 1, reorderedSegments.length);
            }
            
            // 3. 새 위치에 삽입
            reorderedSegments.splice(insertIndex, 0, draggedSegment);
            
            // 4. 새로운 content 생성
            const newContent = reorderedSegments.map(seg => seg.content).join('\n');
            
            console.log('📝 Content update:', {
              originalLength: segments.length,
              newLength: reorderedSegments.length,
              newContent: newContent.substring(0, 100) + '...',
              draggedContent: draggedSegment.content.substring(0, 50)
            });
            
            // 5. 상태 업데이트
            onChangeText(newContent);
            
            // 햅틱 피드백
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
          } else {
            console.log('🟠 Same position or invalid drop');
          }
        } else {
          console.log('❌ Drop conditions not met:', {
            hasDropIndex: currentDropIndex !== null,
            hasDraggedId: !!currentDraggedId
          });
        }
        
        // 부모 컴포넌트에 드래그 종료 알림
        dragCallbacksRef.current.onDragEnd?.();
        
        // 상태 리셋
        setDropZoneIndex(null);
        setDraggingSegmentId(null);
      },
    });
    
    // 캐시에 저장
    panResponderRefs.current[cacheKey] = panResponder;
    return panResponder;
  }, [segments, findDropZone]); // segments 의존성 추가


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
    const filteredSegments = newSegments.filter(segment => {
      if (segment.type === 'image') return true;
      if (segment.type === 'text' && segment.content.trim().length > 0) return true;
      return false;
    });
    
    // 빈 상태면 기본 빈 텍스트 세그먼트 하나만 유지
    if (filteredSegments.length === 0 || filteredSegments.every(s => s.type === 'image')) {
      filteredSegments.push({
        id: `text_${Date.now()}`,
        type: 'text',
        content: '',
        startIndex: 0,
        endIndex: 0,
      });
    }
    
    const newContent = filteredSegments
      .map(segment => segment.content)
      .join('\n');
    
    onChangeText(newContent);
  };

  const renderSegment = useCallback((segment: ContentSegment, index: number) => {
    const isDropZone = dropZoneIndex === index;
    const isDragging = draggingSegmentId === segment.id;
    
    // 드롭존 렌더링 디버그
    if (draggingSegmentId) {
    }
    
    
    if (segment.type === 'image' && segment.imageUrl) {
      const panResponder = createImagePanResponder(segment, index);
      
      return (
        <View key={segment.id} style={{ position: 'relative' }}>
          {/* 드롭 존 표시 선 - before */}
          {isDropZone && draggingSegmentId && draggingSegmentId !== segment.id && dropZonePosition === 'before' && (
            <View style={[
              styles.dropZoneLine,
              {
                top: -2,
                zIndex: 1000
              }
            ]} />
          )}
          
          <View 
            ref={(ref: View | null) => { segmentRefs.current[segment.id] = ref; }}
            style={[
              styles.imageBlock,
              // 노션 스타일: 기존 이미지는 그대로 유지 (애니메이션 제거)
            ]}
            onLayout={(event) => {
              const layout = event.nativeEvent.layout;
              segmentLayouts.current[segment.id] = layout;
            }}
            {...panResponder.panHandlers}
          >
            <TouchableOpacity
              onPress={() => handleImagePress(segment)}
              activeOpacity={0.8}
              disabled={isDragging}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
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
          </View>
          
          {/* 드롭 존 표시 선 - after */}
          {isDropZone && draggingSegmentId && draggingSegmentId !== segment.id && dropZonePosition === 'after' && (
            <View style={[
              styles.dropZoneLine,
              {
                bottom: -2,
                zIndex: 1000
              }
            ]} />
          )}
        </View>
      );
    }

    // 텍스트 세그먼트
    const isEditing = currentEditingSegment === index;
    
    return (
      <View key={segment.id} style={{ position: 'relative' }}>
        {/* before 드롭 존 표시 선 */}
        {isDropZone && draggingSegmentId && dropZonePosition === 'before' && (
          <View style={[
            styles.dropZoneLine,
            {
              top: -2,
              zIndex: 1000
            }
          ]} />
        )}
        
        <View 
          ref={(ref: View | null) => { segmentRefs.current[segment.id] = ref; }}
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
                  const newContent = newSegments.map(seg => seg.content).join('\n');
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
              textAlignVertical="top"
              autoFocus
              placeholder=""
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
        {isDropZone && draggingSegmentId && dropZonePosition === 'after' && (
          <View style={[
            styles.dropZoneLine,
            {
              bottom: -2,
              zIndex: 1000
            }
          ]} />
        )}
      </View>
    );
  }, [
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
  ]);

  return (
    <View style={[styles.container, style]}>
      <View style={styles.editorScroll}>
        <View style={styles.content}>
          {segments.map((segment, index) => renderSegment(segment, index))}
        </View>
      </View>
      
      {/* 드래그 오버레이는 부모 컴포넌트에서 렌더링 */}
    </View>
  );
};
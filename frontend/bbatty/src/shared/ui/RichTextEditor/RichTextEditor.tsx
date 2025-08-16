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
    
    // 먼저 전체 텍스트를 이미지와 비이미지로 분할
    const parts = text.split(/(!\[image\]\([^)]+\))/);
    
    for (const part of parts) {
      if (!part) continue;
      
      const imageMatch = part.match(/!\[image\]\(([^)]+)\)/);
      if (imageMatch) {
        // 이미지 세그먼트
        segments.push({
          id: `image_${segmentId++}`,
          type: 'image',
          content: part,
          imageUrl: imageMatch[1],
          startIndex: currentIndex,
          endIndex: currentIndex + part.length,
        });
      } else {
        // 텍스트 부분을 줄별로 나누어 세그먼트 생성 (드래그앤드롭을 위해)
        const lines = part.split('\n');
        for (let i = 0; i < lines.length; i++) {
          const line = lines[i];
          
          // 모든 라인을 세그먼트로 생성 (드롭존 표시를 위해)
          // 연속된 빈 줄 필터링은 실제 content 재구성 시에만 적용
          segments.push({
            id: `text_${segmentId++}`,
            type: 'text',
            content: line,
            startIndex: currentIndex,
            endIndex: currentIndex + line.length,
          });
          
          currentIndex += line.length;
          
          // 마지막 줄이 아니면 줄바꿈 문자 길이 추가
          if (i < lines.length - 1) {
            currentIndex += 1; // \n
          }
        }
        continue; // currentIndex는 위에서 처리했으므로 아래 증가 건너뛰기
      }
      
      currentIndex += part.length;
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
    

    // 안전한 세그먼트 기반 교체: 전체 세그먼트 배열을 재구성
    const newSegments = [...segments];
    
    if (newText.includes('\n')) {
      // Enter 키로 줄바꿈이 발생한 경우: 더 자연스러운 처리
      const lines = newText.split('\n');
      const currentContent = segment.content;
      
      // 현재 세그먼트의 내용과 비교해서 실제로 줄바꿈이 추가되었는지 확인
      if (lines.length > 1 && !currentContent.includes('\n')) {
        // 새로운 줄바꿈이 추가된 경우에만 분할 처리
        const firstLine = lines[0];
        const remainingLines = lines.slice(1);
        
        // 현재 세그먼트는 첫 번째 줄로 업데이트
        newSegments[segmentIndex] = { ...segment, content: firstLine };
        
        // 나머지 줄들을 새로운 세그먼트로 생성
        const newTextSegments = remainingLines.map((line, index) => ({
          id: `text_${Date.now()}_${index}`,
          type: 'text' as const,
          content: line,
          startIndex: 0,
          endIndex: 0,
        }));
        
        // 새 세그먼트들을 현재 위치 다음에 삽입
        newSegments.splice(segmentIndex + 1, 0, ...newTextSegments);
        
        const updatedContent = newSegments.map(s => s.content).join('\n');
        
        // 자연스러운 업데이트를 위해 즉시 처리
        onChangeText(updatedContent);
        
        // 포커스를 새로 생성된 다음 줄로 이동
        setTimeout(() => {
          setCurrentEditingSegment(segmentIndex + 1);
        }, 10); // 더 빠른 포커스 이동
      } else {
        // 기존 줄바꿈이 있는 텍스트의 수정인 경우 일반 처리
        newSegments[segmentIndex] = { ...segment, content: newText };
        const updatedContent = newSegments.map(s => s.content).join('\n');
        onChangeText(updatedContent);
      }
    } else {
      // 일반적인 텍스트 교체
      newSegments[segmentIndex] = { ...segment, content: newText };
      
      // 빈 세그먼트 정리: 연속된 빈 텍스트 세그먼트 제거
      const cleanedSegments = newSegments.filter((seg, index) => {
        if (seg.type === 'image') return true;
        if (seg.type === 'text' && seg.content === '') {
          const nextSeg = newSegments[index + 1];
          const prevSeg = newSegments[index - 1];
          
          // 첫 번째 세그먼트거나 마지막 세그먼트인 경우 유지
          if (index === 0 || index === newSegments.length - 1) return true;
          
          // 앞뒤가 모두 텍스트이고 비어있지 않은 경우에만 빈 세그먼트 유지 (드롭존용)
          // 연속된 빈 세그먼트는 제거
          if (nextSeg && nextSeg.type === 'text' && nextSeg.content === '') return false;
          if (prevSeg && prevSeg.type === 'text' && prevSeg.content === '') return false;
          
          return true;
        }
        return true;
      });
      
      // 빈 상태면 기본 빈 텍스트 세그먼트 하나만 유지
      if (cleanedSegments.length === 0 || cleanedSegments.every(s => s.type === 'image')) {
        cleanedSegments.push({
          id: `text_${Date.now()}`,
          type: 'text',
          content: '',
          startIndex: 0,
          endIndex: 0,
        });
      }
      
      const updatedContent = cleanedSegments.map(s => s.content).join('\n');
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
    // 포커스 위치 기반 드롭존 계산 옵션 추가
    if (useFocusPosition && currentEditingSegment !== null) {
      // 현재 편집 중인 세그먼트 위치에 드롭존 표시
      const targetIndex = currentEditingSegment;
      const dropPosition: 'before' | 'after' = 'before'; // 항상 텍스트 위에 표시
      
      if (dropZoneIndex !== targetIndex || dropZonePosition !== dropPosition) {
        if (dropZoneUpdateTimeout.current) {
          clearTimeout(dropZoneUpdateTimeout.current);
        }
        
        dropZoneUpdateTimeout.current = setTimeout(() => {
          setDropZonePosition(dropPosition);
          setDropZoneIndex(targetIndex);
        }, 50);
      }
      
      return targetIndex;
    }
    
    // 누적 높이 기반 드롭존 계산
    let bestMatch = { index: 0, position: 'before' as 'before' | 'after', distance: Infinity };
    let accumulatedY = 0;
    
    
    for (let i = 0; i < segments.length; i++) {
      const segment = segments[i];
      const layout = segmentLayouts.current[segment.id];
      
      let segmentHeight: number;
      if (layout) {
        segmentHeight = layout.height;
      } else {
        // 레이아웃 정보가 없는 경우 예상 높이 사용
        segmentHeight = segment.type === 'image' ? 210 : 30;
      }
      
      const segmentTop = accumulatedY;
      const segmentBottom = accumulatedY + segmentHeight;
      const segmentCenter = accumulatedY + segmentHeight / 2;
      
      // 터치 위치와 세그먼트의 거리 계산
      let distance: number;
      let position: 'before' | 'after';
      
      if (touchY < segmentTop) {
        distance = segmentTop - touchY;
        position = 'before';
      } else if (touchY > segmentBottom) {
        distance = touchY - segmentBottom;
        position = 'after';
      } else {
        distance = 0; // 세그먼트 내부
        position = touchY < segmentCenter ? 'before' : 'after';
      }
      
      
      // 가장 가까운 세그먼트 찾기
      if (distance < bestMatch.distance) {
        bestMatch = { index: i, position, distance };
      }
      
      // 다음 세그먼트를 위한 누적 높이 업데이트
      accumulatedY += segmentHeight;
    }
    
    // 빈 상태일 때 처리
    if (segments.length === 0) {
      bestMatch = { index: 0, position: 'before', distance: 0 };
    }
    
    
    // 드롭존 상태가 실제로 변경된 경우에만 업데이트
    if (dropZoneIndex !== bestMatch.index || dropZonePosition !== bestMatch.position) {
      if (dropZoneUpdateTimeout.current) {
        clearTimeout(dropZoneUpdateTimeout.current);
      }
      
      dropZoneUpdateTimeout.current = setTimeout(() => {
        setDropZonePosition(bestMatch.position);
        setDropZoneIndex(bestMatch.index);
      }, 30); // 더 빠른 응답성을 위해 30ms로 감소
    }
    
    return bestMatch.index;
  }, [segments, currentEditingSegment, dropZoneIndex, dropZonePosition]);

  const createImagePanResponder = useCallback((segment: ContentSegment, segmentIndex: number) => {
    // 세그먼트 배열 변경 시 캐시 무효화
    const cacheKey = `${segment.id}_${segmentIndex}_${segments.length}`;
    if (panResponderRefs.current[cacheKey]) {
      return panResponderRefs.current[cacheKey];
    }
    
    const panResponder = PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: (_, gestureState) => {
        return Math.abs(gestureState.dx) > 5 || Math.abs(gestureState.dy) > 5;
      },

      onPanResponderGrant: (evt) => {
        evt.persist(); // SyntheticEvent 재사용 오류 방지
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
        
        // 드롭 존 계산 (throttle)
        const now = Date.now();
        if (now - lastDropZoneUpdate.current > 100) {
          const relativeY = evt.nativeEvent.locationY; // scrollPosition 제거
          findDropZone(relativeY);
          lastDropZoneUpdate.current = now;
        }
      },

      onPanResponderRelease: (_, gestureState) => {
        if (dropZoneIndex !== null && dropZoneIndex !== segmentIndex && draggingSegmentId) {
          // 드래그된 이미지 정보 찾기
          const draggedSegment = segments.find(s => s.id === draggingSegmentId);
          if (draggedSegment && draggedSegment.imageUrl) {
            const imageMarkdown = `![image](${draggedSegment.imageUrl})`;
            
            // 1. 기존 이미지 제거
            const newContent = value.replace(imageMarkdown, '').replace(/\n\n+/g, '\n').trim();
            
            // 2. 새 위치에 이미지 삽입
            const newSegments = parseContent(newContent);
            let insertIndex = dropZoneIndex;
            if (dropZonePosition === 'after') {
              insertIndex = Math.min(dropZoneIndex + 1, newSegments.length);
            }
            
            const beforeSegments = newSegments.slice(0, insertIndex);
            const afterSegments = newSegments.slice(insertIndex);
            
            const newImageSegment: ContentSegment = {
              id: `image_${Date.now()}`,
              type: 'image',
              content: imageMarkdown,
              imageUrl: draggedSegment.imageUrl,
              startIndex: 0,
              endIndex: 0,
            };
            
            const finalSegments = [...beforeSegments, newImageSegment, ...afterSegments];
            const finalContent = finalSegments
              .map(seg => seg.content)
              .filter(content => content.length > 0)
              .join('\n');
            
            onChangeText(finalContent);
          }
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
            <View style={[styles.dropZoneLine, { top: -2 }]} />
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
            <View style={[styles.dropZoneLine, { bottom: -2 }]} />
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
          <View style={[styles.dropZoneLine, { top: -2 }]} />
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
                // 빈 텍스트 블록에서만 백스페이스로 병합 처리
                if (e.nativeEvent.key === 'Backspace' && segment.content === '' && index > 0) {
                  // 현재 빈 세그먼트 삭제
                  const newSegments = segments.filter((_, i) => i !== index);
                  const newContent = newSegments.map(seg => seg.content).join('\n');
                  onChangeText(newContent);
                  
                  // 이전 텍스트 블록으로 포커스 이동
                  const prevTextSegmentIndex = index - 1;
                  if (prevTextSegmentIndex >= 0 && segments[prevTextSegmentIndex].type === 'text') {
                    setTimeout(() => {
                      setCurrentEditingSegment(prevTextSegmentIndex);
                    }, 50);
                  }
                }
                // 한 글자 상태에서 백스페이스는 빈 블록으로 만들기 (병합하지 않음)
                // 이는 onChangeText에서 처리됨 - 여기서는 특별한 처리 안함
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
                <Text style={styles.textContent}> </Text> // 빈 공간으로 클릭 영역 확보
              )}
            </TouchableOpacity>
          )}
        </View>
        
        {/* after 드롭 존 표시 선 */}
        {isDropZone && draggingSegmentId && dropZonePosition === 'after' && (
          <View style={[styles.dropZoneLine, { bottom: -2 }]} />
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
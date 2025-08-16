# 게시글 작성 기능 개발 과정

## 📋 프로젝트 개요
야구팬 커뮤니티 플랫폼 BBATTY의 게시글 작성 기능을 담당하여 개발했습니다. 특히 **Notion과 같은 직관적인 리치 텍스트 에디터**를 React Native로 구현하는 것이 핵심 목표였습니다.

## 🎯 주요 기능
- 텍스트와 이미지가 자유롭게 배치되는 리치 텍스트 에디터
- 드래그앤드롭을 통한 이미지 재배치
- 자연스러운 줄바꿈 및 텍스트 편집
- S3 이미지 업로드 및 마크다운 포맷 지원

## 🔍 개발 과정에서 마주한 주요 문제들과 해결 과정

### 1. 리치 텍스트 에디터 아키텍처 설계 문제

**🚨 문제 상황**
```
기존 TextInput으로는 텍스트와 이미지가 섞인 콘텐츠를 자유롭게 편집할 수 없었음
- 이미지 위치 고정
- 텍스트 중간에 이미지 삽입 불가
- 드래그앤드롭 기능 부재
```

**💡 해결 접근**
```typescript
// 세그먼트 기반 콘텐츠 관리 시스템 설계
interface ContentSegment {
  id: string;
  type: 'text' | 'image';
  content: string;
  imageUrl?: string;
  startIndex: number;
  endIndex: number;
}

const parseContent = useCallback((text: string): ContentSegment[] => {
  const segments: ContentSegment[] = [];
  // 텍스트와 이미지 마크다운을 분리하여 개별 세그먼트로 관리
  const parts = text.split(/(!\\[image\\]\\([^)]+\\))/);
  // ...세그먼트 생성 로직
}, []);
```

**✅ 결과**
- 텍스트와 이미지를 독립적인 세그먼트로 관리
- 각 세그먼트별 개별 편집 가능
- 마크다운 포맷 호환성 유지

### 2. 드래그앤드롭 UX 개선 과정

**🚨 초기 문제**
```
사용자가 이미지를 드래그할 때:
- 세그먼트 블록에 제약되어 어색한 움직임
- 드롭 위치 예측 어려움
- 드래그 중 조작감이 부자연스러움
```

**💡 1차 해결 시도: 복잡한 오버레이 시스템**
```typescript
// 드래그 시 원본 이미지 제거 + 오버레이 생성
const [draggingImageInfo, setDraggingImageInfo] = useState<{
  imageUrl: string;
  originalIndex: number;
} | null>(null);

// 터치 위치 추적 오버레이
{draggingImageInfo && (
  <Animated.View style={[styles.dragOverlay, {
    left: dragPosition.x - 100,
    top: dragPosition.y - 100,
  }]}>
    <Image source={{ uri: draggingImageInfo.imageUrl }} />
  </Animated.View>
)}
```

**🚨 발생한 새로운 문제**
```
- 이미지가 화면 밖으로 빠져나가는 심각한 버그
- 복잡한 위치 계산으로 인한 불안정성
- PanResponder와 오버레이 간 이벤트 충돌
```

**💡 최종 해결: 단순화된 반투명 드래그**
```typescript
// 기존 이미지에서 반투명 처리로 아래 텍스트 확인 가능
onPanResponderGrant: (evt) => {
  Animated.parallel([
    Animated.spring(dragScale, {
      toValue: 0.9,
      useNativeDriver: true,
    }),
    Animated.spring(dragOpacity, {
      toValue: 0.6, // 반투명하게 만들어서 아래가 보이게
      useNativeDriver: true,
    }),
  ]).start();
},
```

**✅ 최종 결과**
- 안정적인 드래그앤드롭 동작
- 반투명 효과로 드롭 위치 확인 가능
- 복잡성 제거로 버그 최소화

### 3. 줄바꿈 처리 및 세그먼트 관리 최적화

**🚨 문제 상황**
```
Enter 키 입력 시:
- 버벅임 현상 발생
- 새로운 텍스트 블록으로 포커스 이동 실패
- 시각적 깜빡임
```

**💡 해결 접근**
```typescript
const handleSegmentTextChange = (segmentIndex: number, newText: string) => {
  if (newText.includes('\n')) {
    const lines = newText.split('\n');
    
    // 기존 복잡한 세그먼트 조작 대신 간단한 배열 재구성
    const beforeSegments = segments.slice(0, segmentIndex);
    const afterSegments = segments.slice(segmentIndex + 1);
    
    const newTextSegments = lines.map((line, index) => ({
      id: `text_${Date.now()}_${index}`,
      type: 'text' as const,
      content: line,
      startIndex: 0,
      endIndex: 0,
    }));
    
    const allSegments = [...beforeSegments, ...newTextSegments, ...afterSegments];
    const newContent = allSegments.map(seg => seg.content).join('\n');
    
    onChangeText(newContent);
    
    // setTimeout 대신 requestAnimationFrame으로 부드러운 포커스 이동
    requestAnimationFrame(() => {
      setCurrentEditingSegment(segmentIndex + 1);
    });
  }
};
```

**✅ 결과**
- 자연스러운 줄바꿈 처리
- 버벅임 없는 포커스 이동
- 향상된 타이핑 경험

### 4. 키보드 UX 및 레이아웃 최적화

**🚨 문제 상황**
```
- 키보드 활성화 시 UI 요소가 가려짐
- 이미지 드래그 중 키보드 간섭
- 불필요한 시각적 요소 (빈 텍스트 영역 회색 배경, 안내 문구)
```

**💡 해결 접근**
```typescript
// 드래그 시작 시 키보드 자동 해제
onPanResponderGrant: (evt) => {
  Keyboard.dismiss();
  setCurrentEditingSegment(null);
  // ...
},

// KeyboardAvoidingView 최적화
<KeyboardAvoidingView
  behavior={Platform.select({ ios: 'padding', android: 'height' })}
  keyboardVerticalOffset={Platform.select({ ios: 88, android: 88 })}
>
  {/* 갤러리 툴바를 KeyboardAvoidingView 내부로 이동 */}
  <View style={styles.toolbar}>
    <TouchableOpacity style={styles.imageBtn} onPress={handleImagePick}>
      {/* ... */}
    </TouchableOpacity>
  </View>
</KeyboardAvoidingView>

// 불필요한 UI 요소 제거
emptyTextBlock: {
  minHeight: 24,
  paddingVertical: 2,
  // 회색 배경 제거
  marginVertical: 1,
},
```

**✅ 결과**
- 키보드와 UI 간 충돌 해결
- 드래그 시 자동 키보드 해제
- 깔끔한 UI 디자인

## 🎯 핵심 학습 포인트

### 1. **단순함의 가치**
복잡한 오버레이 시스템보다 기존 시스템을 활용한 단순한 접근이 더 안정적이고 유지보수가 쉬웠습니다.

### 2. **사용자 중심 설계**
기술적 구현보다 실제 사용자 경험을 우선시하여 반투명 드래그, 키보드 자동 해제 등을 구현했습니다.

### 3. **점진적 개선**
한 번에 완벽한 솔루션을 만들려 하지 않고, 문제를 발견할 때마다 점진적으로 개선해나가는 접근을 택했습니다.

### 4. **성능 최적화**
- `useCallback`, `useMemo`를 활용한 리렌더링 최적화
- S3 URL 재사용으로 네트워크 비용 절약
- `requestAnimationFrame`을 활용한 부드러운 애니메이션

## 🛠 기술 스택
- **React Native** - 크로스 플랫폼 모바일 앱 개발
- **TypeScript** - 타입 안정성 확보
- **Animated API** - 드래그앤드롭 애니메이션
- **PanResponder** - 터치 제스처 처리
- **Expo ImagePicker** - 이미지 선택 기능
- **AWS S3** - 이미지 업로드 및 저장

## 📈 성과
- 직관적인 Notion 스타일 에디터 구현
- 안정적인 드래그앤드롭 기능 제공
- 사용자 피드백 기반 지속적 UX 개선
- 재사용 가능한 컴포넌트 아키텍처 구축

---

*이 프로젝트를 통해 복잡한 UI/UX 문제를 단계적으로 해결하고, 사용자 중심의 설계 사고를 기를 수 있었습니다.*
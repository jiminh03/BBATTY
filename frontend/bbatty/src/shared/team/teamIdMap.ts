// 서버가 사용하는 팀 ID와 이름을 매핑합니다.
// 숫자는 서버 스키마에 맞게 꼭 확인/수정하세요.
export const TEAM_ID_BY_NAME: Record<string, number> = {
  '한화 이글스': 1,
  'LG 트윈스': 2,
  '롯데 자이언츠': 3,
  'KT 위즈': 4,
  '삼성 라이온즈': 5,
  'KIA 타이거즈': 6,
  'SSG 랜더스': 7,
  'NC 다이노스': 8,
  '두산 베어스': 9,
  '키움 히어로즈': 10,
};

// 안전 변환 헬퍼
export const nameToServerTeamId = (name: string, fallback?: number) =>
  TEAM_ID_BY_NAME[name] ?? fallback ?? 0;

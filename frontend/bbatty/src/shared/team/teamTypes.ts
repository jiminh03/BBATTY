// shared/types/teamTypes.ts

export interface Team {
  id: number;
  name: string;
  color: string; // 대표 색상 하나 (UI 테마용)
  imagePath: string | number; // 팀 로고 이미지 경로
}

// KBO 팀 데이터
export const TEAMS: Team[] = [
  {
    id: 1,
    name: '한화 이글스',
    color: '#FF6600',
    imagePath: require('../../../assets/images/teams/hanwha.png'),
  },
  {
    id: 2,
    name: 'LG 트윈스',
    color: '#C30452',
    imagePath: require('../../../assets/images/teams/lg.png'),
  },
  {
    id: 3,
    name: '롯데 자이언츠',
    color: '#002955',
    imagePath: require('../../../assets/images/teams/lotte.png'),
  },
  {
    id: 4,
    name: 'KT 위즈',
    color: '#000000',
    imagePath: require('../../../assets/images/teams/kt.png'),
  },
  {
    id: 5,
    name: '삼성 라이온즈',
    color: '#0066B3',
    imagePath: require('../../../assets/images/teams/samsung.png'),
  },
  {
    id: 6,
    name: 'KIA 타이거즈',
    color: '#EA0029',
    imagePath: require('../../../assets/images/teams/kia.png'),
  },
  {
    id: 7,
    name: 'SSG 랜더스',
    color: '#CE0E2D',
    imagePath: require('../../../assets/images/teams/ssg.png'),
  },
  {
    id: 8,
    name: 'NC 다이노스',
    color: '#1D467F',
    imagePath: require('../../../assets/images/teams/nc.png'),
  },
  {
    id: 9,
    name: '두산 베어스',
    color: '#131230',
    imagePath: require('../../../assets/images/teams/doosan.png'),
  },
  {
    id: 10,
    name: '키움 히어로즈',
    color: '#820024',
    imagePath: require('../../../assets/images/teams/kiwoom.png'),
  },
] as const;

// 팀 ID로 팀 정보 찾기
export const findTeamById = (id: number): Team | undefined => {
  return TEAMS.find((team) => team.id === id);
};

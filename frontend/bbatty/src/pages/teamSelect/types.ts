import { TeamName } from '../../shared/styles';

export interface Team {
  id: number;
  key: TeamName;
  name: string;
  logo: string; // 실제로는 require('../assets/logos/lg.png') 같은 형태
}

export const TEAMS: Team[] = [
  { id: 1, key: 'HANWHA', name: '한화', logo: '🦅' },
  { id: 2, key: 'SAMSUNG', name: '삼성', logo: '🦁' },
  { id: 3, key: 'DOOSAN', name: '두산', logo: '🐻' },
  { id: 4, key: 'NC', name: 'NC', logo: '🦖' },
  { id: 5, key: 'LG', name: 'LG', logo: '👹' },
  { id: 6, key: 'KIA', name: 'KIA', logo: '🐯' },
  { id: 7, key: 'KT', name: 'KT', logo: '🧙' },
  { id: 8, key: 'SSG', name: 'SSG', logo: '🦎' },
  { id: 9, key: 'KIWOOM', name: '키움', logo: '🦸' },
  { id: 10, key: 'LOTTE', name: '롯데', logo: '🦆' },
];

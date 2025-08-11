export interface UserBadges {
  userId: number;
  badgeCategories: BadgeCategory[];
}

export interface BadgeCategory {
  category: string;
  displayName: string;
  description: string;
  season: string | null;
  badges: Badge[];
}

export interface Badge {
  badgeType: string;
  description: string;
  acquired: boolean;
  acquiredAt: string | null;
}

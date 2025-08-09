import { useEffect } from 'react';
import { tokenManager } from '../../shared';
import { resetToAuth, resetToMain } from '../../navigation/navigationRefs';

export const useAuth = () => {
  const logout = async () => {
    try {
      await tokenManager.removeToken();
      resetToAuth();
    } catch (error) {
      console.error('로그아웃 실패:', error);
    }
  };

  const checkTokenExpiry = async () => {
    const token = await tokenManager.getToken();
    if (!token) {
      resetToAuth();
      return false;
    }

    return true;
  };

  return {
    logout,
    checkTokenExpiry,
  };
};

import { useNavigation as useRNNavigation } from '@react-navigation/native';
import { RootStackParamList } from '../types';

export function useNavigation() {
  const navigation = useRNNavigation();

  //   const navigateToHome = (gameId: string) => {
  //     navigation.navigate('MainTabs', {
  //       screen: 'HomeStack',
  //     });
  //   };?
}

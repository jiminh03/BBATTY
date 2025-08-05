// import Geolocation from 'react-native-geolocation-service';
// import { Platform, Linking, Alert } from 'react-native';
// import { check, request, PERMISSIONS, RESULTS, Permission } from 'expo-permissions';

// export interface Location {
//   latitude: number;
//   longitude: number;
//   accuracy?: number;
//   timestamp?: number;
// }

// // 야구장 정보
// export const STADIUM_LOCATIONS = {
//   JAMSIL: {
//     name: '잠실야구장',
//     teams: ['LG', 'DOOSAN'],
//     location: {
//       latitude: 37.5121,
//       longitude: 127.0719,
//     },
//     radius: 500, // 반경 500m
//   },
//   GOCHEOK: {
//     name: '고척스카이돔',
//     teams: ['KIWOOM'],
//     location: {
//       latitude: 37.4982,
//       longitude: 126.8673,
//     },
//     radius: 500,
//   },
//   SUWON: {
//     name: '수원KT위즈파크',
//     teams: ['KT'],
//     location: {
//       latitude: 37.2997,
//       longitude: 127.0098,
//     },
//     radius: 500,
//   },
//   INCHEON: {
//     name: 'SSG랜더스필드',
//     teams: ['SSG'],
//     location: {
//       latitude: 37.437,
//       longitude: 126.6933,
//     },
//     radius: 500,
//   },
//   DAEJEON: {
//     name: '한화생명이글스파크',
//     teams: ['HANWHA'],
//     location: {
//       latitude: 36.317,
//       longitude: 127.4291,
//     },
//     radius: 500,
//   },
//   DAEGU: {
//     name: '대구삼성라이온즈파크',
//     teams: ['SAMSUNG'],
//     location: {
//       latitude: 35.8411,
//       longitude: 128.6818,
//     },
//     radius: 500,
//   },
//   BUSAN: {
//     name: '사직야구장',
//     teams: ['LOTTE'],
//     location: {
//       latitude: 35.194,
//       longitude: 129.0615,
//     },
//     radius: 500,
//   },
//   GWANGJU: {
//     name: '광주-기아챔피언스필드',
//     teams: ['KIA'],
//     location: {
//       latitude: 35.1681,
//       longitude: 126.8895,
//     },
//     radius: 500,
//   },
//   CHANGWON: {
//     name: '창원NC파크',
//     teams: ['NC'],
//     location: {
//       latitude: 35.2229,
//       longitude: 128.5821,
//     },
//     radius: 500,
//   },
// } as const;

// export type StadiumName = keyof typeof STADIUM_LOCATIONS;

// // 위치 권한 요청
// export const requestLocationPermission = async (): Promise<boolean> => {
//   try {
//     const permission: Permission = Platform.select({
//       ios: PERMISSIONS.IOS.LOCATION_WHEN_IN_USE,
//       android: PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
//     }) as Permission;

//     const result = await check(permission);

//     switch (result) {
//       case RESULTS.GRANTED:
//         return true;
//       case RESULTS.DENIED:
//         const requestResult = await request(permission);
//         return requestResult === RESULTS.GRANTED;
//       case RESULTS.BLOCKED:
//         Alert.alert('위치 권한 필요', '직관 인증을 위해 위치 권한이 필요합니다. 설정에서 권한을 허용해주세요.', [
//           { text: '취소', style: 'cancel' },
//           { text: '설정으로 이동', onPress: () => Linking.openSettings() },
//         ]);
//         return false;
//       default:
//         return false;
//     }
//   } catch (error) {
//     console.error('위치 권한 요청 실패:', error);
//     return false;
//   }
// };

// // 현재 위치 가져오기
// export const getCurrentLocation = (): Promise<Location> => {
//   return new Promise((resolve, reject) => {
//     Geolocation.getCurrentPosition(
//       (position) => {
//         resolve({
//           latitude: position.coords.latitude,
//           longitude: position.coords.longitude,
//           accuracy: position.coords.accuracy,
//           timestamp: position.timestamp,
//         });
//       },
//       (error) => {
//         console.error('위치 가져오기 실패:', error);
//         reject(error);
//       },
//       {
//         enableHighAccuracy: true,
//         timeout: 15000,
//         maximumAge: 10000,
//         // 추후 iOS 제작 시 showsBackgroundLocationIndicator: false 추가
//       }
//     );
//   });
// };

// // 두 지점 간 거리 계산 (Haversine formula)
// export const calculateDistance = (location1: Location, location2: Location): number => {
//   const R = 6371e3; // 지구 반지름 (미터)
//   const φ1 = (location1.latitude * Math.PI) / 180;
//   const φ2 = (location2.latitude * Math.PI) / 180;
//   const Δφ = ((location2.latitude - location1.latitude) * Math.PI) / 180;
//   const Δλ = ((location2.longitude - location1.longitude) * Math.PI) / 180;

//   const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
//   const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

//   return R * c; // 미터 단위
// };

// // 야구장 근처인지 확인
// export const isNearStadium = (userLocation: Location, stadiumName: StadiumName): boolean => {
//   const stadium = STADIUM_LOCATIONS[stadiumName];
//   const distance = calculateDistance(userLocation, stadium.location);
//   return distance <= stadium.radius;
// };

// // 직관 인증 가능 여부 확인
// export const canVerifyAttendance = async (
//   stadiumName: StadiumName
// ): Promise<{ canVerify: boolean; message: string }> => {
//   try {
//     // 위치 권한 확인
//     const hasPermission = await requestLocationPermission();
//     if (!hasPermission) {
//       return {
//         canVerify: false,
//         message: '위치 권한이 필요합니다',
//       };
//     }

//     // 현재 위치 가져오기
//     const userLocation = await getCurrentLocation();

//     // 야구장 근처인지 확인
//     const isNear = isNearStadium(userLocation, stadiumName);

//     if (isNear) {
//       return {
//         canVerify: true,
//         message: '직관 인증이 가능합니다',
//       };
//     } else {
//       const stadium = STADIUM_LOCATIONS[stadiumName];
//       const distance = calculateDistance(userLocation, stadium.location);
//       return {
//         canVerify: false,
//         message: `야구장에서 ${Math.round(distance)}m 떨어져 있습니다`,
//       };
//     }
//   } catch (error) {
//     return {
//       canVerify: false,
//       message: '위치 확인에 실패했습니다',
//     };
//   }
// };

// // 위치 감시 시작
// export const watchLocation = (
//   onLocationUpdate: (location: Location) => void,
//   onError?: (error: any) => void
// ): number => {
//   return Geolocation.watchPosition(
//     (position) => {
//       onLocationUpdate({
//         latitude: position.coords.latitude,
//         longitude: position.coords.longitude,
//         accuracy: position.coords.accuracy,
//         timestamp: position.timestamp,
//       });
//     },
//     onError,
//     {
//       enableHighAccuracy: true,
//       distanceFilter: 10, // 10미터 이동 시 업데이트
//       interval: 5000, // 5초마다 업데이트
//       fastestInterval: 2000, // 최소 2초 간격
//     }
//   );
// };

// // 위치 감시 중지
// export const stopWatchingLocation = (watchId: number): void => {
//   Geolocation.clearWatch(watchId);
// };

// // 거리 포맷팅
// export const formatDistance = (meters: number): string => {
//   if (meters < 1000) {
//     return `${Math.round(meters)}m`;
//   } else {
//     return `${(meters / 1000).toFixed(1)}km`;
//   }
// };

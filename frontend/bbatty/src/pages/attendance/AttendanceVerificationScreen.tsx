import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import * as Location from 'expo-location';
import MapView, { Marker } from 'react-native-maps';
import type { RootStackParamList } from '../../navigation/types';
import { attendanceApi } from '../../entities/attendance/api/api';
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import { gameApi } from '../../entities/game/api/api';

type NavigationProp = StackNavigationProp<RootStackParamList, 'AttendanceVerification'>;

// 하드코딩된 경기장 위치 (예: 잠실야구장)
const TARGET_LOCATION = {
  latitude: 37.5124,
  longitude: 127.0719,
  name: '잠실야구장',
};

// 테스트용 - 잠실야구장 근처 위치 (100m 이내)
const TEST_LOCATION = {
  latitude: 37.5125,  // 잠실야구장에서 약 11m 떨어진 위치
  longitude: 127.0720,
};

// 직관 인증을 위한 최대 거리 (미터)
const MAX_DISTANCE = 500;

// 원을 그리기 위한 점들 생성
const createCircle = (center: {latitude: number, longitude: number}, radius: number) => {
  const points = [];
  const earthRadius = 6371000; // 지구 반지름 (미터)
  const numPoints = 50; // 원을 구성하는 점의 개수
  
  for (let i = 0; i < numPoints; i++) {
    const angle = (i * 360 / numPoints) * Math.PI / 180;
    const lat = center.latitude + (radius / earthRadius) * (180 / Math.PI) * Math.cos(angle);
    const lon = center.longitude + (radius / earthRadius) * (180 / Math.PI) * Math.sin(angle) / Math.cos(center.latitude * Math.PI / 180);
    points.push({ latitude: lat, longitude: lon });
  }
  return points;
};

export const AttendanceVerificationScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const mapRef = useRef<MapView>(null);
  const { setAttendanceVerified } = useAttendanceStore();
  const [currentLocation, setCurrentLocation] = useState<{latitude: number; longitude: number} | null>(null);
  const [mapRegion, setMapRegion] = useState({
    latitude: TARGET_LOCATION.latitude,
    longitude: TARGET_LOCATION.longitude,
    latitudeDelta: 0.01,
    longitudeDelta: 0.01,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);
  const [mapInitialized, setMapInitialized] = useState(false);

  useEffect(() => {
    getCurrentLocation();
  }, []);


  const getCurrentLocation = async () => {
    try {
      setIsLoading(true);

      // 위치 권한 요청
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('권한 오류', '위치 접근 권한이 필요합니다. 설정에서 권한을 허용해주세요.');
        setIsLoading(false);
        return;
      }

      // 현재 위치 가져오기
      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
        timeInterval: 15000,
        distanceInterval: 10,
      });

      const { latitude, longitude } = location.coords;
      console.log('현재 위치 획득:', { latitude, longitude });
      console.log('목표 위치 (잠실야구장):', TARGET_LOCATION);
      setCurrentLocation({ latitude, longitude });
      
      const newRegion = {
        latitude,
        longitude,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      };
      
      setMapRegion(newRegion);
      
      // 지도를 새로운 지역으로 애니메이션
      setTimeout(() => {
        if (mapRef.current) {
          mapRef.current.animateToRegion(newRegion, 1000);
        }
      }, 500);
      
      setIsLoading(false);
    } catch (error) {
      console.error('위치 정보 오류:', error);
      Alert.alert('위치 오류', '현재 위치를 가져올 수 없습니다. GPS를 활성화해주세요.');
      setIsLoading(false);
    }
  };

  // 두 지점 간의 거리 계산 (Haversine formula)
  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
    console.log('거리 계산 입력값:', { lat1, lon1, lat2, lon2 });
    
    const R = 6371000; // 지구 반지름을 미터로 직접 설정
    const φ1 = lat1 * Math.PI / 180; // φ, λ는 라디안으로 변환
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;

    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    const distance = R * c; // 미터 단위
    console.log('계산된 거리 (미터):', distance);
    return distance;
  };

  const handleAttendanceVerification = async () => {
    if (!currentLocation) {
      Alert.alert('오류', '현재 위치를 확인할 수 없습니다.');
      return;
    }

    setIsVerifying(true);

    try {
      console.log('현재 위치:', currentLocation);
      
      // API 호출로 직관 인증 (서버에서 거리 검증)
      const response = await attendanceApi.verifyAttendance({
        latitude: currentLocation.latitude,
        longitude: currentLocation.longitude,
      });

      console.log('🎯 직관 인증 API 응답:', response);

      if (response.status === 'SUCCESS') {
        // 오늘의 게임 정보 가져오기
        let gameInfo = null;
        try {
          const gameResponse = await gameApi.getTodayGame();
          if (gameResponse.status === 'SUCCESS') {
            gameInfo = gameResponse.data;
          }
        } catch (error) {
          console.error('게임 정보 로드 실패:', error);
        }

        // 상태 저장
        setAttendanceVerified(true, gameInfo);

        setIsVerifying(false);
        Alert.alert(
          '직관 인증 성공! 🎉',
          '직관 인증이 완료되었습니다!',
          [
            {
              text: '확인',
              onPress: () => {
                // 홈으로 돌아가기 (MainTabs의 HomeStack으로 이동)
                navigation.navigate('MainTabs', { screen: 'HomeStack', params: { screen: 'Home' } });
              },
            },
          ]
        );
      } else {
        setIsVerifying(false);
        Alert.alert(
          '직관 인증 실패 😔',
          response.message || '인증에 실패했습니다. 다시 시도해주세요.',
          [
            {
              text: '위치 새로고침',
              onPress: getCurrentLocation,
            },
            {
              text: '확인',
              style: 'cancel',
            },
          ]
        );
      }
    } catch (error) {
      console.error('직관 인증 중 오류:', error);
      setIsVerifying(false);
      Alert.alert(
        '오류 발생',
        '네트워크 오류가 발생했습니다. 다시 시도해주세요.',
        [
          {
            text: '확인',
            style: 'cancel',
          },
        ]
      );
    }
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007AFF" />
          <Text style={styles.loadingText}>현재 위치를 확인하는 중...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backButton}>← 뒤로가기</Text>
        </TouchableOpacity>
        <Text style={styles.title}>직관 인증</Text>
        <View style={styles.placeholder} />
      </View>

      <View style={styles.mapContainer}>
        <MapView
          ref={mapRef}
          style={styles.map}
          initialRegion={{
            latitude: TARGET_LOCATION.latitude,
            longitude: TARGET_LOCATION.longitude,
            latitudeDelta: 0.01,
            longitudeDelta: 0.01,
          }}
          showsUserLocation={false}
          showsMyLocationButton={true}
          mapType="standard"
          onLayout={() => {
            if (!mapInitialized) {
              setMapInitialized(true);
            }
          }}
        >
          <Marker
            coordinate={{
              latitude: TARGET_LOCATION.latitude,
              longitude: TARGET_LOCATION.longitude,
            }}
            title={TARGET_LOCATION.name}
            description="직관 인증 목표 위치"
            pinColor="red"
          />
          
          {currentLocation && (
            <Marker
              coordinate={{
                latitude: currentLocation.latitude,
                longitude: currentLocation.longitude,
              }}
              title="현재 위치"
              description="현재 있는 위치"
              pinColor="blue"
            />
          )}
        </MapView>
      </View>

      <View style={styles.infoContainer}>
        <Text style={styles.infoTitle}>📍 인증 정보</Text>
        <Text style={styles.infoText}>목표 위치: {TARGET_LOCATION.name}</Text>
        <Text style={styles.infoText}>인증 범위: {MAX_DISTANCE}m 이내</Text>
        {currentLocation && (
          <>
            <Text style={styles.infoText}>
              현재 거리: {Math.round(
                calculateDistance(
                  currentLocation.latitude,
                  currentLocation.longitude,
                  TARGET_LOCATION.latitude,
                  TARGET_LOCATION.longitude
                )
              )}m
            </Text>
            <Text style={styles.currentLocationText}>
              현재 위치: {currentLocation.latitude.toFixed(4)}, {currentLocation.longitude.toFixed(4)}
            </Text>
          </>
        )}
      </View>

      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={styles.refreshButton}
          onPress={getCurrentLocation}
          disabled={isLoading}
        >
          <Text style={styles.refreshButtonText}>📍 위치 새로고침</Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={styles.testButton}
          onPress={() => {
            console.log('테스트 위치로 설정');
            console.log('TEST_LOCATION:', TEST_LOCATION);
            setCurrentLocation({ latitude: TEST_LOCATION.latitude, longitude: TEST_LOCATION.longitude });
            
            const newRegion = {
              latitude: TEST_LOCATION.latitude,
              longitude: TEST_LOCATION.longitude,
              latitudeDelta: 0.01,
              longitudeDelta: 0.01,
            };
            
            setMapRegion(newRegion);
            
            // 지도를 테스트 위치로 애니메이션
            if (mapRef.current) {
              mapRef.current.animateToRegion(newRegion, 1000);
            }
            
            console.log('mapRegion 업데이트됨:', TEST_LOCATION);
          }}
        >
          <Text style={styles.testButtonText}>🧪 테스트 위치 (야구장 근처)</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.verifyButton, isVerifying && styles.verifyButtonDisabled]}
          onPress={handleAttendanceVerification}
          disabled={isVerifying || !currentLocation}
        >
          {isVerifying ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.verifyButtonText}>🎯 직관 인증하기</Text>
          )}
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  backButton: {
    color: '#007AFF',
    fontSize: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  placeholder: {
    width: 60,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  mapContainer: {
    flex: 1,
    margin: 16,
    borderRadius: 12,
    overflow: 'hidden',
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    height: 400,
  },
  map: {
    flex: 1,
  },
  mapPlaceholder: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
  },
  mapPlaceholderText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  infoContainer: {
    backgroundColor: '#fff',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  infoTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  infoText: {
    fontSize: 14,
    color: '#666',
    marginBottom: 6,
  },
  currentLocationText: {
    fontSize: 12,
    color: '#999',
    marginTop: 4,
  },
  buttonContainer: {
    paddingHorizontal: 16,
    paddingBottom: 16,
    gap: 12,
  },
  refreshButton: {
    backgroundColor: '#6c757d',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  refreshButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  testButton: {
    backgroundColor: '#28a745',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  testButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  verifyButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: 'center',
  },
  verifyButtonDisabled: {
    backgroundColor: '#ccc',
  },
  verifyButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
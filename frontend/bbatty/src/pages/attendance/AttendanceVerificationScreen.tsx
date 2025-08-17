import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import * as Location from 'expo-location';
import type { RootStackParamList } from '../../navigation/types';
import { attendanceApi } from '../../entities/attendance/api/api';
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import { gameApi } from '../../entities/game/api/api';

type NavigationProp = StackNavigationProp<RootStackParamList, 'AttendanceVerification'>;

// KBO 야구장 테스트 데이터
const STADIUMS = [
  { name: '잠실야구장', latitude: 37.5124, longitude: 127.0719, teams: 'LG/두산' },
  { name: '고척스카이돔', latitude: 37.4982, longitude: 126.8672, teams: '키움' },
  { name: '수원KT위즈파크', latitude: 37.2997, longitude: 127.0097, teams: 'KT' },
  { name: '인천SSG랜더스필드', latitude: 37.4370, longitude: 126.6934, teams: 'SSG' },
  { name: '대전한화생명볼파크', latitude: 36.3171, longitude: 127.4290, teams: '한화' },
  { name: '광주기아챔피언스필드', latitude: 35.1681, longitude: 126.8887, teams: 'KIA' },
  { name: '대구삼성라이온즈파크', latitude: 35.8408, longitude: 128.6819, teams: '삼성' },
  { name: '부산사직야구장', latitude: 35.1940, longitude: 129.0617, teams: '롯데' },
  { name: '창원NC파크', latitude: 35.2225, longitude: 128.5823, teams: 'NC' },
];

export const AttendanceVerificationScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const { setAttendanceVerified } = useAttendanceStore();
  const mapRef = useRef<MapView>(null);
  const [currentLocation, setCurrentLocation] = useState<{latitude: number; longitude: number} | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);
  const [todayGame, setTodayGame] = useState<any>(null);
  const [showStadiumTest, setShowStadiumTest] = useState(false);
  const [selectedStadium, setSelectedStadium] = useState(STADIUMS[0]);

  useEffect(() => {
    loadTodayGameAndLocation();
  }, []);

  const loadTodayGameAndLocation = async () => {
    setIsLoading(true);
    
    try {
      // 오늘 경기 정보 가져오기
      const gameResponse = await gameApi.getTodayGame();
      if (gameResponse.status === 'SUCCESS' && gameResponse.data) {
        setTodayGame(gameResponse.data);
      }

      // 현재 위치 가져오기
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('권한 오류', '위치 접근 권한이 필요합니다.');
        return;
      }

      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });

      setCurrentLocation({
        latitude: location.coords.latitude,
        longitude: location.coords.longitude,
      });

    } catch (error) {
      console.error('초기 데이터 로딩 실패:', error);
      Alert.alert('오류', '데이터를 불러오는 중 문제가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAttendanceVerification = async () => {
    if (!currentLocation) {
      Alert.alert('오류', '현재 위치를 확인할 수 없습니다.');
      return;
    }

    setIsVerifying(true);

    try {
      const requestData = {
        latitude: currentLocation.latitude,
        longitude: currentLocation.longitude,
      };
      
      const response = await attendanceApi.verifyAttendance(requestData);

      if (response.status === 'SUCCESS') {
        const gameInfo = response.data?.gameInfo ? {
          gameId: response.data.gameInfo.gameId,
          awayTeamName: response.data.gameInfo.awayTeam,
          homeTeamName: response.data.gameInfo.homeTeam,
          dateTime: response.data.gameInfo.gameDateTime,
          stadium: response.data.stadiumInfo?.stadiumName || '야구장',
        } : null;

        setAttendanceVerified(true, gameInfo);

        Alert.alert(
          '직관 인증 성공! 🎉',
          '직관 인증이 완료되었습니다!',
          [
            {
              text: '확인',
              onPress: () => {
                navigation.navigate('MainTabs', { screen: 'HomeStack', params: { screen: 'Home' } });
              },
            },
          ]
        );
      } else {
        Alert.alert(
          '직관 인증 실패',
          response.message || '인증에 실패했습니다.',
          [
            {
              text: '위치 새로고침',
              onPress: loadTodayGameAndLocation,
            },
            {
              text: '확인',
              style: 'cancel',
            },
          ]
        );
      }
    } catch (error: any) {
      console.error('직관 인증 중 오류:', error);
      Alert.alert('직관 인증 실패', '서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsVerifying(false);
    }
  };

  const setTestLocation = (stadium: any) => {
    // 구장 근처 100m 이내 랜덤 위치 설정
    const randomOffset = () => (Math.random() - 0.5) * 0.002; // 약 100m 내외
    const testLocation = {
      latitude: stadium.latitude + randomOffset(),
      longitude: stadium.longitude + randomOffset(),
    };
    
    setCurrentLocation(testLocation);
    setSelectedStadium(stadium);
    
    // 지도를 해당 위치로 이동
    if (mapRef.current) {
      mapRef.current.animateToRegion({
        latitude: testLocation.latitude,
        longitude: testLocation.longitude,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      }, 1000);
    }
    
    Alert.alert('테스트 위치 설정', `${stadium.name} 근처로 위치가 설정되었습니다.`);
    setShowStadiumTest(false);
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007AFF" />
          <Text style={styles.loadingText}>데이터를 불러오는 중...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.content}>
        {todayGame && (
          <View style={styles.gameInfo}>
            <Text style={styles.gameTitle}>🏟️ 오늘의 경기</Text>
            <Text style={styles.gameDetails}>
              {todayGame.awayTeamName} vs {todayGame.homeTeamName}
            </Text>
            <Text style={styles.gameTime}>
              {new Date(todayGame.dateTime).toLocaleString('ko-KR')}
            </Text>
            <Text style={styles.gameStadium}>{todayGame.stadium}</Text>
          </View>
        )}

        {/* 지도 */}
        <View style={styles.mapContainer}>
          <MapView
            ref={mapRef}
            style={styles.map}
            initialRegion={{
              latitude: selectedStadium.latitude,
              longitude: selectedStadium.longitude,
              latitudeDelta: 0.01,
              longitudeDelta: 0.01,
            }}
            showsUserLocation={false}
            showsMyLocationButton={false}
            mapType="standard"
          >
            {/* 구장 마커 */}
            <Marker
              coordinate={{
                latitude: selectedStadium.latitude,
                longitude: selectedStadium.longitude,
              }}
              title={selectedStadium.name}
              description={`${selectedStadium.teams} 홈구장`}
              pinColor="red"
            />
            
            {/* 현재 위치 마커 */}
            {currentLocation && (
              <Marker
                coordinate={{
                  latitude: currentLocation.latitude,
                  longitude: currentLocation.longitude,
                }}
                title="현재 위치"
                description="GPS 위치"
                pinColor="blue"
              />
            )}
          </MapView>
        </View>

        <View style={styles.locationInfo}>
          <Text style={styles.locationTitle}>📍 현재 위치</Text>
          {currentLocation ? (
            <Text style={styles.locationText}>
              위도: {currentLocation.latitude.toFixed(6)}{'\n'}
              경도: {currentLocation.longitude.toFixed(6)}
            </Text>
          ) : (
            <Text style={styles.locationError}>위치를 확인할 수 없습니다</Text>
          )}
        </View>

        <View style={styles.infoBox}>
          <Text style={styles.infoTitle}>ℹ️ 인증 안내</Text>
          <Text style={styles.infoText}>
            • 경기장 150m 이내에서만 인증 가능합니다{'\n'}
            • 경기 시작 2시간 전부터 인증 가능합니다{'\n'}
            • 하루에 한 번만 인증할 수 있습니다
          </Text>
        </View>
        
        {/* 구장 테스트 버튼들 */}
        {showStadiumTest && (
          <View style={styles.stadiumTestContainer}>
            <Text style={styles.stadiumTestTitle}>🏟️ 테스트용 구장 위치 설정</Text>
            {STADIUMS.map((stadium, index) => (
              <TouchableOpacity
                key={index}
                style={styles.stadiumTestButton}
                onPress={() => setTestLocation(stadium)}
              >
                <Text style={styles.stadiumTestButtonText}>
                  {stadium.name} ({stadium.teams})
                </Text>
              </TouchableOpacity>
            ))}
            <TouchableOpacity
              style={styles.stadiumTestCloseButton}
              onPress={() => setShowStadiumTest(false)}
            >
              <Text style={styles.stadiumTestCloseButtonText}>닫기</Text>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>

      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={styles.refreshButton}
          onPress={loadTodayGameAndLocation}
          disabled={isLoading}
        >
          <Text style={styles.refreshButtonText}>🔄 새로고침</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.testButton}
          onPress={() => setShowStadiumTest(!showStadiumTest)}
        >
          <Text style={styles.testButtonText}>
            {showStadiumTest ? '테스트 메뉴 닫기' : '🧪 구장 위치 테스트'}
          </Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[styles.verifyButton, (isVerifying || !currentLocation) && styles.verifyButtonDisabled]}
          onPress={handleAttendanceVerification}
          disabled={isVerifying || !currentLocation}
        >
          {isVerifying ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.verifyButtonText}>직관 인증하기</Text>
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
  content: {
    flex: 1,
    padding: 16,
  },
  mapContainer: {
    height: 250,
    marginBottom: 16,
    borderRadius: 12,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  map: {
    flex: 1,
  },
  gameInfo: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  gameTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  gameDetails: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#007AFF',
    textAlign: 'center',
    marginBottom: 8,
  },
  gameTime: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 4,
  },
  gameStadium: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
  },
  locationInfo: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  locationTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  locationText: {
    fontSize: 14,
    color: '#666',
    fontFamily: 'monospace',
  },
  locationError: {
    fontSize: 14,
    color: '#ff6b6b',
  },
  infoBox: {
    backgroundColor: '#e3f2fd',
    padding: 16,
    borderRadius: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#2196f3',
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1976d2',
    marginBottom: 8,
  },
  infoText: {
    fontSize: 14,
    color: '#1565c0',
    lineHeight: 20,
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
  // 구장 테스트 스타일
  stadiumTestContainer: {
    backgroundColor: '#fff',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  stadiumTestTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
    textAlign: 'center',
  },
  stadiumTestButton: {
    backgroundColor: '#f8f9fa',
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#e9ecef',
  },
  stadiumTestButtonText: {
    color: '#495057',
    fontSize: 14,
    textAlign: 'center',
  },
  stadiumTestCloseButton: {
    backgroundColor: '#dc3545',
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 8,
    marginTop: 8,
  },
  stadiumTestCloseButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    textAlign: 'center',
  },
});
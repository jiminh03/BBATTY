const AdvancedChatPerformanceTest = require('./advanced-chat-performance-test.js');

/**
 * 🔥🔥🔥 비스트 모드: 채팅 서버 한계 도전
 * ⚠️ WARNING: 시스템 리소스를 극한까지 사용합니다!
 */
async function runBeastModeTests() {
    const beastModeConfigs = [
        {
            name: '🌪️  토네이도 테스트 (2000 연결 × 5 msg/s)',
            config: {
                maxConnections: 2000,
                messageRate: 5,
                testDuration: 120000, // 2분
                rampUpDuration: 30000  // 30초
            }
        },
        {
            name: '🌊 쓰나미 테스트 (1000 연결 × 25 msg/s)',
            config: {
                maxConnections: 1000,
                messageRate: 25,
                testDuration: 90000,   // 1.5분
                rampUpDuration: 20000  // 20초
            }
        },
        {
            name: '🔥 헬파이어 테스트 (5000 연결 × 3 msg/s)',
            config: {
                maxConnections: 5000,
                messageRate: 3,
                testDuration: 180000,  // 3분
                rampUpDuration: 60000  // 1분
            }
        },
        {
            name: '💀 데스스타 테스트 (3000 연결 × 10 msg/s)',
            config: {
                maxConnections: 3000,
                messageRate: 10,
                testDuration: 150000,  // 2.5분
                rampUpDuration: 45000  // 45초
            }
        }
    ];
    
    console.log('💀💀💀 비스트 모드 테스트 시작! 💀💀💀');
    console.log('⚠️  경고: 이 테스트는 시스템을 한계까지 밀어붙입니다!');
    console.log('🔥 서버 크래시, 메모리 부족 등이 발생할 수 있습니다.');
    console.log('📊 실시간 모니터링을 권장합니다.\n');
    
    // 첫 번째 비스트 모드 테스트 (가장 안전한?)
    const selectedTest = beastModeConfigs[0]; // 토네이도 테스트
    
    console.log(`🌪️  실행 중: ${selectedTest.name}`);
    console.log(`📊 예상 최대 처리량: ~${(selectedTest.config.maxConnections * selectedTest.config.messageRate * 2.6).toLocaleString()} TPS`);
    console.log('🎛️  더 강한 테스트: beastModeConfigs[1-3] 중 선택\n');
    
    // 시스템 체크
    console.log('🔍 시스템 준비 상태 체크:');
    console.log('   - 충분한 메모리 (8GB+ 권장)');  
    console.log('   - Redis 메모리 설정 확인');
    console.log('   - JVM 힙 크기 (-Xmx4g+ 권장)');
    console.log('   - ulimit -n 설정 (65536+ 권장)\n');
    
    console.log('⏳ 5초 후 시작... Ctrl+C로 취소 가능');
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    const test = new AdvancedChatPerformanceTest(selectedTest.config);
    
    // 테스트 시작 시간
    const startTime = Date.now();
    
    try {
        await test.runAdvancedTest();
        
        const duration = Date.now() - startTime;
        
        // 성공 시 축하 메시지
        console.log('\n🎉🎉🎉 비스트 모드 테스트 완료! 🎉🎉🎉');
        console.log(`🏆 축하합니다! 서버가 ${selectedTest.name}을 버텨냈습니다!`);
        console.log(`⏱️  총 소요 시간: ${(duration / 1000).toFixed(2)}초`);
        
        // 다음 도전 제안
        console.log('\n🚀 다음 도전:');
        console.log('1. 🌊 쓰나미 테스트 (beastModeConfigs[1])');
        console.log('2. 🔥 헬파이어 테스트 (beastModeConfigs[2])'); 
        console.log('3. 💀 데스스타 테스트 (beastModeConfigs[3])');
        
        // 성능 분석
        console.log('\n📈 성능 분석:');
        console.log('- TPS > 20,000: 🔥 엔터프라이즈급');
        console.log('- TPS > 50,000: 🚀 클라우드네이티브급');
        console.log('- TPS > 100,000: 💎 리얼타임급');
        
    } catch (error) {
        console.error('\n💥💥💥 비스트 모드 테스트 실패! 💥💥💥');
        console.error(`❌ 오류: ${error.message}`);
        
        console.log('\n🔧 문제 해결 가이드:');
        console.log('1. 🧠 메모리 부족 시:');
        console.log('   - JVM: -Xmx6g -Xms2g');
        console.log('   - Redis: maxmemory 4gb');
        
        console.log('2. 🔌 연결 한계 시:');
        console.log('   - ulimit -n 65536');
        console.log('   - server.tomcat.max-connections=10000');
        
        console.log('3. ⚡ CPU 부하 시:');
        console.log('   - message rate 줄이기');
        console.log('   - 연결 수 줄이기');
        
        console.log('4. 📊 모니터링:');
        console.log('   - htop (CPU/메모리)');
        console.log('   - redis-cli info memory');
        console.log('   - netstat -an | grep :8084 | wc -l');
    }
    
    console.log('\n🎯 최종 목표: 서버의 진짜 한계점 찾기!');
}

// 메인 실행
if (require.main === module) {
    runBeastModeTests();
}
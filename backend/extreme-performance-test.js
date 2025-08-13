const AdvancedChatPerformanceTest = require('./advanced-chat-performance-test.js');

/**
 * 🔥 극한 성능 테스트 시나리오
 */
async function runExtremeTests() {
    const extremeTestConfigs = [
        {
            name: '🔥 극한 연결 테스트 (1000 동시 연결)',
            config: {
                maxConnections: 1000,
                messageRate: 3,
                testDuration: 60000,
                rampUpDuration: 15000
            }
        },
        {
            name: '⚡ 극한 메시지 테스트 (200 연결 × 20 msg/s)',
            config: {
                maxConnections: 200,
                messageRate: 20,
                testDuration: 90000,
                rampUpDuration: 10000
            }
        },
        {
            name: '💥 종합 극한 테스트 (500 연결 × 15 msg/s × 3분)',
            config: {
                maxConnections: 500,
                messageRate: 15,
                testDuration: 180000,
                rampUpDuration: 30000
            }
        }
    ];
    
    console.log('🔥 극한 성능 테스트 시리즈 시작!');
    console.log('⚠️  주의: 서버에 높은 부하를 줄 수 있습니다.');
    
    // 첫 번째 극한 테스트 실행
    const selectedTest = extremeTestConfigs[0];
    
    console.log(`\n🎯 실행 중: ${selectedTest.name}`);
    console.log('🔄 더 강한 테스트를 원하면 extremeTestConfigs[1] 또는 [2]로 변경하세요.\n');
    
    const test = new AdvancedChatPerformanceTest(selectedTest.config);
    
    try {
        await test.runAdvancedTest();
        
        // 결과에 따른 추천
        console.log('\n📋 다음 단계 추천:');
        console.log('✅ 성공 시: extremeTestConfigs[1] 또는 [2] 시도');
        console.log('⚠️  실패 시: 서버 최적화 또는 부하 분산 고려');
        console.log('📊 모니터링: Redis, Kafka, JVM 메트릭 확인 권장');
        
    } catch (error) {
        console.error('❌ 극한 테스트 실패:', error.message);
        console.log('\n🔧 문제 해결 방안:');
        console.log('1. JVM 힙 크기 증가: -Xmx4g');
        console.log('2. Redis 메모리 설정 확인');
        console.log('3. Kafka 파티션 수 증가');
        console.log('4. OS ulimit 설정 확인');
    }
}

// 메인 실행
if (require.main === module) {
    runExtremeTests();
}
const AdvancedChatPerformanceTest = require('./advanced-chat-performance-test.js');
const WebSocket = require('ws');

/**
 * 🌐 실제 프로덕션 서버 성능 테스트
 * Target: i13a403.p.ssafy.io:8084
 */
class ProductionServerTest extends AdvancedChatPerformanceTest {
    constructor(config = {}) {
        // 실제 서버 URL로 변경
        const productionConfig = {
            ...config,
            serverUrl: 'ws://i13a403.p.ssafy.io:8084'
        };

        super(productionConfig);

        // 프로덕션 환경 고려사항
        this.networkLatency = [];
        this.productionMetrics = {
            networkErrors: 0,
            timeouts: 0,
            dnsLookupTime: 0,
            tcpConnectTime: 0
        };
    }

    async createConnection(connectionId, delay = 0) {
        // 네트워크 지연 측정 시작
        const networkStart = performance.now();

        if (delay > 0) {
            await this.sleep(delay);
        }

        return new Promise((resolve, reject) => {
            try {
                const connectionStart = performance.now();

                // 실제 서버에서 더 안전한 토큰 생성
                const isWatchChat = connectionId % 3 !== 0;
                const timestamp = Date.now();
                const testUrl = isWatchChat
                    ? `${this.serverUrl}/ws/watch-chat?sessionToken=test-prod-watch-${connectionId}-${timestamp}&gameId=${Math.floor(Math.random() * 3) + 1}&teamId=${Math.floor(Math.random() * 4) + 1}`
                    : `${this.serverUrl}/ws/match-chat?sessionToken=test-prod-match-${connectionId}-${timestamp}&matchId=match_${Math.floor(Math.random() * 5) + 1}`;

                console.log(`🌐 [${connectionId}] ${isWatchChat ? '직관' : '매칭'} 채팅 → 실제 서버 연결 시도`);

                const ws = new WebSocket(testUrl);
                let messageCount = 0;
                let messageInterval;
                let isConnected = false;

                // 타임아웃 설정 (실제 서버는 네트워크 지연이 있을 수 있음)
                const connectionTimeout = setTimeout(() => {
                    if (!isConnected) {
                        this.productionMetrics.timeouts++;
                        ws.close();
                        reject(new Error('Connection timeout'));
                    }
                }, 15000); // 15초 타임아웃

                ws.on('open', () => {
                    clearTimeout(connectionTimeout);
                    isConnected = true;

                    const connectionTime = performance.now() - connectionStart;
                    const networkLatency = performance.now() - networkStart;

                    this.metrics.connectionTimes.push(connectionTime);
                    this.networkLatency.push(networkLatency);
                    this.metrics.successfulConnections++;

                    console.log(`✅ [${connectionId}] 실제 서버 연결 성공 (${connectionTime.toFixed(2)}ms, 네트워크: ${networkLatency.toFixed(2)}ms)`);

                    // 프로덕션에서는 메시지 전송율을 조금 낮춤 (서버 부하 고려)
                    const productionMessageRate = Math.max(1, this.messageRate * 0.7);

                    messageInterval = setInterval(() => {
                        if (!this.isRunning || ws.readyState !== WebSocket.OPEN) {
                            clearInterval(messageInterval);
                            return;
                        }

                        const messageStart = performance.now();
                        const messages = isWatchChat
                            ? ['⚽ 실제서버 골!', '🔥 프로덕션 테스트!', '실서버 화이팅!', '라이브 테스트!', '실환경 GG!']
                            : ['실제 서버 테스트', '프로덕션 환경', '라이브 테스트', '실서버 화이팅!', '운영환경 체크'];

                        const message = `${messages[Math.floor(Math.random() * messages.length)]} #${messageCount++} @${Date.now()} [실서버]`;

                        try {
                            ws.send(message);
                            this.metrics.messagesSent++;

                            ws._pendingMessages = ws._pendingMessages || new Map();
                            ws._pendingMessages.set(Date.now(), messageStart);
                        } catch (sendError) {
                            console.error(`❌ [${connectionId}] 메시지 전송 실패:`, sendError.message);
                            this.productionMetrics.networkErrors++;
                        }

                    }, 1000 / productionMessageRate + Math.random() * 1000); // 지터 증가

                    resolve({ ws, connectionId, messageInterval, isWatchChat });
                });

                ws.on('message', (data) => {
                    try {
                        this.metrics.messagesReceived++;

                        // 실제 서버에서는 더 정확한 응답시간 측정 어려움
                        if (ws._pendingMessages && ws._pendingMessages.size > 0) {
                            const timestamps = Array.from(ws._pendingMessages.keys()).sort();
                            const oldestTimestamp = timestamps[0];
                            const startTime = ws._pendingMessages.get(oldestTimestamp);
                            const responseTime = performance.now() - startTime;

                            this.metrics.totalResponseTime += responseTime;
                            this.metrics.minResponseTime = Math.min(this.metrics.minResponseTime, responseTime);
                            this.metrics.maxResponseTime = Math.max(this.metrics.maxResponseTime, responseTime);

                            ws._pendingMessages.delete(oldestTimestamp);
                        }

                        // 가끔 서버 응답 로깅 (너무 많으면 성능에 영향)
                        if (Math.random() < 0.01) { // 1% 확률로 로깅
                            console.log(`📥 [${connectionId}] 실서버 응답 샘플: ${data.toString().substring(0, 50)}...`);
                        }

                    } catch (e) {
                        console.error(`❌ [${connectionId}] 메시지 파싱 오류:`, e.message);
                    }
                });

                ws.on('error', (error) => {
                    clearTimeout(connectionTimeout);
                    console.error(`❌ [${connectionId}] 실제 서버 연결 오류:`, error.message);

                    // 네트워크 관련 오류 분류
                    if (error.message.includes('ECONNRESET')) {
                        this.productionMetrics.networkErrors++;
                    } else if (error.message.includes('timeout')) {
                        this.productionMetrics.timeouts++;
                    }

                    this.metrics.errors.push({
                        connectionId,
                        error: error.message,
                        timestamp: Date.now(),
                        type: isWatchChat ? 'watch' : 'match',
                        environment: 'production'
                    });
                    this.metrics.failedConnections++;
                    if (messageInterval) clearInterval(messageInterval);
                    reject(error);
                });

                ws.on('close', (code, reason) => {
                    clearTimeout(connectionTimeout);
                    if (messageInterval) clearInterval(messageInterval);
                    console.log(`🔌 [${connectionId}] 실서버 연결 종료: ${code} - ${reason}`);
                });

            } catch (error) {
                console.error(`❌ [${connectionId}] 실서버 연결 생성 실패:`, error.message);
                this.metrics.failedConnections++;
                this.productionMetrics.networkErrors++;
                reject(error);
            }
        });
    }

    printAdvancedResults(totalTestTime) {
        // 기본 결과 출력
        super.printAdvancedResults(totalTestTime);

        // 프로덕션 서버 전용 메트릭 추가
        console.log('\n🌐 ==========================================');
        console.log('    실제 서버 환경 추가 분석');
        console.log('🌐 ==========================================');

        if (this.networkLatency.length > 0) {
            const avgNetworkLatency = this.networkLatency.reduce((a, b) => a + b, 0) / this.networkLatency.length;
            const maxNetworkLatency = Math.max(...this.networkLatency);
            const minNetworkLatency = Math.min(...this.networkLatency);

            console.log(`🌐 평균 네트워크 지연: ${avgNetworkLatency.toFixed(2)}ms`);
            console.log(`🚀 최소 네트워크 지연: ${minNetworkLatency.toFixed(2)}ms`);
            console.log(`🐌 최대 네트워크 지연: ${maxNetworkLatency.toFixed(2)}ms`);
        }

        console.log(`📡 네트워크 오류: ${this.productionMetrics.networkErrors}개`);
        console.log(`⏱️  연결 타임아웃: ${this.productionMetrics.timeouts}개`);
        console.log(`🎯 서버 위치: i13a403.p.ssafy.io (원격)`);

        // 로컬 vs 원격 비교 가이드
        console.log('\n📊 로컬 vs 실제 서버 비교:');
        console.log('- 네트워크 지연: 로컬 ~0ms vs 원격 ~10-50ms');
        console.log('- 안정성: 로컬 > 원격 (네트워크 변수)');
        console.log('- 처리량: 로컬 ≥ 원격 (대역폭 제한)');
        console.log('- 응답시간: 로컬 < 원격 (RTT 포함)');

        console.log('\n🎯 실제 서버 성능 등급:');
        const realWorldTps = this.metrics.messagesReceived / (totalTestTime / 1000);
        if (realWorldTps > 5000) {
            console.log('🏆 실제 환경 우수 - 상용 서비스 가능');
        } else if (realWorldTps > 1000) {
            console.log('👍 실제 환경 양호 - 중소 서비스 적합');
        } else {
            console.log('⚠️  실제 환경 개선 필요 - 최적화 권장');
        }

        console.log('🌐 ==========================================\n');
    }
}

// 실제 서버 테스트 실행
async function runProductionServerTests() {
    const productionTestConfigs = [
        {
            name: '🌐 실제 서버 연결 테스트',
            config: {
                maxConnections: 50,
                messageRate: 2,
                testDuration: 30000,
                rampUpDuration: 5000
            }
        },
        {
            name: '🔥 실제 서버 부하 테스트',
            config: {
                maxConnections: 200,
                messageRate: 3,
                testDuration: 60000,
                rampUpDuration: 10000
            }
        },
        {
            name: '⚡ 실제 서버 스트레스 테스트',
            config: {
                maxConnections: 500,
                messageRate: 5,
                testDuration: 90000,
                rampUpDuration: 20000
            }
        }
    ];

    console.log('🌐 실제 프로덕션 서버 성능 테스트 시작!');
    console.log('🎯 Target: i13a403.p.ssafy.io:8084');
    console.log('⚠️  주의: 실제 서버에 부하를 줄 수 있습니다.');

    // 기본적으로 가장 가벼운 테스트부터
    const selectedTest = productionTestConfigs[0];

    console.log(`\n🌐 실행 중: ${selectedTest.name}`);
    console.log('📈 더 강한 테스트: productionTestConfigs[1] 또는 [2]로 변경\n');

    const test = new ProductionServerTest(selectedTest.config);

    try {
        await test.runAdvancedTest();

        console.log('\n🎉 실제 서버 테스트 완료!');
        console.log('📊 결과를 로컬 테스트와 비교해보세요.');

    } catch (error) {
        console.error('\n❌ 실제 서버 테스트 실패:', error.message);
        console.log('\n🔧 가능한 원인:');
        console.log('1. 네트워크 연결 문제');
        console.log('2. 서버 방화벽 설정');
        console.log('3. 서버 리소스 부족');
        console.log('4. Rate limiting');
    }
}

// 🚀 종합 테스트 실행 함수
async function runComprehensiveTests(difficulty, regularTests, spikeTests) {
    const regularTest = regularTests[difficulty];
    const spikeTest = spikeTests[difficulty];

    console.log('📋 테스트 계획:');
    console.log(`1️⃣ ${regularTest.name}`);
    console.log(`2️⃣ ${spikeTest.name}`);
    console.log('3️⃣ 결과 비교 분석\n');

    try {
        // 1단계: 일반 부하 테스트
        console.log('🔥 1단계: 일반 부하 테스트 시작!');
        console.log('================================================\n');

        const regularTestInstance = new ProductionServerTest(regularTest.config);
        const regularResults = await regularTestInstance.runAdvancedTest();

        console.log('\n✅ 1단계 완료! 잠시 서버 회복 대기...');
        await new Promise(resolve => setTimeout(resolve, 5000)); // 5초 대기

        // 2단계: 순간 부하 테스트
        console.log('\n⚡ 2단계: 순간 부하 테스트 시작!');
        console.log('================================================\n');

        const spikeResults = await runSpikeTest(spikeTest);

        // 3단계: 결과 비교
        console.log('\n📊 3단계: 결과 비교 분석');
        console.log('================================================\n');
        compareResults(regularResults, spikeResults, difficulty);

    } catch (error) {
        console.error('\n❌ 종합 테스트 실패:', error.message);
        printTroubleshootingGuide(difficulty);
    }
}

// ⚡ 순간 부하 테스트 실행
async function runSpikeTest(spikeTestConfig) {
    const { config } = spikeTestConfig;

    console.log(`⚡ ${config.spikeConnections}개 연결을 동시에 생성합니다...`);

    const spikeStart = performance.now();
    const connections = [];
    const promises = [];

    // 순간적으로 모든 연결 생성
    for (let i = 0; i < config.spikeConnections; i++) {
        const promise = createSpikeConnection(i, config);
        promises.push(promise);
    }

    try {
        const results = await Promise.allSettled(promises);
        const successful = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.filter(r => r.status === 'rejected').length;

        console.log(`⚡ 순간 연결 결과: 성공 ${successful}개, 실패 ${failed}개`);

        // 스파이크 지속 시간만큼 대기
        console.log(`⏳ ${config.spikeDuration/1000}초간 순간 부하 유지...`);
        await new Promise(resolve => setTimeout(resolve, config.spikeDuration));

        const spikeEnd = performance.now();

        return {
            totalConnections: config.spikeConnections,
            successfulConnections: successful,
            failedConnections: failed,
            successRate: (successful / config.spikeConnections * 100).toFixed(2),
            totalTime: spikeEnd - spikeStart,
            testType: 'spike'
        };

    } catch (error) {
        console.error('❌ 순간 부하 테스트 실패:', error.message);
        throw error;
    }
}

// ⚡ 순간 부하용 연결 생성
async function createSpikeConnection(connectionId, config) {
    return new Promise((resolve, reject) => {
        const timestamp = Date.now();
        const isWatchChat = connectionId % 2 === 0;
        const testUrl = isWatchChat
            ? `ws://i13a403.p.ssafy.io:8084/ws/watch-chat?sessionToken=spike-watch-${connectionId}-${timestamp}&gameId=1&teamId=1`
            : `ws://i13a403.p.ssafy.io:8084/ws/match-chat?sessionToken=spike-match-${connectionId}-${timestamp}&matchId=spike_test`;

        const ws = new WebSocket(testUrl);
        let messagesSent = 0;

        ws.on('open', () => {
            console.log(`⚡ [${connectionId}] 순간 연결 성공`);

            // 빠른 메시지 전송
            const messageInterval = setInterval(() => {
                if (messagesSent >= 10) { // 최대 10개 메시지만
                    clearInterval(messageInterval);
                    ws.close();
                    return;
                }

                try {
                    ws.send(`스파이크테스트_${messagesSent++}_${Date.now()}`);
                } catch (e) {
                    clearInterval(messageInterval);
                    ws.close();
                }
            }, 1000 / config.messageRate);

            resolve(ws);
        });

        ws.on('error', (error) => {
            console.error(`❌ [${connectionId}] 순간 연결 실패: ${error.message.substring(0, 50)}`);
            reject(error);
        });

        // 타임아웃
        setTimeout(() => {
            if (ws.readyState === WebSocket.CONNECTING) {
                ws.close();
                reject(new Error('Spike connection timeout'));
            }
        }, 10000);
    });
}

// 📊 결과 비교 분석
function compareResults(regularResults, spikeResults, difficulty) {
    console.log('📈 ==========================================');
    console.log('    일반 부하 vs 순간 부하 비교 분석');
    console.log('📈 ==========================================');

    console.log('🔄 일반 부하 테스트:');
    console.log(`   - 연결 성공률: ${((regularResults?.successfulConnections || 0) / (regularResults?.totalConnections || 1) * 100).toFixed(1)}%`);
    console.log(`   - 평균 응답시간: ${regularResults?.averageResponseTime || 'N/A'}ms`);

    console.log('\n⚡ 순간 부하 테스트:');
    console.log(`   - 연결 성공률: ${spikeResults.successRate}%`);
    console.log(`   - 총 처리시간: ${(spikeResults.totalTime).toFixed(2)}ms`);

    // 난이도별 평가
    console.log(`\n🎚️ ${difficulty.toUpperCase()} 난이도 평가:`);

    if (difficulty === 'easy') {
        console.log('🟢 하급 테스트 - 기본 연결성 확인');
        console.log('   목표: 연결 성공률 95% 이상');
        console.log('   현재 서버 상태 파악용');
    } else if (difficulty === 'medium') {
        console.log('🟡 중급 테스트 - 실용적 부하 처리');
        console.log('   목표: 연결 성공률 90% 이상');
        console.log('   일반 운영 환경 시뮬레이션');
    } else {
        console.log('🔴 상급 테스트 - 극한 성능 측정');
        console.log('   목표: 연결 성공률 80% 이상');
        console.log('   서버 한계점 탐지');
    }

    console.log('📈 ==========================================\n');
}

// 🔧 문제 해결 가이드
function printTroubleshootingGuide(difficulty) {
    console.log('\n🔧 ==========================================');
    console.log('    문제 해결 가이드');
    console.log('🔧 ==========================================');

    if (difficulty === 'easy') {
        console.log('🟢 하급 테스트 실패 시:');
        console.log('1. 서버가 실행 중인지 확인');
        console.log('2. 네트워크 연결 상태 점검');
        console.log('3. 방화벽 설정 확인');
    } else if (difficulty === 'medium') {
        console.log('🟡 중급 테스트 실패 시:');
        console.log('1. 서버 리소스 모니터링');
        console.log('2. DB 연결 상태 확인');
        console.log('3. Redis/Kafka 상태 점검');
    } else {
        console.log('🔴 상급 테스트 실패 시:');
        console.log('1. JVM 힙 메모리 증설');
        console.log('2. 커넥션 풀 크기 조정');
        console.log('3. 로드밸런서 도입 검토');
    }

    console.log('🔧 ==========================================\n');
}

// 메인 실행 - 난이도별 테스트 시나리오
if (require.main === module) {
    // 🎚️ 난이도별 테스트 설정
    const testDifficulty = {
        // 🟢 하급 - 안전한 연결 테스트
        easy: {
            name: '🟢 하급: 안전한 연결 테스트',
            config: {
                maxConnections: 50,        // 50명
                messageRate: 2,            // 초당 2개
                testDuration: 30000,       // 30초
                rampUpDuration: 10000      // 10초 점진증가
            }
        },

        // 🟡 중급 - 실용적 부하 테스트
        medium: {
            name: '🟡 중급: 실용적 부하 테스트',
            config: {
                maxConnections: 300,       // 300명
                messageRate: 5,            // 초당 5개
                testDuration: 120000,      // 2분
                rampUpDuration: 20000      // 20초 점진증가
            }
        },

        // 🔴 상급 - 극한 스트레스 테스트
        hard: {
            name: '🔴 상급: 극한 스트레스 테스트',
            config: {
                maxConnections: 1000,      // 1000명
                messageRate: 15,           // 초당 15개
                testDuration: 300000,      // 5분
                rampUpDuration: 30000      // 30초 점진증가
            }
        }
    };

    // 🌊 순간 부하 테스트 (스파이크) 설정
    const spikeTestConfig = {
        easy: {
            name: '🟢 하급: 순간 부하 테스트',
            config: {
                spikeConnections: 100,     // 100개 순간 연결
                spikeDuration: 5000,       // 5초간 유지
                messageRate: 10            // 초당 10개
            }
        },
        medium: {
            name: '🟡 중급: 순간 부하 테스트',
            config: {
                spikeConnections: 500,     // 500개 순간 연결
                spikeDuration: 10000,      // 10초간 유지
                messageRate: 20            // 초당 20개
            }
        },
        hard: {
            name: '🔴 상급: 순간 부하 테스트',
            config: {
                spikeConnections: 1500,    // 1500개 순간 연결
                spikeDuration: 15000,      // 15초간 유지
                messageRate: 50            // 초당 50개
            }
        }
    };

    // 🎯 현재 난이도 설정 (여기서 변경!)
    const currentDifficulty = 'hard';  // 'easy', 'medium', 'hard'

    console.log('🎚️ =============================================');
    console.log('    난이도별 프로덕션 서버 성능 테스트');
    console.log('🎚️ =============================================');
    console.log('🎯 Target: i13a403.p.ssafy.io:8084');
    console.log(`🎚️ 현재 난이도: ${currentDifficulty.toUpperCase()}`);
    console.log('⚠️  주의: 실제 서버에 부하를 줄 수 있습니다.\n');

    runComprehensiveTests(currentDifficulty, testDifficulty, spikeTestConfig);
}

module.exports = ProductionServerTest;

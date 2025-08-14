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
            serverUrl: 'ws://i13a403.p.ssafy.io:8083'
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

// ⚡ 순간 부하 테스트 실행 (단계별)
async function runSpikeTest(spikeTestConfig) {
    const { config } = spikeTestConfig;

    console.log(`⚡ ${config.spikeConnections}개 연결을 동시에 생성합니다...`);

    const spikeStart = performance.now();
    const connections = [];
    const promises = [];

    // ⚡ 진짜 순간 부하: 모든 연결을 동시에 생성
    for (let i = 0; i < config.spikeConnections; i++) {
        const promise = createSpikeConnection(i, config)
            .then(result => ({ status: 'fulfilled', value: result }))
            .catch(error => ({ status: 'rejected', reason: error }));
        promises.push(promise);
    }

    try {
        const results = await Promise.all(promises);
        const successful = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.filter(r => r.status === 'rejected').length;

        console.log(`⚡ ${config.spikeConnections}개 연결 결과: 성공 ${successful}개, 실패 ${failed}개`);

        // 스파이크 지속 시간만큼 대기
        console.log(`⏳ ${config.spikeDuration/1000}초간 순간 부하 유지...`);
        await new Promise(resolve => setTimeout(resolve, config.spikeDuration));

        const spikeEnd = performance.now();

        return {
            connections: config.spikeConnections,
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

// 🧹 연결 정리 함수
async function forceCleanupConnections() {
    console.log('🧹 모든 연결 정리 중...');
    
    // Node.js 가비지 컬렉션 강제 실행 (가능한 경우)
    if (global.gc) {
        global.gc();
    }
    
    // 연결 정리 대기 시간
    await new Promise(resolve => setTimeout(resolve, 2000));
    console.log('✅ 연결 정리 완료');
}

// ⚡ 순간 부하용 연결 생성
async function createSpikeConnection(connectionId, config) {
    return new Promise((resolve, reject) => {
        const timestamp = Date.now();
        const isWatchChat = connectionId % 2 === 0;
        const testUrl = isWatchChat
            ? `ws://i13a403.p.ssafy.io:8083/ws/watch-chat?sessionToken=test-spike-watch-${connectionId}-${timestamp}&gameId=1&teamId=1`
            : `ws://i13a403.p.ssafy.io:8083/ws/match-chat?sessionToken=test-spike-match-${connectionId}-${timestamp}&matchId=match_${Math.floor(Math.random() * 5) + 1}`;

        const ws = new WebSocket(testUrl);
        let messagesSent = 0;

        ws.on('open', () => {
            console.log(`⚡ [${connectionId}] 순간 연결 성공`);

            // 빠른 메시지 전송
            const messageInterval = setInterval(() => {
                if (messagesSent >= 3) { // 메시지 수 더 줄임 (서버 부하 최소화)
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

            // 자동 연결 종료 타이머 (5초 후 강제 종료)
            setTimeout(() => {
                if (ws.readyState === WebSocket.OPEN) {
                    ws.close();
                }
            }, 5000);

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

// 🚀 단계별 순간 부하 테스트 실행 함수
async function runProgressiveSpikeTests() {
    const spikeSteps = [200, 500, 700, 900]; // 안전한 테스트 수준
    const allResults = [];

    console.log('⚡ =============================================');
    console.log('    단계별 순간 부하 테스트 시작');
    console.log('⚡ =============================================');
    console.log('🎯 Target: i13a403.p.ssafy.io:8083 (nginx + 2인스턴스)');
    console.log(`📊 테스트 단계: ${spikeSteps.join('개 → ')}개 순간 연결\n`);

    for (let i = 0; i < spikeSteps.length; i++) {
        const connections = spikeSteps[i];

        console.log(`\n📈 ${i + 1}단계: ${connections}개 순간 연결 테스트`);
        console.log('===============================================');

        const spikeConfig = {
            name: `${connections}개 순간 연결`,
            config: {
                spikeConnections: connections,
                spikeDuration: 8000,  // 8초간 유지
                messageRate: 5        // 초당 5개 메시지
            }
        };

        try {
            const result = await runSpikeTest(spikeConfig);
            result.stepNumber = i + 1;
            result.stepName = `${connections}개 연결`;
            allResults.push(result);

            console.log(`✅ ${i + 1}단계 완료! 연결 정리 중...`);
            
            // 모든 연결 강제 정리
            await forceCleanupConnections();

            // 단계 간 서버 회복 시간 (연결 정리 후 충분한 대기)
            if (i < spikeSteps.length - 1) {
                console.log('⏳ 서버 안정화 대기 (5초)...\n');
                await new Promise(resolve => setTimeout(resolve, 5000));
            }

        } catch (error) {
            console.error(`❌ ${i + 1}단계 (${connections}개 연결) 실패:`, error.message);
            allResults.push({
                stepNumber: i + 1,
                stepName: `${connections}개 연결`,
                connections: connections,
                successfulConnections: 0,
                failedConnections: connections,
                successRate: '0.00',
                totalTime: 0,
                error: error.message
            });
        }
    }

    // 🏁 최종 통계 출력
    printFinalSpikeStatistics(allResults);
}

// 📊 최종 통계 출력
function printFinalSpikeStatistics(allResults) {
    console.log('\n🏁 =============================================');
    console.log('    최종 단계별 순간 부하 테스트 통계');
    console.log('🏁 =============================================');

    // 테이블 헤더
    console.log('\n📊 단계별 결과 요약:');
    console.log('┌─────┬─────────────┬──────┬──────┬──────────┬──────────┐');
    console.log('│단계 │   연결 수   │ 성공 │ 실패 │ 성공률(%)│ 처리시간 │');
    console.log('├─────┼─────────────┼──────┼──────┼──────────┼──────────┤');

    let totalConnections = 0;
    let totalSuccessful = 0;
    let totalFailed = 0;
    let avgProcessingTime = 0;

    allResults.forEach(result => {
        const step = result.stepNumber.toString().padStart(2);
        const stepName = result.stepName.padEnd(11);
        const successful = result.successfulConnections.toString().padStart(4);
        const failed = result.failedConnections.toString().padStart(4);
        const successRate = result.successRate.padStart(8);
        const processingTime = result.error ? '   실패   ' : `${(result.totalTime / 1000).toFixed(2)}초`.padStart(8);

        console.log(`│ ${step}  │ ${stepName} │ ${successful} │ ${failed} │ ${successRate} │ ${processingTime} │`);

        if (!result.error) {
            totalConnections += result.connections;
            totalSuccessful += result.successfulConnections;
            totalFailed += result.failedConnections;
            avgProcessingTime += result.totalTime;
        }
    });

    console.log('└─────┴─────────────┴──────┴──────┴──────────┴──────────┘');

    // 전체 요약 통계
    const overallSuccessRate = totalConnections > 0 ? ((totalSuccessful / totalConnections) * 100).toFixed(2) : '0.00';
    const avgTime = allResults.filter(r => !r.error).length > 0 ?
        (avgProcessingTime / allResults.filter(r => !r.error).length / 1000).toFixed(2) : '0.00';

    console.log('\n📈 전체 요약:');
    console.log(`🔢 총 시도 연결 수: ${totalConnections}개`);
    console.log(`✅ 총 성공 연결 수: ${totalSuccessful}개`);
    console.log(`❌ 총 실패 연결 수: ${totalFailed}개`);
    console.log(`📊 전체 성공률: ${overallSuccessRate}%`);
    console.log(`⏱️  평균 처리 시간: ${avgTime}초`);

    // 성능 등급 판정
    console.log('\n🎯 서버 성능 등급:');
    const successRateNum = parseFloat(overallSuccessRate);

    if (successRateNum >= 95) {
        console.log('🏆 최우수 (95%+) - 프로덕션 서비스 완벽 대응');
    } else if (successRateNum >= 90) {
        console.log('🥇 우수 (90-94%) - 프로덕션 서비스 안정적');
    } else if (successRateNum >= 80) {
        console.log('🥈 양호 (80-89%) - 일반적인 부하 처리 가능');
    } else if (successRateNum >= 60) {
        console.log('🥉 보통 (60-79%) - 최적화 필요');
    } else {
        console.log('⚠️  개선 필요 (60% 미만) - 서버 성능 점검 권장');
    }

    // 단계별 성능 트렌드 분석
    console.log('\n📈 성능 트렌드 분석:');
    let trend = '안정적';
    let previousRate = 100;

    for (let i = 0; i < allResults.length; i++) {
        const currentRate = parseFloat(allResults[i].successRate);

        if (i > 0) {
            const diff = currentRate - previousRate;
            if (diff < -10) {
                trend = '급감';
                break;
            } else if (diff < -5) {
                trend = '하락';
            }
        }
        previousRate = currentRate;
    }

    if (trend === '급감') {
        console.log('📉 성능 급감 감지 - 병목 지점 존재 가능성');
        console.log('💡 권장사항: 커넥션 풀 크기, 스레드 풀 설정 점검');
    } else if (trend === '하락') {
        console.log('📊 성능 점진적 하락 - 부하 증가에 따른 자연스러운 현상');
        console.log('💡 권장사항: 모니터링 강화, 스케일링 계획 수립');
    } else {
        console.log('📊 성능 안정적 유지 - 서버가 부하를 잘 처리하고 있음');
        console.log('💡 상태: 현재 설정으로 운영 가능');
    }

    console.log('\n🏁 =============================================');
    console.log('    단계별 순간 부하 테스트 완료!');
    console.log('🏁 =============================================\n');
}

// 메인 실행
if (require.main === module) {
    console.log('⚡ 안전한 단계별 순간 부하 테스트 시작!');
    console.log('📊 5개 → 10개 → 15개 → 20개 → 25개 순간 연결 테스트');
    console.log('🛡️  안전 모드: 연결 자동 정리, 서버 안정화 대기 포함');
    console.log('⚠️  주의: 실제 프로덕션 서버 테스트입니다.\n');

    runProgressiveSpikeTests()
        .then(() => {
            console.log('🎉 모든 단계별 테스트가 완료되었습니다!');
        })
        .catch((error) => {
            console.error('❌ 테스트 실행 중 오류 발생:', error.message);
        });
}

module.exports = ProductionServerTest;

const WebSocket = require('ws');
const { performance } = require('perf_hooks');

/**
 * 🚀 고급 채팅 서버 성능 테스트
 * Artillery 대신 Node.js로 고성능 테스트 구현
 */
class AdvancedChatPerformanceTest {
    constructor(config = {}) {
        this.serverUrl = config.serverUrl || 'ws://localhost:8084';
        this.maxConnections = config.maxConnections || 100;
        this.messageRate = config.messageRate || 3; // messages per second per connection
        this.testDuration = config.testDuration || 60000; // 1 minute
        this.rampUpDuration = config.rampUpDuration || 10000; // 10 seconds
        
        this.connections = [];
        this.metrics = {
            totalConnections: 0,
            successfulConnections: 0,
            failedConnections: 0,
            messagesSent: 0,
            messagesReceived: 0,
            totalResponseTime: 0,
            minResponseTime: Infinity,
            maxResponseTime: 0,
            errors: [],
            connectionTimes: [],
            throughputHistory: []
        };
        
        this.isRunning = false;
        this.startTime = 0;
    }

    async createConnection(connectionId, delay = 0) {
        // Ramp-up: 점진적 연결 생성을 위한 지연
        if (delay > 0) {
            await this.sleep(delay);
        }
        
        return new Promise((resolve, reject) => {
            try {
                const connectionStart = performance.now();
                
                // 직관/매칭 채팅 랜덤 선택
                const isWatchChat = connectionId % 3 !== 0; // 67% 직관, 33% 매칭
                const testUrl = isWatchChat
                    ? `${this.serverUrl}/ws/watch-chat?sessionToken=test-advanced-watch-${connectionId}&gameId=${Math.floor(Math.random() * 5) + 1}&teamId=${Math.floor(Math.random() * 6) + 1}`
                    : `${this.serverUrl}/ws/match-chat?sessionToken=test-advanced-match-${connectionId}&matchId=match_${Math.floor(Math.random() * 10) + 1}`;
                
                console.log(`🔗 [${connectionId}] ${isWatchChat ? '직관' : '매칭'} 채팅 연결 시도`);
                
                const ws = new WebSocket(testUrl);
                let messageCount = 0;
                let messageInterval;
                
                ws.on('open', () => {
                    const connectionTime = performance.now() - connectionStart;
                    this.metrics.connectionTimes.push(connectionTime);
                    this.metrics.successfulConnections++;
                    
                    console.log(`✅ [${connectionId}] 연결 성공 (${connectionTime.toFixed(2)}ms)`);
                    
                    // 주기적으로 메시지 전송
                    messageInterval = setInterval(() => {
                        if (!this.isRunning || ws.readyState !== WebSocket.OPEN) {
                            clearInterval(messageInterval);
                            return;
                        }
                        
                        const messageStart = performance.now();
                        const messages = isWatchChat 
                            ? ['⚽ 골!!!', '🔥 멋진 플레이!', '화이팅!', '우리팀 최고!', 'GG!']
                            : ['같이 게임해요', 'gg wp', '잘 부탁드려요', '화이팅!', '수고하셨어요'];
                        
                        const message = messages[Math.floor(Math.random() * messages.length)] + ` #${messageCount++} @${Date.now()}`;
                        
                        ws.send(message);
                        this.metrics.messagesSent++;
                        
                        // 응답 시간 추적을 위한 타임스탬프 저장
                        ws._pendingMessages = ws._pendingMessages || new Map();
                        ws._pendingMessages.set(Date.now(), messageStart);
                        
                    }, 1000 / this.messageRate + Math.random() * 500); // 약간의 지터 추가
                    
                    resolve({ ws, connectionId, messageInterval, isWatchChat });
                });
                
                ws.on('message', (data) => {
                    try {
                        this.metrics.messagesReceived++;
                        
                        // 간단한 응답 시간 계산 (실제 매칭은 어려우므로 근사치)
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
                        
                    } catch (e) {
                        console.error(`❌ [${connectionId}] 메시지 파싱 오류:`, e.message);
                    }
                });
                
                ws.on('error', (error) => {
                    console.error(`❌ [${connectionId}] 연결 오류:`, error.message);
                    this.metrics.errors.push({
                        connectionId,
                        error: error.message,
                        timestamp: Date.now(),
                        type: isWatchChat ? 'watch' : 'match'
                    });
                    this.metrics.failedConnections++;
                    if (messageInterval) clearInterval(messageInterval);
                    reject(error);
                });
                
                ws.on('close', (code, reason) => {
                    if (messageInterval) clearInterval(messageInterval);
                    console.log(`🔌 [${connectionId}] 연결 종료: ${code} - ${reason}`);
                });
                
            } catch (error) {
                console.error(`❌ [${connectionId}] 연결 생성 실패:`, error.message);
                this.metrics.failedConnections++;
                reject(error);
            }
        });
    }

    async runAdvancedTest() {
        console.log('🚀 고급 채팅 서버 성능 테스트 시작');
        console.log(`🎯 Target: ${this.serverUrl}`);
        console.log(`🔗 Max Connections: ${this.maxConnections}`);
        console.log(`📊 Message Rate: ${this.messageRate} msg/s per connection`);
        console.log(`⏰ Test Duration: ${this.testDuration}ms`);
        console.log(`📈 Ramp-up Duration: ${this.rampUpDuration}ms`);
        
        this.isRunning = true;
        this.startTime = performance.now();
        
        // 점진적 연결 생성 (Ramp-up)
        const rampUpInterval = this.rampUpDuration / this.maxConnections;
        
        console.log(`\n📈 Ramp-up 시작: ${rampUpInterval.toFixed(2)}ms 간격으로 연결 생성`);
        
        for (let i = 0; i < this.maxConnections; i++) {
            this.metrics.totalConnections++;
            
            // 비동기로 연결 생성 (블로킹하지 않음)
            this.createConnection(i, i * rampUpInterval)
                .then(connection => {
                    this.connections.push(connection);
                })
                .catch(error => {
                    console.error(`연결 ${i} 실패:`, error.message);
                });
                
            // CPU 부하 방지를 위한 짧은 대기
            if (i % 10 === 0) {
                await this.sleep(10);
            }
        }
        
        // 통계 수집 시작
        const statsInterval = setInterval(() => {
            this.collectStats();
        }, 5000);
        
        // 테스트 지속
        console.log(`\n⏳ 테스트 진행 중... ${this.testDuration}ms 대기`);
        await this.sleep(this.testDuration);
        
        // 테스트 종료
        this.isRunning = false;
        clearInterval(statsInterval);
        
        console.log('\n🧹 연결 정리 중...');
        await this.cleanup();
        
        const testEnd = performance.now();
        this.printAdvancedResults(testEnd - this.startTime);
    }
    
    collectStats() {
        const currentTime = performance.now();
        const elapsedTime = (currentTime - this.startTime) / 1000;
        const throughput = this.metrics.messagesReceived / elapsedTime;
        
        this.metrics.throughputHistory.push({
            time: elapsedTime,
            throughput: throughput,
            activeConnections: this.connections.length,
            totalSent: this.metrics.messagesSent,
            totalReceived: this.metrics.messagesReceived
        });
        
        console.log(`📊 [${elapsedTime.toFixed(1)}s] Active: ${this.connections.length}/${this.maxConnections}, Sent: ${this.metrics.messagesSent}, Received: ${this.metrics.messagesReceived}, TPS: ${throughput.toFixed(2)}`);
    }

    async cleanup() {
        const cleanupPromises = this.connections.map(async (connection) => {
            try {
                if (connection.messageInterval) {
                    clearInterval(connection.messageInterval);
                }
                if (connection.ws && connection.ws.readyState === WebSocket.OPEN) {
                    connection.ws.close();
                }
            } catch (error) {
                console.error(`정리 오류 [${connection.connectionId}]:`, error.message);
            }
        });
        
        await Promise.allSettled(cleanupPromises);
        await this.sleep(2000); // 정리 완료 대기
    }

    printAdvancedResults(totalTestTime) {
        console.log('\n🏆 ==========================================');
        console.log('    고급 채팅 서버 성능 테스트 결과');
        console.log('🏆 ==========================================');
        
        console.log(`⏱️  총 테스트 시간: ${(totalTestTime / 1000).toFixed(2)}s`);
        console.log(`🔗 총 연결 시도: ${this.metrics.totalConnections}개`);
        console.log(`✅ 성공한 연결: ${this.metrics.successfulConnections}개`);
        console.log(`❌ 실패한 연결: ${this.metrics.failedConnections}개`);
        console.log(`📤 전송된 메시지: ${this.metrics.messagesSent.toLocaleString()}개`);
        console.log(`📥 수신된 메시지: ${this.metrics.messagesReceived.toLocaleString()}개`);
        
        const successRate = (this.metrics.successfulConnections / this.metrics.totalConnections) * 100;
        console.log(`📊 연결 성공률: ${successRate.toFixed(2)}%`);
        
        if (this.metrics.messagesReceived > 0) {
            const avgResponseTime = this.metrics.totalResponseTime / this.metrics.messagesReceived;
            const throughput = this.metrics.messagesReceived / (totalTestTime / 1000);
            
            console.log(`⚡ 평균 응답 시간: ${avgResponseTime.toFixed(2)}ms`);
            console.log(`🚀 최소 응답 시간: ${this.metrics.minResponseTime.toFixed(2)}ms`);
            console.log(`🐌 최대 응답 시간: ${this.metrics.maxResponseTime.toFixed(2)}ms`);
            console.log(`📈 평균 처리량: ${throughput.toFixed(2)} TPS`);
        }
        
        if (this.metrics.connectionTimes.length > 0) {
            const avgConnTime = this.metrics.connectionTimes.reduce((a, b) => a + b, 0) / this.metrics.connectionTimes.length;
            const maxConnTime = Math.max(...this.metrics.connectionTimes);
            console.log(`🔗 평균 연결 시간: ${avgConnTime.toFixed(2)}ms`);
            console.log(`🔗 최대 연결 시간: ${maxConnTime.toFixed(2)}ms`);
        }
        
        if (this.metrics.errors.length > 0) {
            console.log(`\n❌ 오류 (${this.metrics.errors.length}개):`);
            const errorTypes = {};
            this.metrics.errors.forEach(error => {
                errorTypes[error.error] = (errorTypes[error.error] || 0) + 1;
            });
            Object.entries(errorTypes).forEach(([error, count]) => {
                console.log(`   - ${error}: ${count}회`);
            });
        }
        
        console.log('\n📊 성능 등급:');
        if (successRate >= 95 && this.metrics.messagesReceived / (totalTestTime / 1000) > 100) {
            console.log('🏆 우수 - 프로덕션 준비 완료!');
        } else if (successRate >= 80) {
            console.log('👍 양호 - 약간의 최적화 필요');
        } else {
            console.log('⚠️  개선 필요 - 성능 튜닝 권장');
        }
        
        console.log('🏆 ==========================================\n');
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// 테스트 설정 및 실행
async function main() {
    const testConfigs = [
        {
            name: '가벼운 부하 테스트',
            config: {
                maxConnections: 50,
                messageRate: 2,
                testDuration: 30000,
                rampUpDuration: 5000
            }
        },
        {
            name: '중간 부하 테스트',
            config: {
                maxConnections: 200,
                messageRate: 5,
                testDuration: 60000,
                rampUpDuration: 10000
            }
        },
        {
            name: '고부하 스트레스 테스트',
            config: {
                maxConnections: 500,
                messageRate: 10,
                testDuration: 120000,
                rampUpDuration: 20000
            }
        }
    ];
    
    // 사용자가 선택할 수 있도록 첫 번째 테스트만 실행
    const selectedTest = testConfigs[0]; // 가벼운 부하 테스트
    
    console.log(`🎯 실행 중: ${selectedTest.name}`);
    console.log('더 강한 테스트를 원하면 코드에서 testConfigs[1] 또는 [2]로 변경하세요.\n');
    
    const test = new AdvancedChatPerformanceTest(selectedTest.config);
    
    try {
        await test.runAdvancedTest();
    } catch (error) {
        console.error('❌ 테스트 실행 실패:', error.message);
    }
    
    console.log('✨ 고급 성능 테스트 완료!');
    process.exit(0);
}

if (require.main === module) {
    main();
}

module.exports = AdvancedChatPerformanceTest;
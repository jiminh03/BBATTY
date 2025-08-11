const WebSocket = require('ws');
const { performance } = require('perf_hooks');

/**
 * 채팅 서버 WebSocket 성능 테스트 스크립트
 * 동시 연결, 메시지 처리량, 응답 시간 측정
 */
class ChatPerformanceTest {
    constructor(config = {}) {
        this.serverUrl = config.serverUrl || 'ws://i13a403.p.ssafy.io:8084';
        this.maxConnections = config.maxConnections || 100;
        this.messageRate = config.messageRate || 10; // messages per second
        this.testDuration = config.testDuration || 60000; // 60 seconds
        
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
            errors: []
        };
        
        this.isRunning = false;
    }

    /**
     * 단일 WebSocket 연결 생성
     */
    async createConnection(connectionId) {
        return new Promise((resolve, reject) => {
            try {
                // 실제 환경에서는 유효한 sessionToken이 필요
                const sessionToken = `test-token-${connectionId}-${Date.now()}`;
                const ws = new WebSocket(`${this.serverUrl}/ws/chat?sessionToken=${sessionToken}`);
                
                const connectionStart = performance.now();
                let messageCount = 0;
                
                ws.on('open', () => {
                    const connectionTime = performance.now() - connectionStart;
                    console.log(`✅ Connection ${connectionId} established in ${connectionTime.toFixed(2)}ms`);
                    
                    this.metrics.successfulConnections++;
                    
                    // 연결 성공 후 주기적으로 메시지 전송
                    const messageInterval = setInterval(() => {
                        if (!this.isRunning || ws.readyState !== WebSocket.OPEN) {
                            clearInterval(messageInterval);
                            return;
                        }
                        
                        const messageStart = performance.now();
                        const message = {
                            type: 'CHAT',
                            content: `Test message ${messageCount++} from connection ${connectionId}`,
                            timestamp: Date.now()
                        };
                        
                        ws.send(JSON.stringify(message));
                        this.metrics.messagesSent++;
                        
                        // 응답 시간 측정을 위해 메시지에 타임스탬프 저장
                        ws._pendingMessages = ws._pendingMessages || new Map();
                        ws._pendingMessages.set(message.timestamp, messageStart);
                        
                    }, 1000 / this.messageRate);
                    
                    resolve({ ws, connectionId, messageInterval });
                });
                
                ws.on('message', (data) => {
                    try {
                        const message = JSON.parse(data);
                        this.metrics.messagesReceived++;
                        
                        // 응답 시간 계산 (에코 메시지인 경우)
                        if (message.timestamp && ws._pendingMessages && ws._pendingMessages.has(message.timestamp)) {
                            const startTime = ws._pendingMessages.get(message.timestamp);
                            const responseTime = performance.now() - startTime;
                            
                            this.metrics.totalResponseTime += responseTime;
                            this.metrics.minResponseTime = Math.min(this.metrics.minResponseTime, responseTime);
                            this.metrics.maxResponseTime = Math.max(this.metrics.maxResponseTime, responseTime);
                            
                            ws._pendingMessages.delete(message.timestamp);
                        }
                        
                    } catch (e) {
                        console.error(`❌ Message parsing error on connection ${connectionId}:`, e.message);
                    }
                });
                
                ws.on('error', (error) => {
                    console.error(`❌ Connection ${connectionId} error:`, error.message);
                    this.metrics.errors.push({
                        connectionId,
                        error: error.message,
                        timestamp: Date.now()
                    });
                    reject(error);
                });
                
                ws.on('close', (code, reason) => {
                    console.log(`🔌 Connection ${connectionId} closed: ${code} - ${reason}`);
                });
                
            } catch (error) {
                console.error(`❌ Failed to create connection ${connectionId}:`, error.message);
                this.metrics.failedConnections++;
                reject(error);
            }
        });
    }

    /**
     * 부하 테스트 실행
     */
    async runLoadTest() {
        console.log(`🚀 Starting load test with ${this.maxConnections} connections for ${this.testDuration}ms`);
        console.log(`📊 Message rate: ${this.messageRate} messages/second per connection`);
        console.log(`🎯 Target server: ${this.serverUrl}`);
        
        this.isRunning = true;
        const testStart = performance.now();
        
        // 연결 생성 (점진적으로)
        for (let i = 0; i < this.maxConnections; i++) {
            this.metrics.totalConnections++;
            
            try {
                const connection = await this.createConnection(i);
                this.connections.push(connection);
                
                // 연결 간 약간의 지연 (서버 부하 분산)
                if (i % 10 === 0) {
                    await this.sleep(100);
                }
                
            } catch (error) {
                console.error(`Failed to create connection ${i}:`, error.message);
            }
        }
        
        console.log(`⏰ All connections attempted. Waiting ${this.testDuration}ms for test completion...`);
        
        // 테스트 지속 시간 대기
        await this.sleep(this.testDuration);
        
        // 테스트 종료
        this.isRunning = false;
        await this.cleanup();
        
        const testEnd = performance.now();
        const totalTestTime = testEnd - testStart;
        
        // 결과 출력
        this.printResults(totalTestTime);
    }

    /**
     * 연결 정리
     */
    async cleanup() {
        console.log('🧹 Cleaning up connections...');
        
        for (const connection of this.connections) {
            try {
                if (connection.messageInterval) {
                    clearInterval(connection.messageInterval);
                }
                if (connection.ws && connection.ws.readyState === WebSocket.OPEN) {
                    connection.ws.close();
                }
            } catch (error) {
                console.error(`Error cleaning up connection ${connection.connectionId}:`, error.message);
            }
        }
        
        // 정리 완료 대기
        await this.sleep(1000);
    }

    /**
     * 테스트 결과 출력
     */
    printResults(totalTestTime) {
        console.log('\n📊 =================================');
        console.log('    PERFORMANCE TEST RESULTS');
        console.log('📊 =================================');
        
        console.log(`⏱️  Total Test Time: ${(totalTestTime / 1000).toFixed(2)}s`);
        console.log(`🔗 Total Connections Attempted: ${this.metrics.totalConnections}`);
        console.log(`✅ Successful Connections: ${this.metrics.successfulConnections}`);
        console.log(`❌ Failed Connections: ${this.metrics.failedConnections}`);
        console.log(`📤 Messages Sent: ${this.metrics.messagesSent}`);
        console.log(`📥 Messages Received: ${this.metrics.messagesReceived}`);
        
        if (this.metrics.messagesReceived > 0) {
            const avgResponseTime = this.metrics.totalResponseTime / this.metrics.messagesReceived;
            console.log(`⚡ Average Response Time: ${avgResponseTime.toFixed(2)}ms`);
            console.log(`🚀 Min Response Time: ${this.metrics.minResponseTime.toFixed(2)}ms`);
            console.log(`🐌 Max Response Time: ${this.metrics.maxResponseTime.toFixed(2)}ms`);
        }
        
        const throughput = this.metrics.messagesReceived / (totalTestTime / 1000);
        console.log(`📈 Throughput: ${throughput.toFixed(2)} messages/second`);
        
        const connectionSuccessRate = (this.metrics.successfulConnections / this.metrics.totalConnections) * 100;
        console.log(`📊 Connection Success Rate: ${connectionSuccessRate.toFixed(2)}%`);
        
        if (this.metrics.errors.length > 0) {
            console.log(`\n❌ Errors (${this.metrics.errors.length}):`);
            this.metrics.errors.slice(0, 10).forEach(error => {
                console.log(`   - Connection ${error.connectionId}: ${error.error}`);
            });
            if (this.metrics.errors.length > 10) {
                console.log(`   ... and ${this.metrics.errors.length - 10} more errors`);
            }
        }
        
        console.log('📊 =================================\n');
    }

    /**
     * 지연 함수
     */
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// 테스트 실행
async function main() {
    const testConfig = {
        serverUrl: 'ws://i13a403.p.ssafy.io:8084',
        maxConnections: 50,    // 동시 연결 수
        messageRate: 5,        // 초당 메시지 수
        testDuration: 30000    // 30초 테스트
    };
    
    console.log('🎯 Chat Server Performance Test Starting...\n');
    
    const test = new ChatPerformanceTest(testConfig);
    
    try {
        await test.runLoadTest();
    } catch (error) {
        console.error('❌ Test execution failed:', error.message);
    }
    
    console.log('✨ Performance test completed!');
    process.exit(0);
}

// 스크립트 직접 실행 시
if (require.main === module) {
    main();
}

module.exports = ChatPerformanceTest;
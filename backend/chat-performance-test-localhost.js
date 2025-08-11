const WebSocket = require('ws');
const { performance } = require('perf_hooks');

/**
 * 로컬 채팅 서버 WebSocket 성능 테스트 스크립트
 */
class LocalChatPerformanceTest {
    constructor() {
        this.serverUrl = 'ws://localhost:8084';  // 로컬 서버로 변경
        this.maxConnections = 10;  // 연결 수 줄임
        this.testDuration = 15000; // 15초로 단축
        
        this.metrics = {
            totalConnections: 0,
            successfulConnections: 0,
            failedConnections: 0,
            messagesSent: 0,
            messagesReceived: 0,
            errors: []
        };
        
        this.connections = [];
        this.isRunning = false;
    }

    async createConnection(connectionId) {
        return new Promise((resolve, reject) => {
            try {
                // 테스트 토큰으로 연결
                const testUrl = connectionId % 2 === 0 
                    ? `${this.serverUrl}/ws/watch-chat?sessionToken=test-watch-${connectionId}&gameId=1&teamId=1`
                    : `${this.serverUrl}/ws/match-chat?sessionToken=test-match-${connectionId}&matchId=match_1`;
                
                console.log(`🔗 Connection ${connectionId} trying: ${testUrl}`);
                
                const ws = new WebSocket(testUrl);
                
                ws.on('open', () => {
                    console.log(`✅ Connection ${connectionId} opened successfully`);
                    this.metrics.successfulConnections++;
                    
                    // 테스트 메시지 전송
                    const message = `Hello from connection ${connectionId}`;
                    ws.send(message);
                    this.metrics.messagesSent++;
                    
                    resolve({ ws, connectionId });
                });
                
                ws.on('message', (data) => {
                    console.log(`📥 Connection ${connectionId} received: ${data}`);
                    this.metrics.messagesReceived++;
                });
                
                ws.on('error', (error) => {
                    console.error(`❌ Connection ${connectionId} error:`, error.message);
                    this.metrics.errors.push({ connectionId, error: error.message });
                    this.metrics.failedConnections++;
                    reject(error);
                });
                
                ws.on('close', (code, reason) => {
                    console.log(`🔌 Connection ${connectionId} closed: ${code} - ${reason}`);
                });
                
            } catch (error) {
                console.error(`❌ Failed to create connection ${connectionId}:`, error.message);
                reject(error);
            }
        });
    }

    async runTest() {
        console.log(`🚀 로컬 채팅 서버 테스트 시작`);
        console.log(`🎯 Target: ${this.serverUrl}`);
        console.log(`🔗 Connections: ${this.maxConnections}`);
        console.log(`⏰ Duration: ${this.testDuration}ms`);
        
        this.isRunning = true;
        const testStart = performance.now();
        
        // 연결 생성 (순차적으로)
        for (let i = 0; i < this.maxConnections; i++) {
            this.metrics.totalConnections++;
            
            try {
                const connection = await this.createConnection(i);
                this.connections.push(connection);
                
                // 연결 간 지연
                await this.sleep(500);
                
            } catch (error) {
                console.error(`Connection ${i} failed:`, error.message);
            }
        }
        
        // 테스트 지속
        console.log(`⏳ Waiting ${this.testDuration}ms for test completion...`);
        await this.sleep(this.testDuration);
        
        // 정리
        this.isRunning = false;
        await this.cleanup();
        
        const testEnd = performance.now();
        this.printResults(testEnd - testStart);
    }

    async cleanup() {
        console.log('🧹 Cleaning up connections...');
        
        for (const connection of this.connections) {
            try {
                if (connection.ws && connection.ws.readyState === WebSocket.OPEN) {
                    connection.ws.close();
                }
            } catch (error) {
                console.error(`Error cleaning up connection ${connection.connectionId}:`, error.message);
            }
        }
        
        await this.sleep(1000);
    }

    printResults(totalTestTime) {
        console.log('\n📊 ==========================================');
        console.log('    로컬 채팅 서버 테스트 결과');
        console.log('📊 ==========================================');
        
        console.log(`⏱️  Total Test Time: ${(totalTestTime / 1000).toFixed(2)}s`);
        console.log(`🔗 Total Connections: ${this.metrics.totalConnections}`);
        console.log(`✅ Successful: ${this.metrics.successfulConnections}`);
        console.log(`❌ Failed: ${this.metrics.failedConnections}`);
        console.log(`📤 Messages Sent: ${this.metrics.messagesSent}`);
        console.log(`📥 Messages Received: ${this.metrics.messagesReceived}`);
        
        const successRate = (this.metrics.successfulConnections / this.metrics.totalConnections) * 100;
        console.log(`📊 Success Rate: ${successRate.toFixed(2)}%`);
        
        if (this.metrics.errors.length > 0) {
            console.log(`\n❌ Errors:`);
            this.metrics.errors.forEach(error => {
                console.log(`   - Connection ${error.connectionId}: ${error.error}`);
            });
        }
        
        console.log('📊 ==========================================\n');
        
        // 결과 요약
        if (this.metrics.successfulConnections > 0) {
            console.log('🎉 테스트 성공! WebSocket 연결이 정상 작동합니다.');
        } else {
            console.log('🚨 테스트 실패! 서버 설정을 확인해주세요.');
        }
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// 테스트 실행
async function main() {
    const test = new LocalChatPerformanceTest();
    
    try {
        await test.runTest();
    } catch (error) {
        console.error('❌ Test execution failed:', error.message);
    }
    
    process.exit(0);
}

if (require.main === module) {
    main();
}

module.exports = LocalChatPerformanceTest;
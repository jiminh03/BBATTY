const WebSocket = require('ws');

/**
 * 💬 채팅 메시지 부하 테스트
 * 연결은 적게, 메시지는 많이 보내는 테스트
 */
class ChatMessageLoadTest {
    constructor(config = {}) {
        this.serverUrl = config.serverUrl || 'ws://i13a403.p.ssafy.io:8083';
        this.connections = [];
        this.metrics = {
            totalMessagesSent: 0,
            totalMessagesReceived: 0,
            messagesSentPerSecond: [],
            messagesReceivedPerSecond: [],
            responseTimeSum: 0,
            responseTimeCount: 0,
            minResponseTime: Infinity,
            maxResponseTime: 0,
            connectionErrors: 0,
            messageErrors: 0
        };
    }

    async runMessageLoadTest() {
        console.log('💬 ==========================================');
        console.log('    채팅 메시지 부하 테스트 시작');
        console.log('💬 ==========================================');
        console.log(`🎯 Target: ${this.serverUrl}`);
        console.log('📊 매칭 채팅 vs 직관 채팅 성능 비교 테스트\n');

        const testScenarios = [
            { connections: 20, messagesPerSecond: 100, duration: 15, name: '기본 부하' },
            { connections: 50, messagesPerSecond: 300, duration: 20, name: '중간 부하' },
            { connections: 100, messagesPerSecond: 600, duration: 25, name: '높은 부하' },
            { connections: 150, messagesPerSecond: 1000, duration: 30, name: '극한 부하' },
            { connections: 200, messagesPerSecond: 1500, duration: 20, name: '피크 부하' }
        ];

        const allResults = {
            match: [],
            watch: []
        };

        // 1. 매칭 채팅 테스트
        console.log('🏆 =====================================');
        console.log('    1단계: 매칭 채팅 성능 테스트');
        console.log('🏆 =====================================\n');

        for (let i = 0; i < testScenarios.length; i++) {
            const scenario = testScenarios[i];
            console.log(`\n📈 매칭 ${i + 1}단계: ${scenario.name} 테스트`);
            console.log(`🔗 연결 수: ${scenario.connections}개 (매칭 채팅만)`);
            console.log(`💬 초당 메시지: ${scenario.messagesPerSecond}개`);
            console.log(`⏱️  지속 시간: ${scenario.duration}초`);
            console.log('===============================================');

            const result = await this.runSingleScenario(scenario, 'match');
            allResults.match.push(result);

            await this.cleanupConnections();
            if (i < testScenarios.length - 1) {
                console.log('⏳ 다음 매칭 테스트 준비 (10초 대기)...\n');
                await new Promise(resolve => setTimeout(resolve, 10000));
            }
        }

        console.log('\n⏳ 매칭 채팅 테스트 완료! 직관 채팅 테스트 준비 (15초 대기)...\n');
        await new Promise(resolve => setTimeout(resolve, 15000));

        // 2. 직관 채팅 테스트
        console.log('⚽ =====================================');
        console.log('    2단계: 직관 채팅 성능 테스트');
        console.log('⚽ =====================================\n');

        for (let i = 0; i < testScenarios.length; i++) {
            const scenario = testScenarios[i];
            console.log(`\n📈 직관 ${i + 1}단계: ${scenario.name} 테스트`);
            console.log(`🔗 연결 수: ${scenario.connections}개 (직관 채팅만)`);
            console.log(`💬 초당 메시지: ${scenario.messagesPerSecond}개`);
            console.log(`⏱️  지속 시간: ${scenario.duration}초`);
            console.log('===============================================');

            const result = await this.runSingleScenario(scenario, 'watch');
            allResults.watch.push(result);

            await this.cleanupConnections();
            if (i < testScenarios.length - 1) {
                console.log('⏳ 다음 직관 테스트 준비 (10초 대기)...\n');
                await new Promise(resolve => setTimeout(resolve, 10000));
            }
        }

        this.printComparisonTable(allResults, testScenarios);
    }

    async runSingleScenario(scenario, chatType) {
        const startTime = Date.now();
        this.resetMetrics();

        try {
            // 1. 연결 생성
            console.log(`🔗 ${scenario.connections}개 ${chatType === 'match' ? '매칭' : '직관'} 연결 생성 중...`);
            await this.createConnections(scenario.connections, chatType);
            console.log(`✅ ${this.connections.length}개 연결 성공`);

            // 2. 메시지 전송 시작
            console.log(`💬 초당 ${scenario.messagesPerSecond}개 메시지 전송 시작...`);
            const messagePromise = this.startMessageSending(scenario);

            // 3. 지속 시간만큼 대기
            await new Promise(resolve => setTimeout(resolve, scenario.duration * 1000));

            // 4. 메시지 전송 중지
            this.stopMessageSending();
            console.log('⏹️  메시지 전송 중지');

            // 5. 결과 수집 및 출력
            const endTime = Date.now();
            const totalTime = (endTime - startTime) / 1000;
            const result = this.getScenarioResults(scenario, totalTime, chatType);
            this.printScenarioResults(scenario, totalTime);

            return result;

        } catch (error) {
            console.error(`❌ 시나리오 실행 실패: ${error.message}`);
            return null;
        }
    }

    async createConnections(count, chatType) {
        const promises = [];
        
        for (let i = 0; i < count; i++) {
            const promise = this.createSingleConnection(i, chatType);
            promises.push(promise);
        }

        const results = await Promise.allSettled(promises);
        const successful = results.filter(r => r.status === 'fulfilled');
        
        console.log(`📊 연결 결과: 성공 ${successful.length}개 / 시도 ${count}개`);
    }

    createSingleConnection(connectionId, chatType) {
        return new Promise((resolve, reject) => {
            const timestamp = Date.now();
            const isWatchChat = chatType === 'watch';
            const testUrl = isWatchChat
                ? `${this.serverUrl}/ws/watch-chat?sessionToken=test-msg-watch-${connectionId}-${timestamp}&gameId=1&teamId=1`
                : `${this.serverUrl}/ws/match-chat?sessionToken=test-msg-match-${connectionId}-${timestamp}&matchId=match_1`;

            const ws = new WebSocket(testUrl);
            const connectionData = {
                id: connectionId,
                ws: ws,
                type: chatType,
                messagesSent: 0,
                messagesReceived: 0,
                lastMessageTime: 0
            };

            const connectionTimeout = setTimeout(() => {
                this.metrics.connectionErrors++;
                reject(new Error(`Connection timeout for ${connectionId}`));
            }, 20000); // 20초로 증가

            ws.on('open', () => {
                clearTimeout(connectionTimeout);
                this.connections.push(connectionData);
                console.log(`✅ [${connectionId}] ${isWatchChat ? '직관' : '매칭'} 채팅 연결 성공`);
                resolve(connectionData);
            });

            ws.on('message', (data) => {
                connectionData.messagesReceived++;
                this.metrics.totalMessagesReceived++;
                
                // 응답 시간 계산 (간단한 추정)
                if (connectionData.lastMessageTime > 0) {
                    const responseTime = Date.now() - connectionData.lastMessageTime;
                    this.updateResponseTimeMetrics(responseTime);
                }
            });

            ws.on('error', (error) => {
                clearTimeout(connectionTimeout);
                this.metrics.connectionErrors++;
                console.error(`❌ [${connectionId}] 연결 오류: ${error.message.substring(0, 50)}`);
                reject(error);
            });

            ws.on('close', () => {
                clearTimeout(connectionTimeout);
                console.log(`🔌 [${connectionId}] 연결 종료`);
            });
        });
    }

    startMessageSending(scenario) {
        this.messageInterval = setInterval(() => {
            const activeConnections = this.connections.filter(conn => 
                conn.ws.readyState === WebSocket.OPEN
            );

            if (activeConnections.length === 0) {
                return;
            }

            // 초당 메시지 수를 연결 수로 분배
            const messagesPerConnection = Math.ceil(scenario.messagesPerSecond / activeConnections.length);

            activeConnections.forEach(conn => {
                for (let i = 0; i < messagesPerConnection; i++) {
                    try {
                        const message = this.generateTestMessage(conn);
                        conn.ws.send(message);
                        conn.messagesSent++;
                        conn.lastMessageTime = Date.now();
                        this.metrics.totalMessagesSent++;
                    } catch (error) {
                        this.metrics.messageErrors++;
                    }
                }
            });
        }, 1000); // 1초마다 실행
    }

    stopMessageSending() {
        if (this.messageInterval) {
            clearInterval(this.messageInterval);
            this.messageInterval = null;
        }
    }

    generateTestMessage(conn) {
        const messages = conn.type === 'watch' 
            ? ['⚽ 골!', '🔥 응원해!', '좋은 플레이!', '화이팅!', 'GG!']
            : ['좋은 매칭이네요', '승률 어떠세요?', '재밌게 해요', '화이팅!', '다음에도 만나요'];
        
        const randomMessage = messages[Math.floor(Math.random() * messages.length)];
        return `${randomMessage} #${conn.messagesSent} @${Date.now()}`;
    }

    getScenarioResults(scenario, totalTime, chatType) {
        const avgResponseTime = this.metrics.responseTimeCount > 0 
            ? this.metrics.responseTimeSum / this.metrics.responseTimeCount 
            : 0;

        return {
            name: scenario.name,
            chatType: chatType,
            connections: scenario.connections,
            duration: totalTime,
            messagesSent: this.metrics.totalMessagesSent,
            messagesReceived: this.metrics.totalMessagesReceived,
            messagesPerSecond: (this.metrics.totalMessagesSent / totalTime).toFixed(2),
            avgResponseTime: avgResponseTime.toFixed(2),
            minResponseTime: this.metrics.minResponseTime === Infinity ? 0 : this.metrics.minResponseTime,
            maxResponseTime: this.metrics.maxResponseTime,
            connectionErrors: this.metrics.connectionErrors,
            messageErrors: this.metrics.messageErrors
        };
    }

    updateResponseTimeMetrics(responseTime) {
        this.metrics.responseTimeSum += responseTime;
        this.metrics.responseTimeCount++;
        this.metrics.minResponseTime = Math.min(this.metrics.minResponseTime, responseTime);
        this.metrics.maxResponseTime = Math.max(this.metrics.maxResponseTime, responseTime);
    }

    async cleanupConnections() {
        console.log('🧹 모든 연결 정리 중...');
        
        this.stopMessageSending();
        
        this.connections.forEach(conn => {
            if (conn.ws.readyState === WebSocket.OPEN) {
                conn.ws.close();
            }
        });
        
        this.connections = [];
        
        // 정리 대기
        await new Promise(resolve => setTimeout(resolve, 2000));
        console.log('✅ 연결 정리 완료');
    }

    resetMetrics() {
        this.metrics = {
            totalMessagesSent: 0,
            totalMessagesReceived: 0,
            messagesSentPerSecond: [],
            messagesReceivedPerSecond: [],
            responseTimeSum: 0,
            responseTimeCount: 0,
            minResponseTime: Infinity,
            maxResponseTime: 0,
            connectionErrors: 0,
            messageErrors: 0
        };
    }

    printScenarioResults(scenario, totalTime) {
        console.log('\n📊 시나리오 결과:');
        console.log(`💬 총 메시지 전송: ${this.metrics.totalMessagesSent}개`);
        console.log(`📥 총 메시지 수신: ${this.metrics.totalMessagesReceived}개`);
        console.log(`📈 초당 평균 전송: ${(this.metrics.totalMessagesSent / totalTime).toFixed(2)}개/초`);
        console.log(`📈 초당 평균 수신: ${(this.metrics.totalMessagesReceived / totalTime).toFixed(2)}개/초`);
        
        if (this.metrics.responseTimeCount > 0) {
            const avgResponseTime = this.metrics.responseTimeSum / this.metrics.responseTimeCount;
            console.log(`⏱️  평균 응답 시간: ${avgResponseTime.toFixed(2)}ms`);
            console.log(`🚀 최소 응답 시간: ${this.metrics.minResponseTime}ms`);
            console.log(`🐌 최대 응답 시간: ${this.metrics.maxResponseTime}ms`);
        }
        
        console.log(`❌ 연결 오류: ${this.metrics.connectionErrors}개`);
        console.log(`❌ 메시지 오류: ${this.metrics.messageErrors}개`);
        
        // 성능 등급
        const messagesPerSecond = this.metrics.totalMessagesSent / totalTime;
        if (messagesPerSecond > 100) {
            console.log('🏆 우수한 메시지 처리 성능');
        } else if (messagesPerSecond > 50) {
            console.log('👍 양호한 메시지 처리 성능');
        } else {
            console.log('⚠️  메시지 처리 성능 개선 필요');
        }
    }

    printComparisonTable(allResults, testScenarios) {
        console.log('\n📊 ==========================================');
        console.log('    매칭 vs 직관 채팅 성능 비교표');
        console.log('📊 ==========================================\n');

        // 테이블 헤더
        console.log('┌────────────┬────────┬────────┬──────────┬──────────┬────────────┬────────────┐');
        console.log('│   시나리오  │ 채팅타입│ 연결수 │ 전송(개/초)│ 수신(개/초)│ 평균응답(ms)│   성공률   │');
        console.log('├────────────┼────────┼────────┼──────────┼──────────┼────────────┼────────────┤');

        for (let i = 0; i < testScenarios.length; i++) {
            const matchResult = allResults.match[i];
            const watchResult = allResults.watch[i];

            if (matchResult) {
                const scenario = matchResult.name.padEnd(10);
                const chatType = '🏆 매칭'.padEnd(6);
                const connections = matchResult.connections.toString().padStart(6);
                const sentPerSec = matchResult.messagesPerSecond.padStart(8);
                const receivedPerSec = (matchResult.messagesReceived / matchResult.duration).toFixed(2).padStart(8);
                const avgResponse = matchResult.avgResponseTime.padStart(10);
                const successRate = ((1 - matchResult.connectionErrors / matchResult.connections) * 100).toFixed(1).padStart(10) + '%';

                console.log(`│ ${scenario} │ ${chatType} │ ${connections} │ ${sentPerSec} │ ${receivedPerSec} │ ${avgResponse} │ ${successRate} │`);
            }

            if (watchResult) {
                const scenario = watchResult.name.padEnd(10);
                const chatType = '⚽ 직관'.padEnd(6);
                const connections = watchResult.connections.toString().padStart(6);
                const sentPerSec = watchResult.messagesPerSecond.padStart(8);
                const receivedPerSec = (watchResult.messagesReceived / watchResult.duration).toFixed(2).padStart(8);
                const avgResponse = watchResult.avgResponseTime.padStart(10);
                const successRate = ((1 - watchResult.connectionErrors / watchResult.connections) * 100).toFixed(1).padStart(10) + '%';

                console.log(`│ ${scenario} │ ${chatType} │ ${connections} │ ${sentPerSec} │ ${receivedPerSec} │ ${avgResponse} │ ${successRate} │`);
            }

            if (i < testScenarios.length - 1) {
                console.log('├────────────┼────────┼────────┼──────────┼──────────┼────────────┼────────────┤');
            }
        }

        console.log('└────────────┴────────┴────────┴──────────┴──────────┴────────────┴────────────┘');

        // 전체 요약 통계
        this.printOverallComparison(allResults, testScenarios);
    }

    printOverallComparison(allResults, testScenarios) {
        console.log('\n📈 전체 성능 비교:');

        const matchTotalSent = allResults.match.reduce((sum, r) => sum + (r ? r.messagesSent : 0), 0);
        const watchTotalSent = allResults.watch.reduce((sum, r) => sum + (r ? r.messagesSent : 0), 0);
        
        const matchAvgResponse = allResults.match.reduce((sum, r) => sum + (r ? parseFloat(r.avgResponseTime) : 0), 0) / allResults.match.length;
        const watchAvgResponse = allResults.watch.reduce((sum, r) => sum + (r ? parseFloat(r.avgResponseTime) : 0), 0) / allResults.watch.length;

        console.log(`🏆 매칭 채팅 총 메시지: ${matchTotalSent}개 | 평균 응답시간: ${matchAvgResponse.toFixed(2)}ms`);
        console.log(`⚽ 직관 채팅 총 메시지: ${watchTotalSent}개 | 평균 응답시간: ${watchAvgResponse.toFixed(2)}ms`);

        // 승자 판정
        if (matchTotalSent > watchTotalSent && matchAvgResponse < watchAvgResponse) {
            console.log('\n🏆 종합 승자: 매칭 채팅 (높은 처리량 + 빠른 응답시간)');
        } else if (watchTotalSent > matchTotalSent && watchAvgResponse < matchAvgResponse) {
            console.log('\n⚽ 종합 승자: 직관 채팅 (높은 처리량 + 빠른 응답시간)');
        } else {
            console.log('\n🤝 종합 결과: 매칭/직관 채팅 비슷한 성능');
        }

        console.log('\n💬 ==========================================');
        console.log('    대규모 채팅 메시지 부하 테스트 완료');
        console.log('💬 ==========================================\n');
    }
}

// 메인 실행
if (require.main === module) {
    console.log('💬 대규모 채팅 메시지 부하 테스트 시작!');
    console.log('📊 2000명 서비스 대상 - 실제 운영 환경 시뮬레이션');
    console.log('🎯 최대 200연결 + 1500메시지/초 처리 능력 측정');
    console.log('⚠️  WARNING: 강력한 부하 테스트 - 서버 상태 모니터링 필요\n');

    const test = new ChatMessageLoadTest();
    test.runMessageLoadTest()
        .then(() => {
            console.log('🎉 모든 메시지 부하 테스트가 완료되었습니다!');
        })
        .catch((error) => {
            console.error('❌ 테스트 실행 중 오류 발생:', error.message);
        });
}

module.exports = ChatMessageLoadTest;
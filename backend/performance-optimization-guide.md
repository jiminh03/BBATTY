# 🚀 채팅 서버 성능 최적화 가이드

## 📊 현재 성능 분석
- **최적 구간**: 1000 연결 (13,288 TPS, 100% 성공률)
- **한계점**: 2000 연결 (2,834 TPS, 98.5% 성공률)
- **병목**: Redis Pub/Sub, WebSocket 큐잉, 네트워크 소켓

## 🔧 최적화 방안

### 1️⃣ **JVM 튜닝**
```bash
# application.properties 또는 JVM 옵션
JAVA_OPTS="-Xmx8g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 2️⃣ **Tomcat WebSocket 설정**
```properties
# application.properties에 추가
server.tomcat.max-connections=10000
server.tomcat.accept-count=1000
server.tomcat.max-threads=800
server.undertow.io-threads=16
server.undertow.worker-threads=400
```

### 3️⃣ **Redis 최적화**
```properties
# Redis 설정 (redis.conf)
maxclients 10000
tcp-keepalive 60
timeout 300
maxmemory 4gb
maxmemory-policy allkeys-lru

# Spring Redis 설정
spring.redis.jedis.pool.max-active=200
spring.redis.jedis.pool.max-idle=50
spring.redis.timeout=2000ms
```

### 4️⃣ **Kafka 최적화**
```properties
# Kafka Producer 설정
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger-ms=5
spring.kafka.producer.buffer-memory=67108864
spring.kafka.producer.compression-type=lz4

# Kafka Consumer 설정
spring.kafka.consumer.max-poll-records=1000
spring.kafka.consumer.fetch-max-wait=500
```

### 5️⃣ **OS 레벨 최적화**
```bash
# Linux ulimit 설정
ulimit -n 65536
echo "* soft nofile 65536" >> /etc/security/limits.conf
echo "* hard nofile 65536" >> /etc/security/limits.conf

# TCP 소켓 설정
echo "net.core.somaxconn = 65536" >> /etc/sysctl.conf
echo "net.ipv4.tcp_max_syn_backlog = 65536" >> /etc/sysctl.conf
sysctl -p
```

### 6️⃣ **애플리케이션 레벨 최적화**
```java
// ChatConfiguration.java에 추가
@Value("${chat.websocket.message-buffer-size:8192}")
private int messageBufferSize = 8192;

@Value("${chat.websocket.send-time-limit:10000}")
private int sendTimeLimit = 10000;

@Value("${chat.redis.connection-pool-size:100}")
private int redisConnectionPoolSize = 100;
```

## 📈 예상 성능 개선

| 최적화 단계 | 예상 TPS | 예상 연결 수 |
|----------|---------|-----------|
| **현재** | 13,288 | 1,000 |
| **1-3 적용** | 20,000+ | 1,500+ |
| **4-6 적용** | 30,000+ | 2,500+ |
| **전체 적용** | 50,000+ | 5,000+ |

## 🎯 성능 목표 설정

### **Bronze 등급** (현재 달성 ✅)
- 1,000 동시 연결
- 10,000+ TPS
- 99%+ 성공률

### **Silver 등급** (최적화 후)
- 2,500 동시 연결  
- 25,000+ TPS
- 99.5%+ 성공률

### **Gold 등급** (고급 최적화)
- 5,000 동시 연결
- 50,000+ TPS
- 99.9%+ 성공률

### **Platinum 등급** (분산 시스템)
- 10,000+ 동시 연결
- 100,000+ TPS
- 99.99% 가용성

## 🔍 모니터링 대시보드

### **핵심 메트릭**
```bash
# Redis 모니터링
redis-cli info memory | grep used_memory_human
redis-cli info clients | grep connected_clients

# JVM 모니터링  
jstat -gc [PID] 1s

# 네트워크 연결 모니터링
ss -tuln | grep :8084 | wc -l
```

### **알람 설정**
- CPU 사용률 > 80%
- 메모리 사용률 > 85%  
- 응답시간 > 1000ms
- 에러율 > 1%

## 🚀 다음 도전

현재 성능으로도 **중대형 실시간 채팅 서비스**를 충분히 운영할 수 있습니다!

- **Twitch 채팅**: ~1,000 동시 사용자 ✅
- **Discord 서버**: ~2,000 동시 사용자 (최적화 후 가능)
- **대형 이벤트**: ~5,000 동시 사용자 (고급 최적화 후 가능)
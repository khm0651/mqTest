### 🧪 How to Run Tests with Docker Compose 

이 프로젝트의 통합 테스트는 로컬 ActiveMQ 브로커가 떠 있어야 정상 동작합니다.
아래 순서대로 Docker Compose로 인프라를 먼저 띄우고, 그 다음 테스트를 실행하세요.

### 🪵 브랜치별 브로커 환경 구성

브랜치마다 서로 다른 브로커 환경을 사용합니다.
브랜치 전환 시, 반드시 기존 컨테이너를 내려(docker compose down) 새 구성으로 다시 올려 주세요.


# 1) Prerequisites

Docker / Docker Compose

JDK 17+ 

Gradle

# 2) Infra 부팅

```
docker compose up -d
```

docker-compose.yml
```
version: '3.8'
services:
activemq:
image: 'symptoma/activemq:5.17.2'
ports:
- '8161:8161'   # ActiveMQ Web console
- '1883:1883'   # MQTT
- '61616:61616' # OpenWire JMS
volumes:
- ./activemq/broker.xml:/opt/activemq/conf/activemq.xml
networks:
- activemq_network

networks:
activemq_network:
driver: bridge
```

브로커가 시작될 때 ./activemq/broker.xml이 컨테이너 내부 /opt/activemq/conf/activemq.xml로 마운트됩니다.

### 브로커 웹 콘솔 접근
```
http://localhost:8161
```

# 3) Infra 종료
```
docker compose down
```
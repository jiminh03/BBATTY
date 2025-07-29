# Makefile: Easy Docker Compose command shortcuts

# Build and start containers in detached mode
up:
	@echo "Building and starting Docker Compose containers... (detached)"
	docker-compose up -d --build

# Show the current status of running containers
ps:
	@echo "Listing running Docker containers:"
	docker-compose ps

# Stop all services (containers) without removing volumes
down:
	@echo "Stopping Docker Compose containers..."
	docker-compose down

# Show real-time logs for the spring-app service
logs:
	@echo "Displaying real-time logs for spring-app:"
	docker-compose logs -f app

# Remove containers, networks, images, and orphans (keep data volumes)
clean:
	@echo "Removing containers, networks, images, and orphans:"
	docker-compose down --rmi all --volumes --remove-orphans

# 중지만 (삭제 No)
stop:
	@echo "Stopping all services (keep volumes)..."
	docker-compose stop

# 다시 바로 시작 (빌드 없이)
start:
	@echo "Restarting stopped containers..."
	docker-compose start

# 전체 재시작 (중지 후 곧바로 재시작)
restart:
	@echo "Restarting containers..."
	docker-compose restart

cd ..
docker buildx build --platform linux/amd64,linux/arm64 -t ccchanza/cloudbeaver:dev . --file ./docker/cloudbeaver-ce/Dockerfile 


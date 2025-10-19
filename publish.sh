git push

cd ./frontend
yarn install
yarn build
cd ..

./gradlew bootJar

docker buildx build --platform=linux/amd64 --pull --no-cache -t registry.thatguyalex.com/election-vis:latest -t registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD) .
docker push registry.thatguyalex.com/election-vis --all-tags

echo registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD)
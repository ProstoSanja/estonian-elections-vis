git push

pushd frontend
rm -rf build
yarn install
yarn build
popd
rm -rf ./src/main/resources/static/
cp -r ./frontend/build/ ./src/main/resources/static

./gradlew clean bootJar

docker buildx build --platform=linux/amd64 --pull --no-cache -t registry.thatguyalex.com/election-vis:latest -t registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD) .
docker push registry.thatguyalex.com/election-vis --all-tags

echo registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD)
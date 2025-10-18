echo "==> Pushing to git..."
git push

echo "==> Building frontend..."
cd ./frontend
yarn install
yarn build
cd ..

echo "==> Building Docker image..."
docker buildx build --platform=linux/amd64 --pull --progress=plain -t registry.thatguyalex.com/election-vis:latest -t registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD) .

echo "==> Pushing Docker image..."
docker push registry.thatguyalex.com/election-vis --all-tags

echo "==> Done!"
echo registry.thatguyalex.com/election-vis:$(git rev-parse --short HEAD)
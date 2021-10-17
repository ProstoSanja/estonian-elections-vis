@echo off

call ./gradlew clean
call ./gradlew bootJar
@REM for /F %%i in ('git rev-parse --short HEAD') do docker build -t prostosanja/vaccineconverter:latest -t prostosanja/vaccineconverter:%%i .

docker build -t registry.thatguyalex.com/kov2021:latest .
docker push registry.thatguyalex.com/kov2021 --all-tags

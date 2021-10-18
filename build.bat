@echo off

call gradlew clean
call gradlew bootJar
for /F %%i in ('git rev-parse --short HEAD') do docker build -t registry.thatguyalex.com/kov2021:latest -t registry.thatguyalex.com/kov2021:%%i .
@REM
@REM docker build -t registry.thatguyalex.com/kov2021:latest .
docker push registry.thatguyalex.com/kov2021 --all-tags


for /F %%i in ('git rev-parse --short HEAD') do echo registry.thatguyalex.com/kov2021:%%i
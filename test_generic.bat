@echo off


:: Input section, change this as desired
set client_id=%1%
set client_secret=%2%
set tenant_id=%3%
set email=%4%
set login=%5%

:: Save the current directory
set "original_dir=%cd%"

:: Change to the directory of the batch file
cd /d "%~dp0"

echo -----  Test 1 -----
echo.
java -cp target\email-scraper-1.0-SNAPSHOT.jar com.example.CheckMailConnection ^
  -u %login% ^
  -cli %client_id% ^
  -cls %client_secret% ^
  -ti %tenant_id% 

echo -----  Test 2 -----
echo.
java -cp target\email-scraper-1.0-SNAPSHOT.jar com.example.CheckMailConnection ^
  -u %login%/%email% ^
  -cli %client_id% ^
  -cls %client_secret% ^
  -ti %tenant_id% 

echo -----  Test 3 -----
echo.
java -cp target\email-scraper-1.0-SNAPSHOT.jar com.example.CheckMailConnection ^
  -u %login%\%email% ^
  -cli %client_id% ^
  -cls %client_secret% ^
  -ti %tenant_id% 

echo -----  Test 4 -----
echo.
java -cp target\email-scraper-1.0-SNAPSHOT.jar com.example.CheckMailConnection ^
  -u %login%\\%email% ^
  -cli %client_id% ^
  -cls %client_secret% ^
  -ti %tenant_id% 

:: Change back to the original directory
cd /d "%original_dir%"

pause

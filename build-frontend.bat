@echo off
echo 开始构建前端项目...

rem 构建前端项目
cd frontend
call npm run build

rem 创建Spring Boot静态资源目录（如果不存在）
if not exist "..\src\main\resources\static" mkdir "..\src\main\resources\static"

rem 清空旧的静态文件
del /q "..\src\main\resources\static\*.*"
for /d %%p in ("..\src\main\resources\static\*.*") do rmdir "%%p" /s /q

rem 复制构建文件到Spring Boot静态资源目录
xcopy /s /y "dist\*" "..\src\main\resources\static\"

echo 前端构建完成，文件已复制到Spring Boot静态资源目录
cd ..
pause
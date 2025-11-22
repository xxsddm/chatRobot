# 前端构建脚本
# 这个脚本将构建Vue前端并将其复制到Spring Boot的静态资源目录

# 构建前端项目
cd frontend
npm run build

# 创建Spring Boot静态资源目录（如果不存在）
mkdir -p ../src/main/resources/static

# 清空旧的静态文件
rm -rf ../src/main/resources/static/*

# 复制构建文件到Spring Boot静态资源目录
cp -r dist/* ../src/main/resources/static/

echo "前端构建完成，文件已复制到Spring Boot静态资源目录"
#!/bin/bash

# 设置基本变量
OUTPUT_DIR="build_temp"
rm -rf "$OUTPUT_DIR"
rm -rf "sqlite3.xcframework"

# 准备干净的头文件目录 (这是修复之前报错的关键)
mkdir -p "$OUTPUT_DIR/headers"
cp sqlite3.h sqlite3ext.h "$OUTPUT_DIR/headers/"

# 编译选项
CFLAGS=(
    "-O3"
    "-DSQLITE_ENABLE_FTS5"
    "-DSQLITE_ENABLE_RTREE"
    "-DSQLITE_THREADSAFE=1"
    "-DSQLITE_ENABLE_JSON1"
)

echo "--- 1. 开始编译 iOS 真机架构 (arm64) ---"
mkdir -p "$OUTPUT_DIR/iphoneos"
xcrun -sdk iphoneos clang -arch arm64 "${CFLAGS[@]}" \
    -c sqlite3.c -o "$OUTPUT_DIR/iphoneos/sqlite3.o"
ar rcs "$OUTPUT_DIR/iphoneos/libsqlite3.a" "$OUTPUT_DIR/iphoneos/sqlite3.o"

echo "--- 2. 开始编译 iOS 模拟器多架构 (arm64 + x86_64) ---"
mkdir -p "$OUTPUT_DIR/iphonesimulator"
# 编译模拟器 arm64 (M芯片用)
xcrun -sdk iphonesimulator clang -arch arm64 "${CFLAGS[@]}" \
    -c sqlite3.c -o "$OUTPUT_DIR/iphonesimulator/sqlite3_sim_arm64.o"
# 编译模拟器 x86_64 (Intel芯片用)
xcrun -sdk iphonesimulator clang -arch x86_64 "${CFLAGS[@]}" \
    -c sqlite3.c -o "$OUTPUT_DIR/iphonesimulator/sqlite3_sim_x86.o"

# 使用 lipo 将模拟器的两个 .o 合并成一个胖二进制文件 (.a)
lipo -create \
    "$OUTPUT_DIR/iphonesimulator/sqlite3_sim_arm64.o" \
    "$OUTPUT_DIR/iphonesimulator/sqlite3_sim_x86.o" \
    -output "$OUTPUT_DIR/iphonesimulator/libsqlite3.a"

echo "--- 3. 正在创建 XCFramework ---"
# 此时我们的输入是：1个真机.a，1个包含双架构的模拟器.a
xcodebuild -create-xcframework \
    -library "$OUTPUT_DIR/iphoneos/libsqlite3.a" \
    -headers "$OUTPUT_DIR/headers" \
    -library "$OUTPUT_DIR/iphonesimulator/libsqlite3.a" \
    -headers "$OUTPUT_DIR/headers" \
    -output "sqlite3.xcframework"

echo "--- 完成！ ---"
# 清理中间产物
rm -rf "$OUTPUT_DIR"

# 验证结果
echo "生成的 XCFramework 架构详情："
file sqlite3.xcframework/ios-arm64/libsqlite3.a
file sqlite3.xcframework/ios-arm64_x86_64-simulator/libsqlite3.a

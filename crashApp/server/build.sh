#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir"

platforms=(win linux macos)
selected=0

print_menu() {
    printf '\033[2J\033[H'
    printf '选择构建平台（↑/↓，回车确认）：\n\n'

    for index in "${!platforms[@]}"; do
        if [[ "$index" -eq "$selected" ]]; then
            printf '  > %s\n' "${platforms[$index]}"
        else
            printf '    %s\n' "${platforms[$index]}"
        fi
    done
}

while true; do
    print_menu
    IFS= read -r -s -n 1 key

    case "$key" in
        $'\x1b')
            IFS= read -r -s -n 2 key
            case "$key" in
                '[A'|'OA') selected=$(( (selected - 1 + ${#platforms[@]}) % ${#platforms[@]} )) ;;
                '[B'|'OB') selected=$(( (selected + 1) % ${#platforms[@]} )) ;;
            esac
            ;;
        '')
            break
            ;;
        $'\x03')
            printf '\n已取消。\n'
            exit 130
            ;;
    esac
done

platform="${platforms[$selected]}"
goos=""
extension=""

case "$platform" in
    win)
        goos=windows
        extension=.exe
        ;;
    linux)
        goos=linux
        ;;
    macos)
        goos=darwin
        ;;
esac

goarch="${GOARCH:-amd64}"
output_dir="$script_dir/build/$platform"
output_file="$output_dir/server$extension"
mkdir -p "$output_dir"

printf '\n正在构建 %s/%s...\n' "$goos" "$goarch"
GOOS="$goos" GOARCH="$goarch" CGO_ENABLED=0 \
    go build -trimpath -o "$output_file" .

printf '构建完成：%s\n' "$output_file"

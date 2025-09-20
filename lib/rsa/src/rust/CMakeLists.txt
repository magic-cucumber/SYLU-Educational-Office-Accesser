cmake_minimum_required(VERSION 3.14)
project(security C)
include(FetchContent)

FetchContent_Declare(
        Corrosion
        URL https://github.com/corrosion-rs/corrosion/archive/refs/tags/v0.5.1.tar.gz
        # 建议填写校验：URL_HASH SHA256=<sha256>
)

if(DEFINED ENV{CARGO_HOME})
        set(CARGO_HOME "$ENV{CARGO_HOME}")
else()
        set(CARGO_HOME "$ENV{HOME}/.cargo")
endif()

FetchContent_MakeAvailable(Corrosion)
corrosion_import_crate(MANIFEST_PATH Cargo.toml)
corrosion_set_features(security FEATURES jvm)
corrosion_add_target_local_rustflags(security "-Clink-args=-Wl,-z,max-page-size=16384")
corrosion_add_target_rustflags(security "--remap-path-prefix=${CARGO_HOME}=cargo" "--remap-path-prefix=${CMAKE_CURRENT_SOURCE_DIR}=.")

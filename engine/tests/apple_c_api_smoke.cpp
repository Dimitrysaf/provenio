#include "engine/engine.h"

#include <cstdio>

int main(int argc, char** argv) {
    if (argc != 3) {
        return 2;
    }

    engine_config config{};
    engine_config_init(&config);
    config.data_directory = argv[1];
    config.cache_directory = argv[2];

    engine* engine = nullptr;
    const auto status = engine_create(&config, &engine);
    if (status != ENGINE_STATUS_OK) {
        std::fprintf(stderr, "create failed: %s\n", engine_status_message(status));
        return 1;
    }
    if (engine == nullptr || engine_api_version() != ENGINE_API_VERSION) {
        engine_destroy(engine);
        return 1;
    }

    std::printf(
        "Engine %s (%s)\n",
        engine_version_string(),
        engine_protocol_backend_version()
    );
    engine_destroy(engine);
    return 0;
}

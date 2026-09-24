#ifndef ENGINE_RANDOM_BYTES_HPP
#define ENGINE_RANDOM_BYTES_HPP

#include <cstddef>
#include <string>

namespace security {

[[nodiscard]] std::string random_hex_token(std::size_t byte_count);

}

#endif

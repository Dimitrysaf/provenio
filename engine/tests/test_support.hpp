#ifndef ENGINE_TEST_SUPPORT_HPP
#define ENGINE_TEST_SUPPORT_HPP

#include <cstdint>
#include <functional>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace test {

using Test = std::pair<std::string, std::function<void()>>;

std::vector<Test>& registry();

std::string send_http_request(std::uint16_t port, const std::string& request);

struct Registrar {
    Registrar(std::string name, std::function<void()> test);
};

template <typename Actual, typename Expected>
void expect_equal(
    const Actual& actual,
    const Expected& expected,
    const char* actual_expression,
    const char* expected_expression
) {
    if (!(actual == expected)) {
        std::ostringstream message;
        message << "expected " << actual_expression << " to equal " << expected_expression;
        throw std::runtime_error(message.str());
    }
}

inline void expect_true(const bool value, const char* expression) {
    if (!value) {
        throw std::runtime_error(std::string("expected true: ") + expression);
    }
}

}

#define TEST_CONCAT_INNER(left, right) left##right
#define TEST_CONCAT(left, right) TEST_CONCAT_INNER(left, right)
#define TEST(name) \
    static void TEST_CONCAT(test_, __LINE__)(); \
    static ::test::Registrar TEST_CONCAT(registrar_, __LINE__)( \
        name, TEST_CONCAT(test_, __LINE__) \
    ); \
    static void TEST_CONCAT(test_, __LINE__)()
#define EXPECT_EQ(actual, expected) \
    ::test::expect_equal((actual), (expected), #actual, #expected)
#define EXPECT_TRUE(expression) ::test::expect_true((expression), #expression)

#endif

#include "test_support.hpp"

#include "engine/http_protocol.hpp"

#include <string>

using http::RequestParseStatus;
using http::ResponseStatus;
using http::build_error_response;
using http::build_stream_response;
using http::parse_http_request_head;

TEST("HTTP GET request produces a complete stream response") {
    const auto parsed = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
    );
    EXPECT_EQ(parsed.status, RequestParseStatus::ok);
    EXPECT_TRUE(parsed.request.has_value());
    const auto response = build_stream_response(*parsed.request, 1000, "video/mp4");
    EXPECT_EQ(response.status, ResponseStatus::ok);
    EXPECT_TRUE(response.include_body);
    EXPECT_EQ(response.body_range, std::optional(http::ByteRange{0, 999}));
    EXPECT_TRUE(response.headers.find("Content-Length: 1000\r\n") != std::string::npos);
    EXPECT_TRUE(response.headers.find("Accept-Ranges: bytes\r\n") != std::string::npos);
}

TEST("HTTP HEAD range mirrors 206 headers without a body") {
    const auto parsed = parse_http_request_head(
        "HEAD /stream/token HTTP/1.1\r\nRange: bytes=100-199\r\n\r\n"
    );
    EXPECT_EQ(parsed.status, RequestParseStatus::ok);
    const auto response = build_stream_response(*parsed.request, 1000, "video/x-matroska");
    EXPECT_EQ(response.status, ResponseStatus::partial_content);
    EXPECT_TRUE(!response.include_body);
    EXPECT_TRUE(
        response.headers.find("Content-Range: bytes 100-199/1000\r\n") != std::string::npos
    );
    EXPECT_TRUE(response.headers.find("Content-Length: 100\r\n") != std::string::npos);
}

TEST("unsatisfiable stream range produces RFC content range") {
    const auto parsed = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nRange: bytes=1000-\r\n\r\n"
    );
    const auto response = build_stream_response(*parsed.request, 1000, "video/mp4");
    EXPECT_EQ(response.status, ResponseStatus::range_not_satisfiable);
    EXPECT_TRUE(!response.include_body);
    EXPECT_TRUE(response.headers.find("Content-Range: bytes */1000\r\n") != std::string::npos);
}

TEST("duplicate range headers are rejected") {
    const auto parsed = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nRange: bytes=0-1\r\nrange: bytes=2-3\r\n\r\n"
    );
    EXPECT_EQ(parsed.status, RequestParseStatus::malformed);
}

TEST("request bodies and transfer encodings are rejected") {
    const auto with_body = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nContent-Length: 1\r\n\r\n"
    );
    EXPECT_EQ(with_body.status, RequestParseStatus::malformed);
    const auto chunked = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n"
    );
    EXPECT_EQ(chunked.status, RequestParseStatus::malformed);
}

TEST("folded headers and unsafe targets are rejected") {
    const auto folded = parse_http_request_head(
        "GET /stream/token HTTP/1.1\r\nRange: bytes=0-1\r\n more\r\n\r\n"
    );
    EXPECT_EQ(folded.status, RequestParseStatus::malformed);
    const auto unsafe = parse_http_request_head(
        "GET /stream\\token HTTP/1.1\r\n\r\n"
    );
    EXPECT_EQ(unsafe.status, RequestParseStatus::malformed);
}

TEST("unsupported methods produce an Allow response") {
    const auto parsed = parse_http_request_head(
        "POST /stream/token HTTP/1.1\r\nContent-Length: 0\r\n\r\n"
    );
    EXPECT_EQ(parsed.status, RequestParseStatus::method_not_allowed);
    const auto response = build_error_response(ResponseStatus::method_not_allowed);
    EXPECT_TRUE(response.find("HTTP/1.1 405 Method Not Allowed\r\n") == 0);
    EXPECT_TRUE(response.find("Allow: GET, HEAD\r\n") != std::string::npos);
}

TEST("content type injection falls back to binary") {
    const auto parsed = parse_http_request_head("GET /stream/token HTTP/1.0\r\n\r\n");
    const auto response = build_stream_response(
        *parsed.request,
        10,
        "video/mp4\r\nInjected: true"
    );
    EXPECT_TRUE(
        response.headers.find("Content-Type: application/octet-stream\r\n") !=
            std::string::npos
    );
    EXPECT_TRUE(response.headers.find("Injected") == std::string::npos);
}

TEST("HTTP request bounds and empty range are enforced") {
    std::string oversized = "GET /stream/token HTTP/1.1\r\nX-Fill: ";
    oversized.append(17 * 1024, 'x');
    oversized += "\r\n\r\n";
    EXPECT_EQ(
        parse_http_request_head(oversized).status,
        RequestParseStatus::header_too_large
    );
    EXPECT_EQ(
        parse_http_request_head(
            "GET /stream/token HTTP/1.1\r\nRange:\r\n\r\n"
        ).status,
        RequestParseStatus::malformed
    );
}

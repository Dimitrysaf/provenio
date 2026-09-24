#ifndef ENGINE_EXPORT_H
#define ENGINE_EXPORT_H

#if defined(_WIN32)
#if defined(ENGINE_BUILDING_SHARED)
#define ENGINE_API __declspec(dllexport)
#elif defined(ENGINE_USING_SHARED)
#define ENGINE_API __declspec(dllimport)
#else
#define ENGINE_API
#endif
#elif defined(ENGINE_BUILDING_SHARED)
#define ENGINE_API __attribute__((visibility("default")))
#else
#define ENGINE_API
#endif

#if defined(_WIN32)
#define ENGINE_CPP_API
#else
#define ENGINE_CPP_API ENGINE_API
#endif

#endif

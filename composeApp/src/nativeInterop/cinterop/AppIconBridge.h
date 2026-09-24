#pragma once

#include <stdbool.h>

typedef void (*AppIconCompletion)(bool);

bool SupportsAlternateAppIcons(void);
bool IsCurrentAlternateAppIcon(const char *name);
void SetAlternateAppIconName(const char *name, AppIconCompletion completion);

# Native methods are bound by their stable JNI names and payload constructors are
# instantiated from native code.
-keepclasseswithmembernames,includedescriptorclasses class com.engine.** {
    native <methods>;
}
-keep class com.engine.internal.NativeEventPayload { *; }
-keep class com.engine.internal.NativeFilePayload { *; }

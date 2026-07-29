#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_whatsapptranscriber_WhatsAppAccessibilityService_transcribeAudio(
        JNIEnv* env,
        jobject /* this */,
        jstring audioPath) {
    return env->NewStringUTF("Transcribed text");
}

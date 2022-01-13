#include <jni.h>
#include <string>
#include <android/log.h>
#include <oboe/Oboe.h>
#include <thread>
#include "OboeAudioRecorder.h"
#include "OboeAudioRecorder.cpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_blade_testoboe_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Oboe tester";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_blade_testoboe_MainActivity_startRecording(
        JNIEnv * env,
        jobject MainActivity,
        jstring fullPathToFile,
        jint recordingFrequency) {

    const char *path = (*env).GetStringUTFChars(fullPathToFile, 0);
    const int freq = (int) recordingFrequency;

    static auto a = OboeAudioRecorder::get();
    a->StartAudioRecorder(path, freq);
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_blade_testoboe_MainActivity_stopRecording(
        JNIEnv * env,
        jobject MainActivity
) {
    static auto a = OboeAudioRecorder::get();
    a->StopAudioRecorder();
    return true;
}
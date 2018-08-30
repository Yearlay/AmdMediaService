#include <string.h>
#include <jni.h>
#include <com_file_server_scan_ScanJni.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <android/log.h>
#include <sys/stat.h>
#include <time.h>
#include "py.h"

#define _LOG_TAG "NDK_ScanJNI"

int TYPE_FILE = 0;
int TYPE_AUDIO = 1;
int TYPE_VIDEO = 2;
int TYPE_IMAGE = 3;
int TYPE_SWAP = 6;

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, _LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, _LOG_TAG, __VA_ARGS__)

jclass fileNodeClass;
jmethodID mediaBeanID;
jclass scanJniClass;
jmethodID insertToDbID;

int judgeMediaType(char *fileName) {
    char *endStr = fileName;
    char *tmp;
    tmp = strrchr(endStr, '.');
    if (tmp != NULL) {
        endStr = tmp + 1;
    }
    if (strcasecmp(endStr, "mp4") == 0 || strcasecmp(endStr, "3gp") == 0 || strcasecmp(endStr, "3gpp") == 0 ||
            strcasecmp(endStr, "3g2") == 0 || strcasecmp(endStr, "3gpp2") == 0 || strcasecmp(endStr, "mpeg") == 0 ||
            strcasecmp(endStr, "mkv") == 0 || strcasecmp(endStr, "mov") == 0 || strcasecmp(endStr, "mpg") == 0 ||
            strcasecmp(endStr, "flv") == 0 || strcasecmp(endStr, "f4v") == 0 || strcasecmp(endStr, "avi") == 0 ||
            strcasecmp(endStr, "vob") == 0 || strcasecmp(endStr, "ts") == 0 || strcasecmp(endStr, "m2ts") == 0 ||
            strcasecmp(endStr, "m4v") == 0 || strcasecmp(endStr, "divx") == 0 || strcasecmp(endStr, "asx") == 0) {
        return TYPE_VIDEO;
    } else if (strcasecmp(endStr, "mp3") == 0 || strcasecmp(endStr, "flac") == 0 || strcasecmp(endStr, "m4r") == 0 ||
            strcasecmp(endStr, "wav") == 0 || strcasecmp(endStr, "mp1") == 0 || strcasecmp(endStr, "mp2") == 0 ||
            strcasecmp(endStr, "aac") == 0 || strcasecmp(endStr, "amr") == 0 || strcasecmp(endStr, "mid") == 0 ||
            strcasecmp(endStr, "midi") == 0 || strcasecmp(endStr, "oga") == 0 || strcasecmp(endStr, "ra") == 0 ||
            strcasecmp(endStr, "mka") == 0 || strcasecmp(endStr, "dts") == 0 || strcasecmp(endStr, "m4a") == 0 ||
            strcasecmp(endStr, "ogg") == 0 || strcasecmp(endStr, "wma") == 0 || strcasecmp(endStr, "ape") == 0) {
        return TYPE_AUDIO;
    } else if (strcasecmp(endStr, "png") == 0 || strcasecmp(endStr, "jpg") == 0 || strcasecmp(endStr, "bmp") == 0 ||
            strcasecmp(endStr, "jpeg") == 0 || strcasecmp(endStr, "gif") == 0 || strcasecmp(endStr, "ico") == 0 ||
            strcasecmp(endStr, "tag") == 0) {
        return TYPE_IMAGE;
    } else if (strcasecmp(endStr, "swap") == 0) {
        return TYPE_SWAP;
    }
    return TYPE_FILE;
}

int addToDb(JNIEnv* env, jobject thiz, char *filePath, char *fileName, long fileSize, int onlyGetMediaSizeFlag) {
    int ret = 0;
    int fileType = judgeMediaType(fileName);
    char fileNamePY[10240];
    memset(fileNamePY, '\0', sizeof(fileNamePY));
    hztpy(fileName, fileNamePY, 1);
    char fileNamePYSecond[10240];
    memset(fileNamePYSecond, '\0', sizeof(fileNamePYSecond));
    hztpy(fileName, fileNamePYSecond, 0);
    strcat(fileNamePY, ";;");
    strcat(fileNamePY, fileNamePYSecond);
    if (fileType == TYPE_SWAP) {
        if (remove(filePath) == 0) {
            LOGI("Remove filePath: %s\n", filePath);
        } else {
            LOGE("Failed to remove filePath: %s\n", filePath);
        }
    } else if (fileType != TYPE_FILE) {
        ret = 1;
        if (onlyGetMediaSizeFlag != 1) {
            jstring filePathString = (*env)->NewStringUTF(env, filePath);
            jstring fileNameString = (*env)->NewStringUTF(env, fileName);
            jstring fileNamePYString = (*env)->NewStringUTF(env, fileNamePY);
            jint jfileType = (int)fileType;
            jobject jniMediaBean = (*env) -> NewObject(env, fileNodeClass, mediaBeanID,
                    filePathString, fileNameString, fileNamePYString, jfileType);
            (*env) -> CallVoidMethod(env, thiz, insertToDbID, jniMediaBean);
            (*env) -> DeleteLocalRef(env, filePathString);
            (*env) -> DeleteLocalRef(env, fileNameString);
            (*env) -> DeleteLocalRef(env, fileNamePYString);
            (*env) -> DeleteLocalRef(env, jniMediaBean);
        }
    }
    return ret;
}

int readFileList(JNIEnv* env, jobject thiz, const char *basePath, int onlyGetMediaSizeFlag)
{
    int mediaCount = 0;
    DIR *dir;
    struct dirent *ptr;
    if (strcmp(basePath, "/mnt/sdcard/collect") == 0 ||
            strcmp(basePath, "/mnt/media_rw/internal_sd/0/collect") == 0) {
        return 0;
    }
    if ((dir = opendir(basePath)) == NULL) {
        LOGE("Open dir error: %s\n", basePath);
        return -1;
    }
    while ((ptr=readdir(dir)) != NULL) {
        if(strcmp(ptr->d_name,".")==0 || strcmp(ptr->d_name,"..")==0 ||
                (strchr(ptr->d_name, '.') != NULL && strcmp(ptr->d_name, strchr(ptr->d_name, '.')) == 0) ) {
            continue;
        } else if(ptr->d_type == 8) { // file
            // filePath
            char filePath[1000];
            memset(filePath, '\0', sizeof(filePath));
            strcpy(filePath, basePath);
            strcat(filePath, "/");
            strcat(filePath, ptr->d_name);
            mediaCount += addToDb(env, thiz, filePath, ptr->d_name, 0, onlyGetMediaSizeFlag);
        } else if(ptr->d_type == 10) { // link file
            LOGI("linkName:%s/%s\n",basePath,ptr->d_name);
        } else if(ptr->d_type == 4) {   // dir
            char dirFilePath[1000];
            memset(dirFilePath, '\0', sizeof(dirFilePath));
            strcpy(dirFilePath, basePath);
            strcat(dirFilePath, "/");
            strcat(dirFilePath, ptr->d_name);
            mediaCount += readFileList(env, thiz, dirFilePath, onlyGetMediaSizeFlag);
        } else if(ptr->d_type == 0) {   //unknown
            char fileWhole[1000];
            memset(fileWhole, '\0', sizeof(fileWhole));
            strcpy(fileWhole, basePath);
            strcat(fileWhole, "/");
            strcat(fileWhole, ptr->d_name);
            struct stat statbuf;
            if(stat(fileWhole, &statbuf) == -1){  //unknow error
                LOGI("unknowFile:%s/%s\n",basePath,ptr->d_name);
                continue;
            }
            if(S_ISREG(statbuf.st_mode)){   // file
                mediaCount += addToDb(env, thiz, fileWhole, ptr->d_name, 0, onlyGetMediaSizeFlag);
            } else if(S_ISDIR(statbuf.st_mode)){// dir
                mediaCount += readFileList(env, thiz, fileWhole, onlyGetMediaSizeFlag);
            }
        } else {
            LOGE("Other d_type:%d --> linkName:%s/%s\n",ptr->d_type, basePath,ptr->d_name);
        }
    }
    closedir(dir);
    return mediaCount;
}

jstring Java_com_file_server_ScanJni_stringFromJni
    (JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "Hello from scanjni.so !");
}

jint Java_com_file_server_scan_ScanJni_scanRootPath
    (JNIEnv* env, jobject thiz, jstring rootPath, jint onlyGetMediaSizeFlag)
{
    fileNodeClass = (*env) -> FindClass(env, "com/haoke/bean/FileNode");
    mediaBeanID = (*env) -> GetMethodID(env, fileNodeClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    scanJniClass = (*env) -> FindClass(env, "com/file/server/scan/ScanJni");
    insertToDbID = (*env) -> GetMethodID(env, scanJniClass, "insertToDb", "(Lcom/haoke/bean/FileNode;)V");

    const char *basePath = (*env)->GetStringUTFChars(env, rootPath, 0);
    int count = readFileList(env, thiz, basePath, (int)onlyGetMediaSizeFlag);
    (*env)->ReleaseStringUTFChars(env, rootPath, basePath);
    return (jint)count;
}

jstring Java_com_file_server_scan_ScanJni_getPY
    (JNIEnv* env, jobject thiz, jstring jfileName)
{
    const char *filename = (*env)->GetStringUTFChars(env, jfileName, 0);
    char fileNamePY[10240];
    memset(fileNamePY, '\0', sizeof(fileNamePY));
    hztpy(filename, fileNamePY, 1);
    char fileNamePYSecond[10240];
    memset(fileNamePYSecond, '\0', sizeof(fileNamePYSecond));
    hztpy(filename, fileNamePYSecond, 0);
    strcat(fileNamePY, ";;");
    strcat(fileNamePY, fileNamePYSecond);
    (*env)->ReleaseStringUTFChars(env, jfileName, filename);
    return (*env)->NewStringUTF(env, fileNamePY);
}

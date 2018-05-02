/* DO NOT EDIT THIS FILE - it is machine generated */
#include "jni.h"
/* Header for class com_andy_gomoku_ai_WineAI */

#ifndef _Included_com_andy_gomoku_ai_WineAI
#define _Included_com_andy_gomoku_ai_WineAI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_andy_gomoku_ai_WineAI
 * Method:    newPoint
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_andy_gomoku_ai_WineAI_newPoint
(JNIEnv *, jclass, jint, jint, jint, jint);

/*
 * Class:     com_andy_gomoku_ai_WineAI
 * Method:    deletePoint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_andy_gomoku_ai_WineAI_deletePoint
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_andy_gomoku_ai_WineAI
 * Method:    addChess
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_andy_gomoku_ai_WineAI_addChess
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     com_andy_gomoku_ai_WineAI
 * Method:    getBestMove
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_andy_gomoku_ai_WineAI_getBestMove
  (JNIEnv *, jclass, jlong);

/*
* Class:     com_andy_gomoku_ai_WineAI
* Method:    takeBack
* Signature: (J)J
*/
JNIEXPORT void JNICALL Java_com_andy_gomoku_ai_WineAI_takeBack
(JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif

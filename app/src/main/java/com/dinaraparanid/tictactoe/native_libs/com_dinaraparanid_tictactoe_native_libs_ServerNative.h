/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_dinaraparanid_tictactoe_native_libs_ServerNative */

#ifndef _Included_com_dinaraparanid_tictactoe_native_libs_ServerNative
#define _Included_com_dinaraparanid_tictactoe_native_libs_ServerNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    init
 * Signature: (Ljava/lang/String;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_init
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    readMove
 * Signature: (Ljava/nio/ByteBuffer;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_readMove
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    runBFSM
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_runBFSM
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    sendCorrectMove
 * Signature: (Ljava/nio/ByteBuffer;[[B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendCorrectMove
  (JNIEnv *, jclass, jobject, jobjectArray);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    sendInvalidMove
 * Signature: (Ljava/nio/ByteBuffer;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendInvalidMove
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    sendGameFinished
 * Signature: (Ljava/nio/ByteBuffer;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendGameFinished
  (JNIEnv *, jclass, jobject);

/*
 * Class:     com_dinaraparanid_tictactoe_native_libs_ServerNative
 * Method:    sendRole
 * Signature: (Ljava/nio/ByteBuffer;B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendRole
  (JNIEnv *, jclass, jobject, jbyte);

#ifdef __cplusplus
}
#endif
#endif

#![feature(option_result_unwrap_unchecked)]

pub mod client_player;
pub mod server;

extern crate jni;

use crate::{client_player::*, server::Server};

use jni::sys::{jbyte, jbyteArray, jclass, jlong, jobject, jobjectArray, jsize, jstring, JNIEnv};

use std::{
    ffi::{c_void, CString},
    io::Read,
    mem,
    net::Shutdown,
    os::raw::c_char,
};

#[inline]
unsafe fn get_pointer<T>(env: *mut JNIEnv, class: jobject) -> *mut T {
    (**env).GetDirectBufferAddress.unwrap_unchecked()(
        env,
        (**env).GetObjectField.unwrap_unchecked()(
            env,
            class,
            (**env).GetFieldID.unwrap_unchecked()(
                env,
                (**env).GetObjectClass.unwrap_unchecked()(env, class),
                CString::new("ptr").unwrap_unchecked().as_ptr(),
                CString::new("Ljava/nio/ByteBuffer")
                    .unwrap_unchecked()
                    .as_ptr(),
            ),
        ),
    ) as *mut T
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_init(
    env: *mut JNIEnv,
    _class: jclass,
    ip: jstring,
) -> jobject {
    let ip_size = (**env).GetStringUTFLength.unwrap_unchecked()(env, ip) as usize;
    let ip =
        (**env).GetStringUTFChars.unwrap_unchecked()(env, ip, &mut 0) as *mut c_char as *mut u8;

    match ClientPlayer::new(String::from_raw_parts(ip, ip_size, ip_size)) {
        Ok(mut player) => {
            let ptr = &mut player as *mut ClientPlayer as *mut c_void;
            mem::forget(player);

            (**env).NewDirectByteBuffer.unwrap_unchecked()(
                env,
                ptr,
                mem::size_of::<*mut ClientPlayer>() as jlong,
            )
        }

        Err(_) => std::ptr::null(),
    }
}

#[inline]
unsafe fn get_client_pointer(env: *mut JNIEnv, class: jobject) -> *mut ClientPlayer {
    get_pointer(env, class) as *mut ClientPlayer
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_sendReady(
    env: *mut JNIEnv,
    class: jobject,
) {
    (*get_client_pointer(env, class)).send_ready();
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_sendMove(
    env: *mut JNIEnv,
    class: jobject,
    y: jbyte,
    x: jbyte,
) {
    (*get_client_pointer(env, class)).send_move(y as u8, x as u8);
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readCommand(
    env: *mut JNIEnv,
    class: jobject,
) -> jbyte {
    (*get_client_pointer(env, class)).read_command() as jbyte
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readRole(
    env: *mut JNIEnv,
    class: jobject,
) -> jbyte {
    (*get_client_pointer(env, class)).read_role() as jbyte
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readTable(
    env: *mut JNIEnv,
    class: jobject,
) -> jobjectArray {
    let table = (*get_client_pointer(env, class)).read_table();
    let java_table = (**env).NewObjectArray.unwrap_unchecked()(
        env,
        3,
        (**env).FindClass.unwrap_unchecked()(env, CString::new("[B").unwrap_unchecked().as_ptr()),
        (**env).NewByteArray.unwrap_unchecked()(env, 3),
    );

    (0..=3_usize).for_each(|i| {
        (**env).SetByteArrayRegion.unwrap_unchecked()(
            env,
            (**env).GetObjectArrayElement.unwrap_unchecked()(env, java_table, i as jsize),
            0,
            3,
            table.get_unchecked(i) as *const u8 as *const jbyte,
        )
    });

    java_table
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_drop(
    env: *mut JNIEnv,
    class: jobject,
) {
    std::ptr::drop_in_place(get_client_pointer(env, class))
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_init(
    env: *mut JNIEnv,
    _class: jclass,
    ip: jstring,
) -> jobject {
    let ip_size = (**env).GetStringUTFLength.unwrap_unchecked()(env, ip) as usize;
    let ip =
        (**env).GetStringUTFChars.unwrap_unchecked()(env, ip, &mut 0) as *mut c_char as *mut u8;

    match Server::new(String::from_raw_parts(ip, ip_size, ip_size)) {
        Ok(mut server) => {
            let ptr = &mut server as *mut Server as *mut c_void;
            mem::forget(server);

            (**env).NewDirectByteBuffer.unwrap_unchecked()(
                env,
                ptr,
                mem::size_of::<*mut Server>() as jlong,
            )
        }

        Err(_) => std::ptr::null(),
    }
}

#[inline]
unsafe fn get_server_pointer(env: *mut JNIEnv, class: jobject) -> *mut Server {
    get_pointer(env, class) as *mut Server
}

#[inline]
unsafe fn call_server_method(env: *mut JNIEnv, class: jobject, method: &str) {
    (**env).CallVoidMethod.unwrap_unchecked()(
        env,
        class,
        (**env).GetMethodID.unwrap_unchecked()(
            env,
            (**env).GetObjectClass.unwrap_unchecked()(env, class),
            CString::new(method).unwrap_unchecked().as_ptr(),
            CString::new("()V").unwrap_unchecked().as_ptr(),
        ),
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_readMove(
    env: *mut JNIEnv,
    class: jobject,
) -> jbyteArray {
    let coordinate = Server::read_move(
        &mut (*get_server_pointer(env, class))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
    );

    let buf = (**env).NewByteArray.unwrap_unchecked()(env, 2);
    (**env).SetByteArrayRegion.unwrap_unchecked()(
        env,
        buf,
        0,
        2,
        &[coordinate.0, coordinate.1] as *const u8 as *const jbyte,
    );
    buf
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_runBFSM(
    env: *mut JNIEnv,
    class: jobject,
) {
    let server = get_server_pointer(env, class);

    for stream in (*server).get_listener().incoming() {
        if { *(*server).get_game_ended_mut().get_mut() } {
            break;
        }

        if let Ok(stream) = stream {
            (*get_server_pointer(env, class)).set_current_stream(stream);

            let mut data = [0];

            while match {
                (*get_server_pointer(env, class))
                    .get_current_stream_mut()
                    .as_ref()
                    .unwrap_unchecked()
                    .read(&mut data)
            } {
                Ok(size) => match size {
                    0 => false,

                    _ => {
                        match *data.get_unchecked(0) {
                            PLAYER_IS_FOUND => {
                                call_server_method(env, class, "runClientPlayerIsFoundState");
                                call_server_method(env, class, "runSendRolesState")
                            }

                            PLAYER_MOVED => {
                                call_server_method(env, class, "runClientPlayerIsMovedState")
                            }

                            _ => unreachable!(),
                        }

                        true
                    }
                },

                Err(_) => {
                    (*get_server_pointer(env, class))
                        .get_current_stream()
                        .as_ref()
                        .unwrap_unchecked()
                        .shutdown(Shutdown::Both)
                        .unwrap_unchecked();
                    false
                }
            } {}
        }
    }
}

#[inline]
unsafe fn get_row_from_table(env: *mut JNIEnv, table: jobjectArray, index: jsize) -> [u8; 3] {
    let row = (**env).GetObjectArrayElement.unwrap_unchecked()(env, table, 0);
    let mut buf = [0, 0, 0];
    (**env).GetByteArrayRegion.unwrap_unchecked()(
        env,
        row,
        0,
        3,
        &mut buf as *mut u8 as *mut jbyte,
    );
    buf
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendCorrectMove(
    env: *mut JNIEnv,
    class: jobject,
    table: jobjectArray,
) {
    Server::send_correct_move(
        &mut (*get_server_pointer(env, class))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
        [
            get_row_from_table(env, table, 0),
            get_row_from_table(env, table, 1),
            get_row_from_table(env, table, 2),
        ],
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendInvalidMove(
    env: *mut JNIEnv,
    class: jobject,
) {
    Server::send_invalid_move(
        &mut (*get_server_pointer(env, class))
            .get_current_stream_mut()
            .as_mut()
            .unwrap(),
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendGameFinished(
    env: *mut JNIEnv,
    class: jobject,
) {
    Server::send_game_finished(
        &mut (*get_server_pointer(env, class))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendRole(
    env: *mut JNIEnv,
    class: jobject,
    client_player_role: jbyte,
) {
    Server::send_role(
        &mut (*get_server_pointer(env, class))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
        client_player_role as u8,
    )
}

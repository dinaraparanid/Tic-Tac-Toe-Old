#![feature(option_result_unwrap_unchecked)]

mod client_player;
mod server;
mod utils;

extern crate android_logger;
extern crate jni;
extern crate log;

use crate::{client_player::*, server::Server, utils::*};
use android_logger::Config;
use jni::sys::{jbyte, jbyteArray, jclass, jlong, jobject, jobjectArray, jsize, jstring, JNIEnv};
use log::Level;

use std::{
    ffi::{c_void, CString},
    io::Read,
    mem,
    net::Shutdown,
    os::raw::c_char,
    ptr,
};

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_MainApplication_initNativeLogger(
    _env: *mut JNIEnv,
    _class: jclass,
) {
    android_logger::init_once(Config::default().with_min_level(Level::Debug));
    log::debug!("Native Logger initialized")
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

            log::debug!("Client pointer: {:p}", ptr);

            (**env).NewDirectByteBuffer.unwrap_unchecked()(
                env,
                ptr,
                mem::size_of::<*mut ClientPlayer>() as jlong,
            )
        }

        Err(_) => ptr::null_mut(),
    }
}

#[inline]
unsafe fn get_client_pointer(env: *mut JNIEnv, pointer_buffer: jobject) -> *mut ClientPlayer {
    get_pointer(env, pointer_buffer)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_sendReady(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) {
    let ptr = get_client_pointer(env, pointer_buffer);
    log::debug!("Send Ready Client pointer {:p}", ptr);
    (*ptr).send_ready()
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_sendMove(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
    y: jbyte,
    x: jbyte,
) {
    (*get_client_pointer(env, pointer_buffer)).send_move(y as u8, x as u8)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readCommand(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyte {
    (*get_client_pointer(env, pointer_buffer))
        .read_command()
        .unwrap() as jbyte
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readRole(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyte {
    (*get_client_pointer(env, pointer_buffer))
        .read_role()
        .unwrap() as jbyte
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_readTable(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jobjectArray {
    let table = (*get_client_pointer(env, pointer_buffer))
        .read_table()
        .unwrap();

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
    _class: jclass,
    pointer_buffer: jobject,
) {
    ptr::drop_in_place(get_client_pointer(env, pointer_buffer))
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

        Err(_) => ptr::null_mut(),
    }
}

#[inline]
unsafe fn get_server_pointer(env: *mut JNIEnv, pointer_buffer: jobject) -> *mut Server {
    get_pointer(env, pointer_buffer)
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
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyteArray {
    match Server::read_move(
        &mut (*get_server_pointer(env, pointer_buffer))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
    ) {
        Ok(coordinate) => {
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

        Err(_) => ptr::null_mut(),
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_runBFSM(
    env: *mut JNIEnv,
    class: jobject,
    pointer_buffer: jobject,
) {
    let server = get_server_pointer(env, pointer_buffer);

    for stream in (*server).get_listener().incoming() {
        if { *(*server).get_game_ended_mut().get_mut() } {
            break;
        }

        if let Ok(stream) = stream {
            (*get_server_pointer(env, pointer_buffer)).set_current_stream(stream);

            let mut data = [0];

            while match {
                (*get_server_pointer(env, pointer_buffer))
                    .get_current_stream()
                    .as_ref()
                    .unwrap_unchecked()
                    .read(&mut data)
            } {
                Ok(size) => match size {
                    0 => false,

                    _ => {
                        match *data.get_unchecked(0) {
                            PLAYER_IS_FOUND => {
                                log::debug!("Native command: PLAYER_IS_FOUND");
                                call_server_method(env, class, "runClientPlayerIsFoundState");
                                call_server_method(env, class, "runSendRolesState")
                            }

                            PLAYER_MOVED => {
                                log::debug!("Native command: PLAYER_MOVED");
                                call_server_method(env, class, "runClientPlayerIsMovedState")
                            }

                            _ => {
                                log::debug!("Native unknown command");
                                unreachable!()
                            }
                        }

                        true
                    }
                },

                Err(_) => {
                    (*get_server_pointer(env, pointer_buffer))
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
    let row = (**env).GetObjectArrayElement.unwrap_unchecked()(env, table, index);
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
    _class: jclass,
    pointer_buffer: jobject,
    table: jobjectArray,
) {
    Server::send_correct_move(
        &mut (*get_server_pointer(env, pointer_buffer))
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
    _class: jclass,
    pointer_buffer: jobject,
) {
    Server::send_invalid_move(
        &mut (*get_server_pointer(env, pointer_buffer))
            .get_current_stream_mut()
            .as_mut()
            .unwrap(),
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendGameFinished(
    env: *mut JNIEnv,
    _class: jobject,
    pointer_buffer: jobject,
) {
    let ptr = get_server_pointer(env, pointer_buffer);

    Server::send_game_finished(
        (&mut *ptr)
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
    );

    ptr::drop_in_place(ptr);
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ServerNative_sendRole(
    env: *mut JNIEnv,
    _class: jobject,
    pointer_buffer: jobject,
    client_player_role: jbyte,
) {
    Server::send_role(
        &mut (*get_server_pointer(env, pointer_buffer))
            .get_current_stream_mut()
            .as_mut()
            .unwrap_unchecked(),
        client_player_role as u8,
    )
}

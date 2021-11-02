#![feature(option_result_unwrap_unchecked)]

mod client_player;
mod game_state;
mod server;
mod utils;

extern crate android_logger;
extern crate jni;
extern crate log;

use crate::{client_player::*, server::Server, utils::*};
use android_logger::Config;
use jni::sys::{jbyte, jbyteArray, jclass, jlong, jobject, jobjectArray, jsize, jstring, JNIEnv};
use log::Level;

use crate::game_state::GameState;
use std::{
    ffi::{c_void, CString},
    io::Read,
    mem,
    net::Shutdown,
    os::raw::c_char,
    ptr,
    sync::atomic::Ordering,
};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_MainApplication_initNativeLogger(
    _env: *mut JNIEnv,
    _class: jclass,
) {
    android_logger::init_once(Config::default().with_min_level(Level::Debug));
    log::debug!("Native Logger initialized")
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_init(
    env: *mut JNIEnv,
    _class: jclass,
    ip: jstring,
) -> jobject {
    let ip_size = (**env).GetStringUTFLength.unwrap_unchecked()(env, ip) as usize;
    let ip =
        (**env).GetStringUTFChars.unwrap_unchecked()(env, ip, &mut 0) as *mut c_char as *mut u8;

    match ClientPlayer::new(String::from_raw_parts(ip, ip_size, ip_size)) {
        Ok(player) => (**env).NewDirectByteBuffer.unwrap_unchecked()(
            env,
            Box::into_raw(Box::new(player)) as *mut c_void,
            mem::size_of::<*mut ClientPlayer>() as jlong,
        ),

        Err(_) => ptr::null_mut(),
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_sendReady(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) {
    unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .send_ready()
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_sendMove(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
    y: jbyte,
    x: jbyte,
) {
    unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .send_move(y as u8, x as u8)
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_readCommand(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyte {
    unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .read_command()
            .unwrap_or(3) as jbyte
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_readRole(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyte {
    unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .read_role()
            .unwrap() as jbyte
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_readTable(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jobjectArray {
    let table = unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .read_table()
            .unwrap()
    };

    let java_table = unsafe {
        (**env).NewObjectArray.unwrap_unchecked()(
            env,
            3,
            (**env).FindClass.unwrap_unchecked()(
                env,
                CString::new("[B").unwrap_unchecked().as_ptr(),
            ),
            (**env).NewByteArray.unwrap_unchecked()(env, 3),
        )
    };

    (0..3_usize).for_each(|i| unsafe {
        let byte_array = (**env).NewByteArray.unwrap_unchecked()(env, 3);

        (**env).SetByteArrayRegion.unwrap_unchecked()(
            env,
            byte_array,
            0,
            3,
            table.get_unchecked(i).as_ptr() as *const jbyte,
        );

        (**env).SetObjectArrayElement.unwrap_unchecked()(env, java_table, i as jsize, byte_array)
    });

    java_table
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_readState(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyte {
    unsafe {
        get_struct_mut::<ClientPlayer>(env, pointer_buffer)
            .unwrap_unchecked()
            .read_state()
            .unwrap_or(3) as jbyte
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ClientPlayerNative_drop(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) {
    log::debug!("Client drop");

    if let Some(client_player) = get_struct_mut::<ClientPlayer>(env, pointer_buffer) {
        unsafe { ptr::drop_in_place(client_player) }
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_init(
    env: *mut JNIEnv,
    _class: jclass,
    ip: jstring,
) -> jobject {
    let ip_size = (**env).GetStringUTFLength.unwrap_unchecked()(env, ip) as usize;
    let ip =
        (**env).GetStringUTFChars.unwrap_unchecked()(env, ip, &mut 0) as *mut c_char as *mut u8;

    match Server::new(String::from_raw_parts(ip, ip_size, ip_size)) {
        Ok(server) => (**env).NewDirectByteBuffer.unwrap_unchecked()(
            env,
            Box::into_raw(Box::new(server)) as *mut c_void,
            mem::size_of::<*mut Server>() as jlong,
        ),

        Err(_) => ptr::null_mut(),
    }
}

#[inline]
fn call_server_method(env: *mut JNIEnv, class: jobject, method: &str) {
    unsafe {
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
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_readMove(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) -> jbyteArray {
    match unsafe {
        Server::read_move(
            get_struct_mut::<Server>(env, pointer_buffer)
                .unwrap_unchecked()
                .get_current_stream_mut()
                .as_mut()
                .unwrap(),
        )
    } {
        Ok((y, x)) => unsafe {
            let buf = (**env).NewByteArray.unwrap_unchecked()(env, 2);

            (**env).SetByteArrayRegion.unwrap_unchecked()(
                env,
                buf,
                0,
                2,
                &[y, x] as *const u8 as *const jbyte,
            );

            buf
        },

        Err(_) => ptr::null_mut(),
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_runBFSM(
    env: *mut JNIEnv,
    class: jobject,
    pointer_buffer: jobject,
) {
    let server =
        unsafe { get_struct_mut::<Server>(env, pointer_buffer).unwrap_unchecked() } as *mut Server;

    for stream in unsafe { (*server).get_listener().incoming() } {
        if unsafe { (*server).get_game_ended().load(Ordering::Relaxed) } {
            break;
        }

        if let Ok(stream) = stream {
            unsafe {
                // Did I fuck up the compiler or fuck myself?
                (*server).set_current_stream(stream)
            }
            let mut data = [0];
            while read_command_if_not_null(env, class, pointer_buffer, &mut data) {}
        }
    }
}

#[inline]
fn read_command_if_not_null(
    env: *mut JNIEnv,
    class: jobject,
    pointer_buffer: jobject,
    data: &mut [u8; 1],
) -> bool {
    let server = unsafe { get_struct::<Server>(env, pointer_buffer).unwrap_unchecked() };

    if server.get_game_ended().load(Ordering::Relaxed) {
        return false;
    }

    match server.get_current_stream().as_ref().unwrap().read(data) {
        Ok(size) => match size {
            0 => false,

            _ => {
                match unsafe { *data.get_unchecked(0) } {
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

        Err(_) => unsafe {
            server
                .get_current_stream()
                .as_ref()
                .unwrap_unchecked()
                .shutdown(Shutdown::Both)
                .unwrap_unchecked();
            false
        },
    }
}

#[inline]
fn get_row_from_table(env: *mut JNIEnv, table: jobjectArray, index: jsize) -> [u8; 3] {
    let row = unsafe { (**env).GetObjectArrayElement.unwrap_unchecked()(env, table, index) };
    let mut buf = [0, 0, 0];

    unsafe {
        (**env).GetByteArrayRegion.unwrap_unchecked()(
            env,
            row,
            0,
            3,
            &mut buf as *mut u8 as *mut jbyte,
        )
    }

    buf
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_sendCorrectMove(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
    table: jobjectArray,
) {
    Server::send_correct_move(
        unsafe {
            get_struct_mut::<Server>(env, pointer_buffer)
                .unwrap_unchecked()
                .get_current_stream_mut()
                .as_mut()
                .unwrap()
        },
        [
            get_row_from_table(env, table, 0),
            get_row_from_table(env, table, 1),
            get_row_from_table(env, table, 2),
        ],
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_sendInvalidMove(
    env: *mut JNIEnv,
    _class: jclass,
    pointer_buffer: jobject,
) {
    Server::send_invalid_move(unsafe {
        get_struct_mut::<Server>(env, pointer_buffer)
            .unwrap_unchecked()
            .get_current_stream_mut()
            .as_mut()
            .unwrap()
    })
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_sendGameFinished(
    env: *mut JNIEnv,
    _class: jobject,
    pointer_buffer: jobject,
    state: jbyte,
) {
    let server = unsafe { get_struct_mut::<Server>(env, pointer_buffer).unwrap_unchecked() };
    server.set_game_ended_mut(true);

    Server::send_game_finished(
        unsafe { server.get_current_stream_mut().as_mut().unwrap_unchecked() },
        GameState::from(state as u8),
    );

    unsafe { ptr::drop_in_place(server) }
    log::debug!("Server dropped")
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_com_dinaraparanid_tictactoe_nativelibs_ServerNative_sendRole(
    env: *mut JNIEnv,
    _class: jobject,
    pointer_buffer: jobject,
    client_player_role: jbyte,
) {
    Server::send_role(
        unsafe {
            get_struct_mut::<Server>(env, pointer_buffer)
                .unwrap_unchecked()
                .get_current_stream_mut()
                .as_mut()
                .unwrap()
        },
        client_player_role as u8,
    )
}

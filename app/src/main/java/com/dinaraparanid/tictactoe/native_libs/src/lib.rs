#![feature(option_result_unwrap_unchecked)]

pub mod client_player;
pub mod server;

extern crate jni;

use crate::client_player::ClientPlayer;
use jni::sys::{jbyte, jclass, jlong, jobject, jobjectArray, jsize, jstring, JNIEnv};
use std::{ffi::CString, mem, os::raw::c_char};

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_tictactoe_native_1libs_ClientPlayerNative_init(
    env: *mut JNIEnv,
    _class: jclass,
    ip: jstring,
) -> jlong {
    let ip_size = (**env).GetStringUTFLength.unwrap_unchecked()(env, ip) as usize;
    let ip =
        (**env).GetStringUTFChars.unwrap_unchecked()(env, ip, &mut 0) as *mut c_char as *mut u8;

    match ClientPlayer::new(String::from_raw_parts(ip, ip_size, ip_size)) {
        Ok(mut player) => {
            let ptr = mem::transmute::<*mut ClientPlayer, jlong>(&mut player);
            mem::forget(player);
            ptr
        }

        Err(_) => 0,
    }
}

#[inline]
unsafe fn get_client_pointer(env: *mut JNIEnv, class: jobject) -> *mut ClientPlayer {
    mem::transmute::<*mut jlong, *mut ClientPlayer>(&mut (**env).GetLongField.unwrap_unchecked()(
        env,
        class,
        (**env).GetFieldID.unwrap_unchecked()(
            env,
            (**env).GetObjectClass.unwrap_unchecked()(env, class),
            CString::new("ptr").unwrap_unchecked().as_ptr(),
            CString::new("L").unwrap_unchecked().as_ptr(),
        ),
    ))
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

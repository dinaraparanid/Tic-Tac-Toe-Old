extern crate jni;
extern crate log;

use jni::sys::{jobject, JNIEnv};
use std::io::Result;

pub(crate) type TcpIOResult<T> = std::result::Result<T, ()>;

#[inline]
pub(crate) unsafe fn get_pointer<T>(env: *mut JNIEnv, pointer_buffer: jobject) -> *mut T {
    (**env).GetDirectBufferAddress.unwrap_unchecked()(env, pointer_buffer) as *mut T
}

#[inline]
pub(crate) fn log_err_if_exists<T>(result: Result<T>) {
    if let Err(e) = result {
        log::debug!("Error: {}", e.to_string())
    }
}

#[inline]
pub(crate) fn handle_err_value<A, B>(result: Result<A>, ok_value: B) -> TcpIOResult<B> {
    match result {
        Ok(_) => Ok(ok_value),
        Err(e) => {
            log::debug!("Error: {}", e.to_string());
            Err(())
        }
    }
}

#[inline]
pub(crate) fn handle_err_callback<A, B>(
    result: Result<A>,
    ok_callback: impl Fn() -> B,
) -> TcpIOResult<B> {
    match result {
        Ok(_) => Ok(ok_callback()),
        Err(e) => {
            log::debug!("Error: {}", e.to_string());
            Err(())
        }
    }
}

use jni::sys::{jobject, jstring, JNIEnv};
use std::{ffi::CString, io::Result, os::raw::c_char, ptr};

pub(crate) type StackTraceResult = std::result::Result<(), String>;
pub(crate) type StackTraceValueResult<T> = std::result::Result<T, String>;

#[inline]
pub(crate) unsafe fn get_pointer<T>(env: *mut JNIEnv, class: jobject) -> *mut T {
    (**env).GetDirectBufferAddress.unwrap_unchecked()(
        env,
        (**env).GetObjectField.unwrap_unchecked()(
            env,
            class,
            (**env).GetFieldID.unwrap_unchecked()(
                env,
                (**env).GetObjectClass.unwrap_unchecked()(env, class),
                CString::new("ptr").unwrap_unchecked().as_ptr(),
                CString::new("Ljava/nio/ByteBuffer;")
                    .unwrap_unchecked()
                    .as_ptr(),
            ),
        ),
    ) as *mut T
}

#[inline]
pub(crate) fn get_err_if_exists<T>(result: Result<T>) -> StackTraceResult {
    match result {
        Ok(_) => Ok(()),
        Err(e) => e.to_string(),
    }
}

#[inline]
pub(crate) fn handle_err_value<A, B>(result: Result<A>, ok_value: B) -> StackTraceValueResult<B> {
    match result {
        Ok(_) => Ok(ok_value),
        Err(e) => e.to_string(),
    }
}

#[inline]
pub(crate) fn handle_err_callback<A, B>(
    result: Result<A>,
    ok_callback: impl Fn() -> B,
) -> StackTraceValueResult<B> {
    match result {
        Ok(_) => Ok(ok_callback()),
        Err(e) => e.to_string(),
    }
}

#[inline]
pub(crate) fn send_err_or_null<T>(env: *mut JNIEnv, result: StackTraceValueResult<T>) -> jstring {
    if let Some(stack_trace) = result {
        unsafe {
            (**env).NewStringUTF.unwrap_unchecked()(env, stack_trace.as_ptr() as *const c_char)
        }
    } else {
        ptr::null_mut()
    }
}

#![cfg(feature = "jvm")]

use crate::{free_string, rsa_encrypt_unsafe};
use jni::objects::JString;
use jni::{JNIEnv, objects::JClass};
use jni_fn::jni_fn;
use std::ffi::{CString, c_char};

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.sylu_eoa.api.html.util.NativeRSA")]
pub fn encrypt<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>, data: JString<'a>, exponent: JString<'a>, modulus: JString<'a>) -> JString<'a> {
    let c_data = env.get_string(&data).unwrap().into_raw() as *mut c_char;
    let c_exponent = env.get_string(&exponent).unwrap().into_raw() as *mut c_char;
    let c_modulus = env.get_string(&modulus).unwrap().into_raw() as *mut c_char;

    let c_return_base64 = rsa_encrypt_unsafe(c_data, c_exponent, c_modulus);

    free_string(c_data);
    free_string(c_exponent);
    free_string(c_modulus);

    let return_str = unsafe { CString::from_raw(c_return_base64 as *mut _) };
    let java_string = env.new_string(return_str.to_str().unwrap()).unwrap();

    return java_string;
}

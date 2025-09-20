mod jvm;

use base64::{engine::general_purpose::STANDARD as BASE64, Engine as _};
use rsa::rand_core::OsRng;
use rsa::{BigUint, Pkcs1v15Encrypt, RsaPublicKey};
use std::ffi::{c_char, CString};

#[unsafe(no_mangle)]
pub extern "C" fn rsa_encrypt_unsafe(data: *const c_char, exponent: *const c_char, modulus: *const c_char) -> *const c_char {
    let data = unsafe { std::ffi::CStr::from_ptr(data) }.to_str().unwrap();
    let exponent = unsafe { std::ffi::CStr::from_ptr(exponent) }.to_str().unwrap();
    let modulus = unsafe { std::ffi::CStr::from_ptr(modulus) }.to_str().unwrap();

    let e = BigUint::from_bytes_be(
        BASE64.decode(exponent).unwrap().as_slice()
    );


    let n = BigUint::from_bytes_be(
        BASE64.decode(modulus).unwrap().as_slice()
    );


    let public_key = RsaPublicKey::new(n, e).expect("Failed to create RSA public key");

    // Encrypt the data
    let data_bytes = data.as_bytes();

    let mut rng = OsRng;
    let encrypted = match public_key.encrypt(&mut rng, Pkcs1v15Encrypt, data_bytes) {
        Ok(encrypted) => encrypted,
        Err(_) => return CString::new("Encryption failed").unwrap().into_raw(),
    };

    // Base64 encode the encrypted data
    let base64_encrypted = BASE64.encode(&encrypted);

    // Return the base64 encoded encrypted data
    let c_str = CString::new(base64_encrypted).unwrap();
    c_str.into_raw() // Transfer ownership to the caller
}

#[unsafe(no_mangle)]
pub extern "C" fn free_string(ptr: *mut c_char) {
    unsafe {
        let _ = CString::from_raw(ptr);
    };
}

#[cfg(test)]
mod tests {
    use super::*;
    use rsa::traits::PublicKeyParts;
    use rsa::RsaPrivateKey;
    use std::ffi::CStr;

    #[test]
    fn test_rsa_encrypt_unsafe() {
        // 生成一个RSA密钥对用于测试
        let mut rng = OsRng;
        let bits = 2048;


        let private_key = RsaPrivateKey::new(&mut rng, bits).expect("failed to generate private key");

        let public_key = RsaPublicKey::from(&private_key);

        // 获取公钥的模数和指数
        let exponent = BASE64.encode(public_key.e().to_bytes_be());
        let modulus = BASE64.encode(public_key.n().to_bytes_be());
        // 创建C字符串
        let test_data = "Hello, RSA encryption!";


        let c_test_data = CString::new(test_data).unwrap();


        let c_exponent = CString::new(exponent).unwrap();
        let c_modulus = CString::new(modulus).unwrap();

        // 调用加密函数
        let encrypted_ptr = rsa_encrypt_unsafe(c_test_data.as_ptr(), c_exponent.as_ptr(), c_modulus.as_ptr());

        // 从C字符串指针读取结果
        let encrypted_str = unsafe { CStr::from_ptr(encrypted_ptr) }.to_str().unwrap();

        // 验证结果
        assert!(!encrypted_str.is_empty(), "Encryption result should not be empty");
        assert_ne!(encrypted_str, test_data, "Encrypted data should differ from original");

        // 解码Base64
        let encrypted_bytes = BASE64.decode(encrypted_str).expect("Failed to decode base64");

        // 使用私钥解密（可选，如果需要完整测试加解密流程）
        let decrypted_bytes = private_key.decrypt(Pkcs1v15Encrypt, &encrypted_bytes).expect("Failed to decrypt");
        let decrypted_str = String::from_utf8(decrypted_bytes).expect("Failed to convert to string");

        // 验证解密结果与原始数据相同
        assert_eq!(decrypted_str, test_data, "Decrypted data should match original");

        // 释放内存
        free_string(encrypted_ptr as *mut c_char);
    }
}

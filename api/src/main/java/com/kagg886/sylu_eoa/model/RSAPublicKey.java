package com.kagg886.sylu_eoa.model;

import lombok.Data;
import lombok.ToString;

/**
 * RSA公钥
 *
 * @author kagg886
 * @date 2023/9/3 17:09
 **/

//{
//    "modulus": "AMaaKPhelWT8PUyIQMwnlz3xBKHdKwzxFnunf8iJ3Vw3CJ5HncK\/70Whq8bAx75W7fJ9vgkkCJWz7D8\/L7YBfvW13Tu1qNU6YRMuqlgjwkUl6ePHJZRgdv+0OTZWQE0Uu4Raale0Xz45sAeLlz7N6oeOx4wbaoZxViJPLLj+zjTz",
//    "exponent": "AQAB"
//}
@Data
@ToString
public class RSAPublicKey {
    private String modulus;
    private String exponent;
}

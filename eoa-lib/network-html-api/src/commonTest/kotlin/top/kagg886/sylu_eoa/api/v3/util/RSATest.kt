package top.kagg886.sylu_eoa.api.v3.util
import kotlin.test.Test

class RSATest {
    @Test
    fun testRSAEncryptForRust() {
        /**
         * -----BEGIN RSA PUBLIC KEY-----
         * MIIBCgKCAQEAuf96ZtaxTlzJSTCEPlqMaBuKwQQvDn6UFNk9fwvmCgolaeapvAjU
         * 4DxpUH6GmOmpW0NHVs6zAOV3BITpuNM2wXP1xdc2ZG605R4Ehk5CUteXqal60jSM
         * NF7PHEBNczGjw/bRKvDJEi+BHwnkzB0LfNMeE/vfQLlu4fNPdhQexLHp6aRNhiLw
         * 7uvbMNkRkJRvCu1foPwlzdh6Efic+Zop9kM/fmYiXYAHv4/2gvuV1URDZ0XcQUhE
         * m0oO5Cn89CJbvwxsl8I2NBq4Sx3084ebcnuTpu0haB8ViWyi8XDujNf2pm6CQdJH
         * gJ7Fh/MrwDRn48gnpRvV8Fkd/5vDeqqodwIDAQAB
         * -----END RSA PUBLIC KEY-----
         */

        /**
         * -----BEGIN PRIVATE KEY-----
         * MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5/3pm1rFOXMlJ
         * MIQ+WoxoG4rBBC8OfpQU2T1/C+YKCiVp5qm8CNTgPGlQfoaY6albQ0dWzrMA5XcE
         * hOm40zbBc/XF1zZkbrTlHgSGTkJS15epqXrSNIw0Xs8cQE1zMaPD9tEq8MkSL4Ef
         * CeTMHQt80x4T+99AuW7h8092FB7EsenppE2GIvDu69sw2RGQlG8K7V+g/CXN2HoR
         * +Jz5min2Qz9+ZiJdgAe/j/aC+5XVRENnRdxBSESbSg7kKfz0Ilu/DGyXwjY0GrhL
         * HfTzh5tye5Om7SFoHxWJbKLxcO6M1/amboJB0keAnsWH8yvANGfjyCelG9XwWR3/
         * m8N6qqh3AgMBAAECggEAPP+ZL+LXEUECzlr3888UXwwxTC3IFPPUMqYwMdtAHSjI
         * rlt0bPNkhQmq7DgigkGXRhPhcImnUPLwPm4sjG3Qdk9GWhTDhNp9vupCR52gyLRl
         * y8GGQyvykzZIrBY2kEHGWH04ieGfv7QOn5RKEdqwqsY+BgXDaOGniLiX5byhfpXf
         * Wsk9elf3olA8jXCFtkYELeTa4MrSSv16yDPicnPMNhOyAH+OvK7ykU7Smz94iMVp
         * R1hl0WNMEFfP8NG2xkEg7V9Q5zCqCpCVMGdVISU0URI/dFlUsrQMg6ZWG9INomC2
         * MUiiS0YWIGtsySjIArdWbevoH0c3PPLstjt3BHdx0QKBgQDffYJOwPTE9b49EFUy
         * fZ/pXLytfjs6nE+mn5LgLy8usV0k4mEjRPe+3SJolO/ZfRUJqqRFaquFGrC8F+D3
         * z+UnY50GucCoqPaF8FtsUn6gVJHm5yEWnD2wI1RBSIAI4+AoHFhKeVEXRL3YYXqL
         * pxBWCfO7bL1SeJ5nCyUroWYD+QKBgQDVDc28EwxYX6klkbcJ4lONso+j1cPxbNRU
         * 4srOJAtOPdxS/j3eNQPVQDilvSOSalZXFozTnzm3QJ2DPmtN6tFovDIvTCy6HHhf
         * ptQMWHJEct2+6wYqpUfHefPRauhHt4ryWvs+rkKv+Mc4ZWbBz9lpJ4Uw8xd1iry1
         * M8Fj7htL7wKBgQDZdu0G6KbDyEM2c+AS4z5o+n31a3EClPrELV3jmsYUby0tKMJQ
         * aVmQdOh4TwQb4qRH5a02hpcjS1dRjwKu37Ig48L1umWHN/G5Xqn2+0aPh/r4bUo7
         * EAjOjXH8nClJfUKuhPY8cVPAinkYRbHSldtFfSWW6DZIO2oW5jdi/wefMQKBgE9g
         * ijM1IZdXZKyjon9jPGABk/SlcxBizKUnrgmpcjDfaVPed2xvchMkjzZoxnUJe0uN
         * EkDSBO3afBTmz5HDF+wemc/3YH6iltKEVGKZHVGVPjqgU7bVx21xaT6vxwTZREJR
         * VWQgBqaLWKYRWXyCFO1RlRUMrMPVQcN9GeNCVhcvAoGAVRr7Th21+cDSHM0QUQzQ
         * gM64Kfi04w5ME2FSFygw3rWYKTOmqk+b+wZIigTjciMhDSZj46ceIbsGRZ4p7Ozq
         * vNhOo3yoNTT6+wTSj/QKsX0jIa5akVKzmAx5VeW2UOzFv51w3n+NZUQSAE2TqC4k
         * MN+1Yo5WV/crpaHFPvJQCmI=
         * -----END PRIVATE KEY-----
         *
         */
        //{"modulus":"AK4mv15hZiCTMQbls3zk92VXeZstDElNddH4PRQn0DVmuqKLz2v6A5EBe8XBKlk3mF69iCpneoMpnLOOSLY6yx+DFtG80vKiOHsZ04bymzUX8xwSTy7trzQEKL9oHL6zJYpCPJwC8IWwe7wt3gvOqyhjb7o0EUQuTZ4fPOa6Qq2Z","exponent":"AQAB"}
        val print = RSA.encrypt(
            plaintext = "Hello World!",
            modulus = "MIIBCgKCAQEAuf96ZtaxTlzJSTCEPlqMaBuKwQQvDn6UFNk9fwvmCgolaeapvAjU4DxpUH6GmOmpW0NHVs6zAOV3BITpuNM2wXP1xdc2ZG605R4Ehk5CUteXqal60jSMNF7PHEBNczGjw/bRKvDJEi+BHwnkzB0LfNMeE/vfQLlu4fNPdhQexLHp6aRNhiLw7uvbMNkRkJRvCu1foPwlzdh6Efic+Zop9kM/fmYiXYAHv4/2gvuV1URDZ0XcQUhEm0oO5Cn89CJbvwxsl8I2NBq4Sx3084ebcnuTpu0haB8ViWyi8XDujNf2pm6CQdJHgJ7Fh/MrwDRn48gnpRvV8Fkd/5vDeqqodwID",
            exponent = "AQAB"
        )
        println(print)
    }
}

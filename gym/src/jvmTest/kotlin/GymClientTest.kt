import kotlinx.coroutines.runBlocking
import top.kagg886.eoa.gym.GymClient
import kotlin.test.Test

class GymClientTest {

    @Test
    fun testGym() = runBlocking {
        val client = GymClient("")
        client.login("")
        val list = client.list().apply(::println)

        for ((id) in list) {
            val detail = client.detail(id).apply(::println)
            for ((id1) in detail) {
                client.history(id, id1).apply(::println)
            }
        }
    }
}

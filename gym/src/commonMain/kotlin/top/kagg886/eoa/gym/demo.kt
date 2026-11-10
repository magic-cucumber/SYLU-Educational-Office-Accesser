package top.kagg886.eoa.gym

import kotlin.time.Instant

/** test 账号使用的本地体育测试示例数据。 */
internal object GymDemo {
    val details: Map<String, List<GymDetail>> = mapOf(
        "2026" to listOf(
            GymDetail("bmi", "体重指数（BMI）", "21.5", 100f, "kg/m²", "优秀", "15", 1),
            GymDetail("fhl", "肺活量", "4200", 80f, "ml", "良好", "15", 1),
            GymDetail("50m", "50米跑", "7.5", 80f, "秒", "良好", "20", 1),
            GymDetail("zwtqq", "坐位体前屈", "18.2", 85f, "cm", "良好", "10", 1),
            GymDetail("ldty", "立定跳远", "245", 80f, "cm", "良好", "10", 1),
            GymDetail("ytxs", "引体向上", "12", 80f, "次", "良好", "10", 1),
            GymDetail("1000m", "1000米跑", null, null, "分:秒", null, "20", 0),
        ),
        "2025" to listOf(
            GymDetail("bmi", "体重指数（BMI）", "21.8", 100f, "kg/m²", "优秀", "15", 1),
            GymDetail("fhl", "肺活量", "4000", 78f, "ml", "及格", "15", 1),
            GymDetail("50m", "50米跑", "7.7", 76f, "秒", "及格", "20", 1),
            GymDetail("zwtqq", "坐位体前屈", "15.4", 76f, "cm", "及格", "10", 1),
            GymDetail("ldty", "立定跳远", "238", 76f, "cm", "及格", "10", 1),
            GymDetail("ytxs", "引体向上", "10", 76f, "次", "及格", "10", 1),
            GymDetail("1000m", "1000米跑", "3:42", 80f, "分:秒", "良好", "20", 1),
        ),
        "2024" to listOf(
            GymDetail("bmi", "体重指数（BMI）", "22.0", 100f, "kg/m²", "优秀", "15", 1),
            GymDetail("fhl", "肺活量", "3800", 74f, "ml", "及格", "15", 1),
            GymDetail("50m", "50米跑", "8.0", 70f, "秒", "及格", "20", 1),
            GymDetail("zwtqq", "坐位体前屈", "12.0", 70f, "cm", "及格", "10", 1),
            GymDetail("ldty", "立定跳远", "225", 70f, "cm", "及格", "10", 1),
            GymDetail("ytxs", "引体向上", "7", 60f, "次", "及格", "10", 1),
            GymDetail("1000m", "1000米跑", "4:05", 66f, "分:秒", "及格", "20", 1),
        ),
    )

    val summaries: List<GymSummary> = details.map { (year, items) ->
        GymSummary(
            id = year,
            score = items.sumOf { ((it.score ?: 0f) * it.ratio.toFloat()).toDouble() }.toFloat() / 100,
            all = items.size,
            done = items.count { it.isJoin == 1 },
        )
    }

    private val histories: Map<Pair<String, String>, List<GymDetailHistory>> =
        details.flatMap { (year, items) ->
            items.map { item ->
                (year to item.id) to if (item.isJoin == 1 && item.result != null) {
                    listOf(
                        GymDetailHistory(item.result, Instant.parse("$year-10-20T02:30:00Z"), "2"),
                        GymDetailHistory(item.result, Instant.parse("$year-10-13T02:30:00Z"), "1"),
                    )
                } else {
                    emptyList()
                }
            }
        }.toMap()

    fun history(listId: String, detailId: String, page: Int, size: Int): HistoryResponse {
        require(page > 0) { "page must be positive" }
        require(size > 0) { "size must be positive" }
        val records = histories[listId to detailId].orEmpty()
        val offset = ((page - 1).toLong() * size).coerceAtMost(records.size.toLong()).toInt()
        return HistoryResponse(records.drop(offset).take(size), records.size)
    }
}

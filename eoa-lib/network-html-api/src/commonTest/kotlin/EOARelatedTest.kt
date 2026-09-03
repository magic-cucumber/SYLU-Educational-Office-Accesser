import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.kagg886.sylu_eoa.api.html.EOAHTMLClient
import top.kagg886.sylu_eoa.api.v2.Storage
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.math.ceil
import kotlin.math.floor

class EOARelatedTest {
    companion object {
        internal var client by Delegates.notNull<EOAHTMLClient>()
            private set

        suspend fun beforeAll() = run {
            this@Companion.client = EOAHTMLClient()
            client.username = "2203050528"
            client.password = "CaiCai5201314/"
            client.init(
                object : Storage {
                    private var string: String? = null
                    override fun get(): String? = string

                    override fun set(value: String) {
                        string = value
                    }
                }
            )
            client.login()
        }
    }

    @Test
    fun testEOAAllUnRelatedItem() = runBlocking {
        beforeAll()
        println(allUnRelatedItemReturn())
    }

    @Test
    fun testEOAUnRelatedItemDetail() = runBlocking {
        beforeAll()
        val item = allUnRelatedItemReturn().first()
        println(unRelatedItemDetail(item))
    }

    @Test
    fun testEOAUnRelatedItemDetailCommit() = runBlocking {
        beforeAll()
        val item = allUnRelatedItemReturn().first()
        val detail = unRelatedItemDetail(item)
        commit(payload = detail)
    }

    private suspend fun allUnRelatedItemReturn(): List<UnRelatedItem> {
        val resp = client.client.submitForm(
            url = "/xspjgl/xspj_cxXspjIndex.html?doType=query&gnmkdm=N401605&su=${client.username}",
            formParameters = Parameters.build {
                append("nd", Clock.System.now().toEpochMilliseconds().toString())
                append("queryModel.showCount", "5000")
                append("queryModel.currentPage", "1")
                append("queryModel.sortOrder", "asc")
                append("time", "0")
            }
        ).body<InternalUnRelatedItemReturn>()

        return resp.items
    }

    private suspend fun commit(
        status: UnRelatedItemSubmitStatus = UnRelatedItemSubmitStatus.SAVED,
        payload: RelatedItemQuestionReturn
    ) {
        check(status != UnRelatedItemSubmitStatus.UNSAVED) {
            "status 必须为 SAVED,COMMITED"
        }
        fun invalid(question: RelatedItemQuestion, validation: String): Nothing {
            throw InvaildArgumentException("题目「${question.title}」校验失败：$validation")
        }

        fun answerKey(question: RelatedItemQuestion, suffix: String): String =
            question.metadata.keys.singleOrNull { it.endsWith(suffix) }
                ?: invalid(question, "缺少或存在多个提交字段 $suffix")

        fun validateText(
            question: RelatedItemQuestion,
            value: String,
            range: IntRange,
        ) {
            if (question.required && value.isEmpty()) {
                invalid(question, "必填")
            }
            if (value.length !in range) {
                invalid(
                    question,
                    "字数应在 ${range.first}..${range.last} 之间，实际为 ${value.length}"
                )
            }
        }

        fun validateNumber(
            question: RelatedItemQuestion,
            value: Int,
            range: IntProgression,
        ) {
            if (value !in range) {
                invalid(
                    question,
                    "数值应符合 ${range.first}..${range.last}，步长为 ${range.step}，实际为 $value",
                )
            }
        }

        val requestMap = LinkedHashMap(payload.metadata)
        payload.list.forEach { question ->
            requestMap.putAll(question.metadata)
            when (question) {
                is RelatedItemQuestion.TextInput -> {
                    validateText(question, question.value, question.range)
                    requestMap[answerKey(question, ".zgpj")] = question.value
                }

                is RelatedItemQuestion.RangeSelect -> {
                    validateNumber(question, question.value, question.range)
                    requestMap[answerKey(question, ".pjf")] = question.value.toString()
                }

                is RelatedItemQuestion.ChoiceSelect.Single -> {
                    if (question.choice.isEmpty()) {
                        invalid(question, "没有可选项")
                    }
                    if (question.value !in question.choice) {
                        invalid(question, "所选项不属于该题")
                    }
                    requestMap.putAll(question.value.metadata)
                }

                is RelatedItemQuestion.ChoiceSelect.Multi -> {
                    if (question.required && question.value.isEmpty()) {
                        invalid(question, "至少选择一项")
                    }
                    if (question.value.any { it !in question.choice }) {
                        invalid(question, "包含不属于该题的选项")
                    }
                    if (question.value.distinct().size != question.value.size) {
                        invalid(question, "包含重复选项")
                    }
                    question.value
                        .flatMap { it.metadata.entries }
                        .groupBy({ it.key }, { it.value })
                        .forEach { (key, values) -> requestMap[key] = values.joinToString("@") }
                }

                is RelatedItemQuestion.ScoreSelect -> {
                    validateNumber(question, question.value, question.range)
                    val gradeKey = question.metadata.keys.singleOrNull { it.endsWith(".xzpjdj") }
                    if (gradeKey == null) {
                        requestMap[answerKey(question, ".pjf")] = question.value.toString()
                    } else {
                        val scoreKey = gradeKey.removeSuffix(".xzpjdj") + ".srbfzpf"
                        if (scoreKey !in question.metadata) {
                            invalid(question, "缺少提交字段 .srbfzpf")
                        }
                        requestMap[gradeKey] = when (question.value) {
                            in 90..100 -> "01"
                            in 80..89 -> "02"
                            in 70..79 -> "03"
                            in 0..69 -> "04"
                            else -> invalid(
                                question,
                                "精确总分应在 0..100 之间，实际为 ${question.value}"
                            )
                        }
                        requestMap[scoreKey] = question.value.toString()
                    }
                }

                is RelatedItemQuestion.Comments -> {
                    validateText(question, question.value, question.range)
                    requestMap[answerKey(question, ".py")] = encodeURIComponent(question.value)
                }

                is RelatedItemQuestion.Strengths -> {
                    validateText(question, question.value, question.range)
                    requestMap[answerKey(question, ".yd")] = encodeURIComponent(question.value)
                }

                is RelatedItemQuestion.Weaknesses -> {
                    validateText(question, question.value, question.range)
                    requestMap[answerKey(question, ".bz")] = encodeURIComponent(question.value)
                }
            }
        }

        requestMap.keys.filter { it.endsWith(".pjzt") }.forEach { requestMap[it] = "1" }
        requestMap["tjzt"] = when (status) {
            UnRelatedItemSubmitStatus.SAVED -> "0"
            UnRelatedItemSubmitStatus.COMMITED -> "1"
        }


        //提交xspj_tjXspj
        //保存xspj_bcXspj
        val router = when (status) {
            UnRelatedItemSubmitStatus.SAVED -> "xspj_bcXspj"
            UnRelatedItemSubmitStatus.COMMITED -> "xspj_tjXspj"
        }
        val resp = client.client.submitForm(
            url = "/xspjgl/$router.html",
            formParameters = Parameters.build {
                requestMap.forEach { (key, value) -> append(key, value) }
            },
        ).body<String>()

        check(resp.contains("成功")) {
            resp
        }
    }

    private fun encodeURIComponent(value: String): String = buildString {
        val hex = "0123456789ABCDEF"
        value.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and 0xff
            val unescaped = code in 'A'.code..'Z'.code ||
                    code in 'a'.code..'z'.code ||
                    code in '0'.code..'9'.code ||
                    code == '-'.code || code == '_'.code || code == '.'.code ||
                    code == '!'.code || code == '~'.code || code == '*'.code ||
                    code == '\''.code || code == '('.code || code == ')'.code
            if (unescaped) {
                append(code.toChar())
            } else {
                append('%')
                append(hex[code ushr 4])
                append(hex[code and 0x0f])
            }
        }
    }


    private suspend fun unRelatedItemDetail(item: UnRelatedItem): RelatedItemQuestionReturn {
        val resp = client.client.submitForm(
            url = "/xspjgl/xspj_cxXspjDisplay.html?gnmkdm=N401605&su=${client.username}",
            formParameters = Parameters.build {
                for ((k, v) in Json.encodeToJsonElement(item).jsonObject) {
                    append(k, v.jsonPrimitive.content)
                }
            }
        ).body<Document>()

        fun Element.data(name: String): String = attr("data-$name")

        fun Element.inputValue(): String = when (tagName()) {
            "textarea" -> wholeText()
            "select" -> selectFirst("option[selected]")?.attr("value")
                ?: selectFirst("option:not([disabled])")?.attr("value").orEmpty()

            else -> attr("value")
        }

        fun Element.fieldTitle(fallback: String): String =
            parent()?.selectFirst("label")?.text()?.trim()?.takeIf(String::isNotEmpty)
                ?: fallback

        fun Element.rowTitle(): String =
            children().firstOrNull()?.text()?.trim()?.removePrefix("*")?.trim().orEmpty()

        fun Element.integerRange(
            minAttribute: String,
            maxAttribute: String,
            defaultMin: Double,
            defaultMax: Double,
        ): IntRange {
            val min = data(minAttribute).toDoubleOrNull() ?: attr(minAttribute).toDoubleOrNull()
            ?: defaultMin
            val max = data(maxAttribute).toDoubleOrNull() ?: attr(maxAttribute).toDoubleOrNull()
            ?: defaultMax
            return ceil(min).toInt()..floor(max).toInt()
        }

        fun Element.choiceLabel(): String =
            parent()?.text()?.trim()?.takeIf(String::isNotEmpty)
                ?: attr("title").ifEmpty { attr("value") }

        fun Map<String, String>.withAnswerTarget(key: String): Map<String, String> =
            this + (key to "")

        val body = resp.selectFirst("div.xspj-body") ?: error("缺少 div.xspj-body")
        val rootMetadata = mapOf(
            "ztpjbl" to body.data("ztpjbl"),
            "jszdpjbl" to body.data("jszdpjbl"),
            "xykzpjbl" to body.data("xykzpjbl"),
            "jxb_id" to body.data("jxb_id"),
            "kch_id" to body.data("kch_id"),
            "jgh_id" to body.data("jgh_id"),
            "xsdm" to body.data("xsdm"),
        )
        val xspjpykz = resp.selectFirst("#xspjpykz")?.inputValue().orEmpty()
        val commentMin = resp.selectFirst("#xspjpyzszs")?.inputValue()?.toIntOrNull() ?: 0
        val commentMax =
            resp.selectFirst("#xspjpyzskz")?.inputValue()?.toIntOrNull() ?: Int.MAX_VALUE
        val showStrengths = resp.selectFirst("#jmyd")?.inputValue().orEmpty().isNotEmpty()
        val showWeaknesses = resp.selectFirst("#jmbz")?.inputValue().orEmpty().isNotEmpty()
        val commentsRequired = resp.selectFirst("#xspjpybtkz")?.inputValue() == "1"
        val strengthsRequired = resp.selectFirst("#ydbtkz")?.inputValue() == "1"
        val weaknessesRequired = resp.selectFirst("#bzbtkz")?.inputValue() == "1"
        val preciseScoreEnabled = resp.selectFirst("#sfkzjzdf")?.inputValue() == "1"
        val scoreMode = resp.selectFirst("#bfztxfs")?.inputValue().orEmpty()

        val questions = buildList {
            resp.selectFirst("#jmsfxhjs")
                ?.takeIf { it.inputValue().isNotEmpty() }
                ?.let { titleElement ->
                    val answerKey = "sfxhjs"
                    val choices = resp.select("input[name=sfxhjs]").map { input ->
                        RelatedItemQuestion.Choice(
                            label = input.choiceLabel(),
                            value = input.attr("value"),
                            metadata = mapOf(answerKey to input.attr("value")),
                        )
                    }
                    if (choices.isNotEmpty()) {
                        add(
                            RelatedItemQuestion.ChoiceSelect.Single(
                                title = titleElement.inputValue(),
                                choice = choices,
                                value = choices.first(),
                                required = true,
                                metadata = emptyMap(),
                            )
                        )
                    }
                }

            resp.select("div.panel-pjdx").forEachIndexed { panelIndex, panel ->
                val modelPrefix = "modelList[$panelIndex]"
                val panelMetadata = mapOf(
                    "$modelPrefix.pjmbmcb_id" to panel.data("pjmbmcb_id"),
                    "$modelPrefix.pjmbmc" to panel.data("pjmbmc"),
                    "$modelPrefix.pjdxdm" to panel.data("pjdxdm"),
                    "$modelPrefix.fxzgf" to panel.data("fxzgf"),
                    "$modelPrefix.xspfb_id" to panel.data("xspfb_id"),
                    "$modelPrefix.py" to "",
                    "$modelPrefix.pjzt" to "",
                )

                if (preciseScoreEnabled) {
                    panel.selectFirst("select[name=xzpjdj]")?.let { grade ->
                        val scoreKey = "$modelPrefix.srbfzpf"
                        add(
                            RelatedItemQuestion.ScoreSelect(
                                title = grade.fieldTitle("评价总分"),
                                value = 100,
                                range = 0..100,
                                required = true,
                                metadata = panelMetadata + mapOf(
                                    "$modelPrefix.xzpjdj" to "",
                                    scoreKey to "",
                                ),
                            )
                        )
                    }
                }

                panel.select("table.table-xspj").forEachIndexed { tableIndex, table ->
                    val tablePrefix = "$modelPrefix.xspjList[$tableIndex]"
                    val tableMetadata = panelMetadata +
                            ("$tablePrefix.pjzbxm_id" to table.data("pjzbxm_id"))

                    table.select("tr.tr-xspj").forEachIndexed { questionIndex, row ->
                        val questionPrefix = "$tablePrefix.childXspjList[$questionIndex]"
                        val questionMetadata = tableMetadata + mapOf(
                            "$questionPrefix.pjzbxm_id" to row.data("pjzbxm_id"),
                            "$questionPrefix.pfdjdmb_id" to row.data("pfdjdmb_id"),
                            "$questionPrefix.zsmbmcb_id" to row.data("zsmbmcb_id"),
                        )
                        val required = row.data("sfbt") == "1"
                        val title = row.rowTitle()

                        when (row.data("dtlx")) {
                            "1" -> row.selectFirst("textarea.form-control")?.let { textarea ->
                                add(
                                    RelatedItemQuestion.TextInput(
                                        title = title,
                                        value = "",
                                        range = textarea.integerRange(
                                            "zsxx",
                                            "zssx",
                                            0.0,
                                            Int.MAX_VALUE.toDouble()
                                        ),
                                        required = required,
                                        metadata = questionMetadata.withAnswerTarget("$questionPrefix.zgpj"),
                                    )
                                )
                            }

                            "2" -> when {
                                row.selectFirst("div.form-group") != null -> {
                                    val inputName = listOf(
                                        panel.data("pjmbmcb_id"),
                                        row.data("pjzbxm_id"),
                                        row.data("pfdjdmb_id"),
                                    ).joinToString("_")
                                    val answerKey = "$questionPrefix.pfdjdmxmb_id"
                                    val choices =
                                        row.select("input[name='$inputName']").map { input ->
                                            val value = input.data("pfdjdmxmb_id")
                                            RelatedItemQuestion.Choice(
                                                label = input.choiceLabel(),
                                                value = value,
                                                metadata = mapOf(answerKey to value),
                                            )
                                        }
                                    if (choices.isNotEmpty()) {
                                        add(
                                            RelatedItemQuestion.ChoiceSelect.Single(
                                                title = title,
                                                choice = choices,
                                                value = choices.first(),
                                                required = required,
                                                metadata = questionMetadata,
                                            )
                                        )
                                    }
                                }

                                row.selectFirst("div.range-slider") != null -> {
                                    row.selectFirst("input.range-slider__range")?.let { input ->
                                        val range = input.integerRange("min", "max", 0.0, 100.0)
                                        val step =
                                            input.attr("step").toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        add(
                                            RelatedItemQuestion.RangeSelect(
                                                title = title,
                                                range = range.first..range.last step step,
                                                value = range.first,
                                                required = required,
                                                metadata = questionMetadata.withAnswerTarget("$questionPrefix.pjf"),
                                            )
                                        )
                                    }
                                }

                                else -> row.selectFirst("input.form-control")?.let { input ->
                                    val multiplier = if (scoreMode == "2") {
                                        (input.data("yjqz").toDoubleOrNull() ?: 0.0) *
                                                (input.data("ejqz").toDoubleOrNull() ?: 0.0)
                                    } else {
                                        1.0
                                    }
                                    val min =
                                        (input.data("zxfz").toDoubleOrNull() ?: 0.0) * multiplier
                                    val max =
                                        (input.data("zdfz").toDoubleOrNull() ?: 100.0) * multiplier
                                    val range = ceil(min).toInt()..floor(max).toInt()
                                    add(
                                        RelatedItemQuestion.ScoreSelect(
                                            title = title,
                                            value = range.first,
                                            range = range,
                                            required = required,
                                            metadata = questionMetadata.withAnswerTarget("$questionPrefix.pjf"),
                                        )
                                    )
                                }
                            }

                            "3" -> {
                                val inputName = listOf(
                                    panel.data("pjmbmcb_id"),
                                    row.data("pjzbxm_id"),
                                    row.data("pfdjdmb_id"),
                                ).joinToString("_")
                                val answerKey = "$questionPrefix.pfdjdmxmb_id"
                                val choices = row.select("input[name='$inputName']").map { input ->
                                    val value = input.data("pfdjdmxmb_id")
                                    RelatedItemQuestion.Choice(
                                        label = input.choiceLabel(),
                                        value = value,
                                        metadata = mapOf(answerKey to value),
                                    )
                                }
                                if (choices.isNotEmpty()) {
                                    add(
                                        RelatedItemQuestion.ChoiceSelect.Multi(
                                            title = title,
                                            choice = choices,
                                            value = emptyList(),
                                            required = required,
                                            metadata = questionMetadata,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                val templateId = panel.data("pjmbmcb_id")
                val comments = when {
                    xspjpykz == "1" -> resp.selectFirst("#${templateId}_py")
                    xspjpykz == "2" && panel.data("pjdxdm") == "01" -> resp.selectFirst("#js_py")
                    else -> null
                }
                comments?.let {
                    add(
                        RelatedItemQuestion.Comments(
                            title = it.fieldTitle("评语"),
                            value = "",
                            range = commentMin..commentMax,
                            required = commentsRequired,
                            metadata = panelMetadata.withAnswerTarget("$modelPrefix.py"),
                        )
                    )
                }
                if (showStrengths) {
                    resp.selectFirst("#${templateId}_yd")?.let {
                        add(
                            RelatedItemQuestion.Strengths(
                                title = it.fieldTitle("优点"),
                                value = "",
                                range = 0..Int.MAX_VALUE,
                                required = strengthsRequired,
                                metadata = panelMetadata.withAnswerTarget("$modelPrefix.yd"),
                            )
                        )
                    }
                }
                if (showWeaknesses) {
                    resp.selectFirst("#${templateId}_bz")?.let {
                        add(
                            RelatedItemQuestion.Weaknesses(
                                title = it.fieldTitle("不足"),
                                value = "",
                                range = 0..Int.MAX_VALUE,
                                required = weaknessesRequired,
                                metadata = panelMetadata.withAnswerTarget("$modelPrefix.bz"),
                            )
                        )
                    }
                }

                panel.selectFirst("select[name=pjf]")?.let { select ->
                    val answerKey = "$modelPrefix.pjf"
                    val choices = select.select("option[value]")
                        .filter { it.attr("value").isNotEmpty() }
                        .map { option ->
                            RelatedItemQuestion.Choice(
                                label = option.text().trim(),
                                value = option.attr("value"),
                                metadata = mapOf(answerKey to option.attr("value")),
                            )
                        }
                    if (choices.isNotEmpty()) {
                        add(
                            RelatedItemQuestion.ChoiceSelect.Single(
                                title = select.fieldTitle("整体评价等级"),
                                choice = choices,
                                value = choices.first(),
                                required = true,
                                metadata = panelMetadata,
                            )
                        )
                    }
                }
            }
        }
        return RelatedItemQuestionReturn(
            metadata = rootMetadata,
            list = questions,
        )
    }
}

@Serializable
enum class UnRelatedItemSubmitStatus {
    SAVED, COMMITED, UNSAVED
}

@Serializable
data class UnRelatedItem(
    @SerialName("jzgmc")
    val teacher: String,
    @SerialName("jxdd")
    val room: String = "",
    @SerialName("kcmc")
    val name: String,

    @SerialName("tjzt")
    private val _submitStatus: Int,

    internal val jxb_id: String,
    internal val jgh_id: String,
    internal val kch_id: String,
    internal val xsdm: String,
) {

    val submitStatus: UnRelatedItemSubmitStatus by lazy {
        when (_submitStatus) {
            0 -> UnRelatedItemSubmitStatus.UNSAVED
            1 -> UnRelatedItemSubmitStatus.COMMITED
            2 -> UnRelatedItemSubmitStatus.SAVED
            else -> error("unknown value.")
        }
    }
}

@Serializable
private data class InternalUnRelatedItemReturn(
    val items: List<UnRelatedItem>
)

data class RelatedItemQuestionReturn(
    val metadata: Map<String, String>,
    val list: List<RelatedItemQuestion>
)

sealed interface RelatedItemQuestion {
    val title: String
    val metadata: Map<String, String>

    val required: Boolean

    /**
     * 主观文本题
     */
    class TextInput internal constructor(
        override val title: String,
        var value: String = "",
        val range: IntRange,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion

    /**
     * 滑块题
     */
    class RangeSelect internal constructor(
        override val title: String,
        val range: IntProgression,
        var value: Int,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion

    /**
     * 选择题目
     */
    sealed interface ChoiceSelect : RelatedItemQuestion {
        val choice: List<Choice>

        class Single internal constructor(
            override val title: String,
            override val choice: List<Choice>,
            var value: Choice,
            override val required: Boolean,
            override val metadata: Map<String, String>,
        ) : ChoiceSelect


        class Multi internal constructor(
            override val title: String,
            override val choice: List<Choice>,
            var value: List<Choice>,
            override val required: Boolean,
            override val metadata: Map<String, String>,
        ) : ChoiceSelect
    }

    data class Choice(
        val label: String,
        private val value: String,
        internal val metadata: Map<String, String>
    )


    /**
     * 分数题
     */
    class ScoreSelect internal constructor(
        override val title: String,
        var value: Int,
        val range: IntProgression,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion

    /**
     * 评语
     */
    class Comments internal constructor(
        override val title: String,
        var value: String,
        val range: IntRange,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion

    /**
     * 优点
     */
    class Strengths internal constructor(
        override val title: String,
        var value: String,
        val range: IntRange,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion

    /**
     * 不足
     */
    class Weaknesses internal constructor(
        override val title: String,
        var value: String,
        val range: IntRange,
        override val required: Boolean,
        override val metadata: Map<String, String>,
    ) : RelatedItemQuestion
}

class InvaildArgumentException(message: String) : IllegalArgumentException(message)

import io.ktor.http.Cookie
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import top.kagg886.eoa.second.TWUser
import top.kagg886.eoa.second.config.BuildConfig
import top.kagg886.eoa.util.Storage
import top.kagg886.eoa.vpn.VPNClient
import top.kagg886.eoa.vpn.bean.CaptchaReturn
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 21:17
 * ================================================
 */


class EOASecondVPNTest {
    @Test
    fun testEOASecondVPN(): Unit = runBlocking {
        val username = ""
        val password = ""
        val tw = ""

        val client = VPNClient(
            username = username,
            password = password,
        )

        client.login(
            totpHandler = {
                770242
            },
            captchaHandler = { b,s->
                CaptchaReturn(
                    560,
                    waitSliderWidth(s,b).toInt()
                )
            }
        )

        ///Resource(name=团委第二课堂系统, redirect=/http/77726476706e69737468656265737421e8f00f8f3e3c7d1e7b0c9ce29b5b/SyluTW/Sys/UserLogin.aspx)
        val portal = client.portal().first { it.name == "团委第二课堂系统" }.redirect

        val twClient = TWUser(
            baseURL = "https://webvpn.${BuildConfig.MESSAGE_API_ENDPOINT}${portal.substringBefore("UserLogin.aspx")}",
            user = username,
            ticket = client.ticket()
        )

        twClient.login(tw)

        println(twClient.getData().entries.associate { it.key.id to it.value.sumOf { it.score } })
    }

    @Test
    fun testEOAVpnDirect(): Unit = runBlocking {
        val username = ""
        val password = ""

        val twClient = TWUser(
            baseURL = "http://xg.${BuildConfig.MESSAGE_API_ENDPOINT}/SyluTW/Sys/",
            user = username
        )

        twClient.login(password)

        println(twClient.getData().entries.associate { it.key.id to it.value.sumOf { it.score } })
    }

    @Test
    fun testDurationNegative(): Unit = runBlocking {
        val a = 1.seconds
        val b = 1.minutes
        println(a - b)
    }
}


suspend fun waitSliderWidth(smallImageData: ByteArray, bigImageData: ByteArray): String {
    return withContext(Dispatchers.IO) {
        // 创建图片对象
        val smallImage = ImageIcon(smallImageData).image
        val bigImage = ImageIcon(bigImageData).image

        // 使用 CompletableDeferred 来等待用户输入
        val result = CompletableDeferred<String>()

        // 在 EDT (Event Dispatch Thread) 上创建 GUI
        SwingUtilities.invokeLater {
            // 定义尺寸
            val BIG_WIDTH = 590
            val BIG_HEIGHT = 360
            val SMALL_WIDTH = 93
            val SMALL_HEIGHT = 360

            // 创建主窗口
            val frame = JFrame("滑块验证").apply {
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                layout = BorderLayout()
                isResizable = false
            }

            // 计算滑块的最大移动距离（背景宽度 - 滑块宽度）
            val maxSliderPosition = BIG_WIDTH - SMALL_WIDTH

            // 创建滑块，范围从0到最大移动距离
            val slider = JSlider(0, maxSliderPosition, 0).apply {
                paintTicks = true
                paintLabels = false
                majorTickSpacing = maxSliderPosition / 5
                minorTickSpacing = maxSliderPosition / 20
            }

            // 创建显示当前值的标签
            val valueLabel = JLabel("位置: 0 px", SwingConstants.CENTER).apply {
                font = Font("Arial", Font.BOLD, 14)
            }

            // 创建图片面板
            val imagePanel = object : JPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                    // 绘制背景图（大图）
                    g2d.drawImage(bigImage, 0, 0, BIG_WIDTH, BIG_HEIGHT, this)

                    // 获取滑块当前位置
                    val xPos = slider.value

                    // 绘制滑块图（小图）- 垂直居中，水平位置根据滑块调整
                    g2d.drawImage(smallImage, xPos, 0, SMALL_WIDTH, SMALL_HEIGHT, this)

                    // 绘制半透明提示框
                    g2d.color = Color(0, 0, 0, 100)
                    g2d.fillRect(10, 10, 200, 30)
                    g2d.color = Color.WHITE
                    g2d.font = Font("Arial", Font.PLAIN, 12)
                    g2d.drawString("拖动滑块完成验证", 20, 30)
                }
            }.apply {
                preferredSize = Dimension(BIG_WIDTH, BIG_HEIGHT)
                border = BorderFactory.createLineBorder(Color.DARK_GRAY, 2)
                background = Color.WHITE
            }

            // 给滑块添加监听器
            slider.addChangeListener {
                imagePanel.repaint()
                valueLabel.text = "位置: ${slider.value} px"
            }

            // 创建滑块控制面板
            val sliderPanel = JPanel(GridBagLayout()).apply {
                val gbc = GridBagConstraints()
                gbc.fill = GridBagConstraints.HORIZONTAL
                gbc.weightx = 1.0
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.insets = Insets(5, 10, 5, 10)
                add(slider, gbc)

                gbc.gridy = 1
                gbc.insets = Insets(0, 10, 5, 10)
                add(valueLabel, gbc)
            }

            // 创建底部面板
            val bottomPanel = JPanel(BorderLayout()).apply {
                add(sliderPanel, BorderLayout.CENTER)

                // 创建按钮面板
                val buttonPanel = JPanel().apply {
                    add(JButton("确定").apply {
                        font = Font("Arial", Font.BOLD, 14)
                        preferredSize = Dimension(100, 35)
                        addActionListener {
                            result.complete(slider.value.toString())
                            frame.dispose()
                        }
                    })
                    add(JButton("重置").apply {
                        font = Font("Arial", Font.PLAIN, 14)
                        preferredSize = Dimension(80, 35)
                        addActionListener {
                            slider.value = 0
                        }
                    })
                }
                add(buttonPanel, BorderLayout.SOUTH)
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            }

            // 组装界面
            frame.apply {
                add(imagePanel, BorderLayout.CENTER)
                add(bottomPanel, BorderLayout.SOUTH)
                pack()
                setLocationRelativeTo(null) // 居中显示
                isVisible = true
            }

            // 处理窗口关闭事件
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    if (!result.isCompleted) {
                        result.complete("0") // 默认值
                    }
                }
            })
        }

        result.await()
    }
}

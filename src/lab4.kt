package example

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.abs
import kotlin.math.sign

class Lab4{
    private var window: Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var colorMain = floatArrayOf(.0f, .0f, .0f)
    private var lines = ArrayList<Pair<Double, Double>>()
    private var width = WIDTH
    private var height = HEIGHT
    private var frameBuffer = FloatArray(width * height * 3)
    private var colored = HashSet<Int>()
    private var filter = false
    private var final = false

    companion object {
        const val WIDTH = 800
        const val HEIGHT = 600
    }

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "LAB4", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")
        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (action == GLFW.GLFW_RELEASE && key == GLFW.GLFW_KEY_ESCAPE) {
                    GLFW.glfwSetWindowShouldClose(
                        window,
                        GLFW.GLFW_TRUE.toBoolean()
                    )
                } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                    when (key) {
                        GLFW.GLFW_KEY_SPACE -> {
//                            filter = !filter
//                            drawLines()
                        }
                        GLFW.GLFW_KEY_BACKSPACE -> {
                            clearBuffer()
                            lines.clear()
                            colored.clear()
                        }
                        GLFW.GLFW_KEY_ENTER -> {
                            final = true
                            drawLines()
                        }
                    }
                }
            }
        }.also { keyCallback = it })

        GLFW.glfwSetMouseButtonCallback(window) { window, button, action, _ ->
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS){
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                GLFW.glfwGetCursorPos(window, x, y)
                lines.add(Pair(x[0], y[0]))
                clearBuffer()
                setPixelColor(x[0].toInt(), y[0].toInt(), Triple(1f, 1f, 1f), false)
                drawLines()
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS){
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                GLFW.glfwGetCursorPos(window, x, y)
                fill(x[0].toInt(), y[0].toInt())
            }
        }

        GLFW.glfwSetFramebufferSizeCallback(window) { _, w, h ->
            width = w
            height = h
            resize()
        }

        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - width) / 2, (vidMode.height() - height) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }

    private fun setPixelColor(x: Int, y: Int, rgb: Triple<Float, Float, Float>, f: Boolean) {
        val index = getIndex(x, y)

        if (f) {
            colored.add(y)
        }

        frameBuffer[index] = rgb.first
        frameBuffer[index+1] = rgb.second
        frameBuffer[index+2] = rgb.third
    }

    private fun drawLines(){
        colored.clear()
        if (lines.size > 1 && !final) {
            if (!filter) {
                for (i in 0 until lines.lastIndex) {
                    drawLine(lines[i].first, lines[i].second, lines[i + 1].first, lines[i + 1].second)
                }
            } else {
                for (i in 0 until lines.lastIndex) {
                    drawLinePostFiltered(lines[i].first, lines[i].second, lines[i + 1].first, lines[i + 1].second)
                }
            }
        } else if (final) {
            if (!filter) {
                drawLine(lines.last().first, lines.last().second, lines.first().first, lines.first().second)
            }else {
                drawLinePostFiltered(lines.last().first, lines.last().second, lines.first().first, lines.first().second)
            }
            final = false
        }
    }

    private fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        glPixelZoom(1f, 1f)

        var x = x1.toInt()
        var y = y1.toInt()
        var dx = abs(x2-x1).toInt()
        var dy = abs(y2-y1).toInt()
        val signX = sign(x2-x1).toInt()
        val signY = sign(y2-y1).toInt()
        var e = 2 * dy - dx

        if (dy == 0) {
            var i = x
            while (abs(x2-i) > 0) {
                setPixelColor(i, y, Triple(1f, 1f, 1f), false)
                i += signX
            }
            return
        }
        if (dx == 0) {
            var i = y
            while (abs(y2-i) > 0) {
                setPixelColor(x, i, Triple(1f, 1f, 1f), false)
                i += signY
            }
            return
        }

        var change = 0
        if (dy >= dx) {
            dx = dy.also { dy = dx }
            change = 1
        }

        var i = 0
        while (true) {
            setPixelColor(x, y, Triple(1f, 1f, 1f), false)
            if (e < dx) {
                if (change == 1)
                    y += signY
                else x += signX
                e += 2 * dy
            } else {
                if (change == 1)
                    x += signX
                else y += signY
                e -= 2 * dx
            }
            i++
            if (i > dx + dy) {
                setPixelColor(x, y, Triple(1f, 1f, 1f), false)
                break
            }
        }
    }

    // постфильтрация с равномерным усреднением области NхN (без использования аккумулирующего буфера)
    private fun drawLinePostFiltered(x1: Double, y1: Double, x2: Double, y2: Double) {

    }

    private fun fill(x: Int, y: Int) {
        // А7: построчного заполнения с затравкой для восьмисвязной гранично-определенной области;
        val s = Stack<Pair<Int, Int>>()
        s.push(Pair(x, y))

        while (s.isNotEmpty()) {
            val p = s.pop()
            var x1Min = p.first
            var x1Max = p.first
            val y1 = p.second

            // строка вправо
            while (true) {
                val index = getIndex(x1Max + 1, y1)
                val point = Triple(frameBuffer[index], frameBuffer[index + 1], frameBuffer[index + 2])
                if (point.first != 0f && point.second != 0f && point.third != 0f) break

//                if (frame.contains(index)) break
                x1Max++
                if (x1Max+1 >= width) {
                    x1Max = width - 1
                    break
                }
            }
            // влево
            while (true) {
                val index = getIndex(x1Min - 1, y1)
                val point = Triple(frameBuffer[index], frameBuffer[index + 1], frameBuffer[index + 2])
                if (point.first != 0f && point.second != 0f && point.third != 0f) break
                x1Min--
                if (x1Min < 0) {
                    x1Min = 0
                    break
                }
            }
            for (i in x1Min until x1Max) {
                setPixelColor(i, y1, Triple(1f, 1f, 1f), true)
            }

            // верх
            if (y1-1 > 0 && !colored.contains(y1-1)) {
                var f = true
                var i = if (x1Min > 0) x1Min - 1 else 0
                val max = if (x1Max < width - 2) x1Max + 1 else width - 1
                while (i <= max) {
                    val index = getIndex(i, y1 - 1)
                    val point = Triple(frameBuffer[index], frameBuffer[index + 1], frameBuffer[index + 2])
                    if (point.first == 0f && point.second == 0f && point.third == 0f) {
                        if (f) {
                            s.push(Pair(i, y1-1))
                            f = false
                        }
                    } else {
                        f = true
                    }
                    i++
                }
            }

            // низ
            if (y1 + 1 < height && !colored.contains(y1+1)) {
                var f = true
                var i = if (x1Min > 0) x1Min - 1 else 0
                val max = if (x1Max < width - 2) x1Max + 1 else width - 1
                while (i <= max) {
                    val index = getIndex(i, y1 + 1)
                    val point = Triple(frameBuffer[index], frameBuffer[index + 1], frameBuffer[index + 2])
                    if (point.first == 0f && point.second == 0f && point.third == 0f) {
                        if (f) {
                            s.push(Pair(i, y1+1))
                            f = false
                        }
                    } else {
                        f = true
                    }
                    i++
                }
            }
        }
        colored.clear()
    }

    private fun resize() {
        glViewport(0, 0, width, height)

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        glOrtho(0.0, width.toDouble(), 0.0, height.toDouble(), 0.0, 0.0)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        clearBuffer()
        lines.clear()
        colored.clear()
    }

    private fun loop() {

        GL.createCapabilities()
        glViewport(0, 0, width, height)

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        glOrtho(0.0, width.toDouble(), 0.0, height.toDouble(), 0.0, 0.0)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()


        glClearColor(colorMain[0], colorMain[1], colorMain[2], 0.0f)
        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE){

            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)



            glDrawPixels(width, height, GL_RGB, GL_FLOAT, frameBuffer)

            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }
    }

    private fun getIndex(x: Int, y: Int): Int = (height - y) * 3 * width + 3 * x

    fun run(){
        println("running LWJGL")
        try {
            init()
            loop()
            GLFW.glfwDestroyWindow(window)
            keyCallback!!.free()
        } finally {
            GLFW.glfwTerminate()
            errorCallback!!.free()
        }
    }

    private fun clearBuffer(){
        frameBuffer = FloatArray(width * height * 3)
    }
}

fun main(){
    Lab4().run()
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0
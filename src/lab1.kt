package example

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil
import kotlin.random.Random

class LWJGL {
    internal enum class Actions {
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        RESIZE_LEFT,
        RESIZE_RIGHT,
        RESIZE_UP,
        RESIZE_DOWN,
        SWAP_COLOR,
        CHANGE_MAIN,
        CHANGE_QUAD
    }

    private var window : Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var left = -1f
    private var right = -0.2f
    private var top = -0.2f
    private var bottom = -1f
    private var count: Byte = 0
    private var colorMain = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
    private var colorQuad = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())

    private fun init(){
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
        val WIDTH = 500
        val HEIGHT = 500
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "Hello LWJGL3", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")
        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
                    GLFW.glfwSetWindowShouldClose(window,
                        GLFW.GLFW_TRUE.toBoolean()) // закрытие окна по клавише esc
            }
        }.also { keyCallback = it })
        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }
    private fun update(mode: Actions){
        when (mode){
            Actions.MOVE_LEFT -> {
                if (left > -1f) {
                    left -= 0.01f
                    right -= 0.01f
                }
            }
            Actions.MOVE_RIGHT -> {
                if(right < 1f) {
                    right += 0.01f
                    left += 0.01f
                }
            }
            Actions.MOVE_UP -> {
                if (top < 1f) {
                    top += 0.01f
                    bottom += 0.01f
                }
            }
            Actions.MOVE_DOWN -> {
                if (bottom > -1f) {
                    top -= 0.01f
                    bottom -= 0.01f
                }
            }
            Actions.RESIZE_LEFT ->{
                if (left < right - 0.02f)
                    right -= 0.01f
            }
            Actions.RESIZE_RIGHT -> {
                if (right < 1f)
                    right += 0.01f
            }
            Actions.RESIZE_DOWN -> {
                if (top > bottom + 0.02f)
                    top -= .01f
            }
            Actions.RESIZE_UP -> {
                if (top < 1f)
                    top += .01f
            }
            Actions.SWAP_COLOR -> {
                if(count > 10) {
                    count = 0
                    colorMain = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
                    colorQuad = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
                }
            }
            Actions.CHANGE_MAIN -> {
                if (count > 20) {
                    count = 0
                    colorMain = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
                }
            }
            Actions.CHANGE_QUAD -> {
                if (count > 20) {
                    count = 0
                    colorQuad = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
                }
            }
        }
    }
    private fun render(){
        drawQuad()
    }

    private fun drawQuad(){
        GL11.glColor3f(colorQuad[0], colorQuad[1], colorQuad[2])
        GL11.glBegin(GL11.GL_QUADS)

        GL11.glVertex2f(left, bottom)
        GL11.glVertex2f(left, top)
        GL11.glVertex2f(right, top)
        GL11.glVertex2f(right, bottom)

        GL11.glEnd()
    }

    private fun loop(){
        GL.createCapabilities()
        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE) {
            GL11.glClearColor(colorMain[0], colorMain[1], colorMain[2], 0.0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

            when {
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == 1 -> update(Actions.MOVE_UP)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == 1 -> update(Actions.MOVE_DOWN)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == 1 -> update(Actions.MOVE_LEFT)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == 1 -> update(Actions.MOVE_RIGHT)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == 1 -> update(Actions.RESIZE_UP)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == 1 -> update(Actions.RESIZE_LEFT)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == 1 -> update(Actions.RESIZE_DOWN)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == 1 -> update(Actions.RESIZE_RIGHT)
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == 1 -> update(Actions.SWAP_COLOR)
                GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == 1 -> update(Actions.CHANGE_MAIN)
                GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == 1 -> update(Actions.CHANGE_QUAD)
            }

            if (count < Byte.MAX_VALUE) count++
            else count = 0

            render()
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }
    }

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
}

fun main() {
    LWJGL().run()
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0
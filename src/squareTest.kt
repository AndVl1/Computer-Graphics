package example

import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil

class HelloLWJGL {
    private var errorCallback: GLFWErrorCallback? = null
    private var keyCallback: GLFWKeyCallback? = null
    private var window: Long = 0
    private var sp = 0.0f // сторона квадрата
    private var swapcolor = false
    private fun init() { // инициализация окна
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
        val WIDTH = 300
        val HEIGHT = 300
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

    private fun update(f: Float) {
        sp += f
        if (sp > 1.0f) {
            sp = 0.0f
            swapcolor = !swapcolor
        } else if (sp < 0.0f) {
            sp = 1.0f
            swapcolor = !swapcolor
        }
    }

    private fun render() {
        drawQuad()
    }

    private fun drawQuad() {
        if (!swapcolor) {
            GL11.glColor3f(0.0f, 1.0f, 0.0f)
        } else {
            GL11.glColor3f(0.0f, 0.0f, 1.0f)
        }
        GL11.glBegin(GL11.GL_QUADS)
        run {
            GL11.glVertex3f(-sp, -sp, 0.0f)
            GL11.glVertex3f(sp, -sp, 0.0f)
            GL11.glVertex3f(sp, sp, 0.0f)
            GL11.glVertex3f(-sp, sp, 0.0f)
        }
        GL11.glEnd()
    }

    private fun loop() {
        GL.createCapabilities()
        println("----------------------------")
        println("OpenGL Version : " + GL11.glGetString(GL11.GL_VERSION))
        println("OpenGL Max Texture Size : " + GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE))
        println("OpenGL Vendor : " + GL11.glGetString(GL11.GL_VENDOR))
        println("OpenGL Renderer : " + GL11.glGetString(GL11.GL_RENDERER))
        println("OpenGL Extensions supported by your card : ")
        val extensions = GL11.glGetString(GL11.GL_EXTENSIONS)
        val extArr = extensions!!.split(" ".toRegex()).toTypedArray()
        for (i in extArr.indices) {
            println(extArr[i])
        }
        println("----------------------------")
        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE) {
            if (!swapcolor) {
                GL11.glClearColor(0.0f, 0.0f, 1.0f, 0.0f)
            } else {
                GL11.glClearColor(0.0f, 1.0f, 0.0f, 0.0f)
            }
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

            when {
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == 1 -> {
                    update(0.001f)
                }
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == 1 -> {
                    update(-0.001f)
                }
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == 1 -> {
                    update(0.005f)
                }
            }

            render()
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }
    }

    fun run() {
        println("Hello LWJGL3 " + Version.getVersion() + "!")
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

fun main(args: Array<String>) {
    HelloLWJGL().run()
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0

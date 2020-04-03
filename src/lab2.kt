package example

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import kotlin.math.*


class Lab2 {
    private var window : Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var colorMain = floatArrayOf(0f, 0f, 0f)
    private var rotationX = .0f
    private var rotationY = .0f
    private var rotationZ = .0f
    private var moveX = .0f
    private var moveY = .0f
    private var moveZ = .0f
    private var sizeX = .6f
    private var sizeY = .6f
    private var sizeZ = .6f
    private var mode = GL_FILL
    private val big = .4f
    private val small = .15f

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        val WIDTH = 1000
        val HEIGHT = 1000
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "LAB2", MemoryUtil.NULL, MemoryUtil.NULL)
        if (window == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

        GLFW.glfwSetKeyCallback(window, object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if (action == GLFW.GLFW_RELEASE) {
                    when (key) {
                        GLFW.GLFW_KEY_ESCAPE -> GLFW.glfwSetWindowShouldClose(
                            window,
                            GLFW.GLFW_TRUE.toBoolean()
                        )
                    }
                } else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                    when (key) {
                        GLFW.GLFW_KEY_UP -> rotationX += 5f
                        GLFW.GLFW_KEY_DOWN -> rotationX -= 5f
                        GLFW.GLFW_KEY_LEFT -> rotationY += 5f
                        GLFW.GLFW_KEY_RIGHT -> rotationY -= 5f
                        GLFW.GLFW_KEY_SPACE -> changeMode()
                        GLFW.GLFW_KEY_W -> moveY += .1f
                        GLFW.GLFW_KEY_S -> moveY -= .1f
                        GLFW.GLFW_KEY_A -> moveX -= .1f
                        GLFW.GLFW_KEY_D -> moveX += .1f
                        GLFW.GLFW_KEY_O -> moveZ += .1f
                        GLFW.GLFW_KEY_K -> moveZ -= .1f
                        GLFW.GLFW_KEY_P -> {
                            sizeX += .01f
                            sizeY += .01f
                            sizeZ += .01f
                        }
                        GLFW.GLFW_KEY_M -> {
                            sizeX -= .01f
                            sizeY -= .01f
                            sizeZ -= .01f
                        }
                    }
                }
            }
        }.also { keyCallback = it })

        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }

    private fun render() {
        drawCoords()
        drabBigCube()
        drawSmallCube()
    }

    private fun drabBigCube() {
        glPushMatrix()

        glTranslatef(moveX, moveY, moveZ)
        glRotatef(rotationX, 1f, 0f, 0f)
        glRotatef(rotationY, 0f, 1f, 0f)
        glRotatef(rotationZ, 0f, 0f, 1f)
        glScalef(sizeX, sizeY, sizeZ)

        glBegin(GL_QUADS)

        glColor3f(1f,0f,0f)
        glVertex3f(-big, -big, -big)
        glVertex3f(-big, big, -big)
        glVertex3f(big, big, -big)
        glVertex3f(big, -big, -big)

        glColor3f(0f, 1f, 0f)
        glVertex3f(-big, -big, big)
        glVertex3f(-big, big, big)
        glVertex3f(big, big, big)
        glVertex3f(big, -big, big)

        glColor3f(0f, 0f, 1f)
        glVertex3f(-big, big, -big)
        glVertex3f(big, big, -big)
        glVertex3f(big, big, big)
        glVertex3f(-big, big, big)

        glColor3f(1f, 1f, 0f)
        glVertex3f(-big, -big, -big)
        glVertex3f(big, -big, -big)
        glVertex3f(big, -big, big)
        glVertex3f(-big, -big, big)

        glColor3f(1f, 1f, 1f)
        glVertex3f(-big, -big, -big)
        glVertex3f(-big, big, -big)
        glVertex3f(-big, big, big)
        glVertex3f(-big, -big, big)

        glColor3f(1f, .5f, .1f)
        glVertex3f(big, -big, -big)
        glVertex3f(big, big, -big)
        glVertex3f(big, big, big)
        glVertex3f(big, -big, big)

        glEnd()

        glPopMatrix()
    }

    private fun drawSmallCube(){
        glPushMatrix()
        glLoadIdentity()

        glTranslatef(-.7f, -.7f, 0f)

        glBegin(GL_QUADS)

        glColor3f(1f,0f,0f)
        glVertex3f(-small, -small, -small)
        glVertex3f(-small, small, -small)
        glVertex3f(small, small, -small)
        glVertex3f(small, -small, -small)

        glColor3f(0f, 1f, 0f)
        glVertex3f(-small, -small, small)
        glVertex3f(-small, small, small)
        glVertex3f(small, small, small)
        glVertex3f(small, -small, small)

        glColor3f(0f, 0f, 1f)
        glVertex3f(-small, small, -small)
        glVertex3f(small, small, -small)
        glVertex3f(small, small, small)
        glVertex3f(-small, small, small)

        glColor3f(1f, 1f, 0f)
        glVertex3f(-small, -small, -small)
        glVertex3f(small, -small, -small)
        glVertex3f(small, -small, small)
        glVertex3f(-small, -small, small)

        glColor3f(1f, 1f, 1f)
        glVertex3f(-small, -small, -small)
        glVertex3f(-small, small, -small)
        glVertex3f(-small, small, small)
        glVertex3f(-small, -small, small)

        glColor3f(1f, 0f, .5f)
        glVertex3f(small, -small, -small)
        glVertex3f(small, small, -small)
        glVertex3f(small, small, small)
        glVertex3f(small, -small, small)
        glEnd()

        glPopMatrix()
    }

    private fun rotateCoords() {
        val cabView = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            -.5f*cos(PI / 4).toFloat(),  -.5f*sin(PI / 4).toFloat(), -1f, 0f,
            0f, 0f, 0f, 1f
        )
        glMultMatrixf(cabView)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
    }

    private fun loop(){
        GL.createCapabilities()

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEFT)

        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE) {
            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()

            rotateCoords()

            glClearColor(colorMain[0], colorMain[1], colorMain[2], 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            glPolygonMode(GL_FRONT_AND_BACK, mode)

            render()
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }
    }

    private fun drawCoords() {
        glPushMatrix()
        glLoadIdentity()

        glColor3f(.5f, .5f, .5f)

        glBegin(GL_LINE_STRIP)
        glVertex3f(0f, 0f, 0f)
        glVertex3f(10f, 0f, 0f)
        glEnd()

        glBegin(GL_LINE_STRIP)
        glVertex3f(0f, 0f, 0f)
        glVertex3f(0f, 10f, 0f)
        glEnd()

        glBegin(GL_LINE_STRIP)
        glVertex3f(0f, 0f, 0f)
        glVertex3f(0f, 0f, 10f)
        glEnd()
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

    fun changeMode() {
        mode = if (mode == GL_FILL) {
            GL_LINE
        } else GL_FILL
    }

}

fun main() {
    Lab2().run()
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0
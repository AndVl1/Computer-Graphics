package example

import org.lwjgl.glfw.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import kotlin.math.*


class Lab3 {
    private var window: Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var colorMain = floatArrayOf(.0f, .0f, .0f)
    private var rotationX = .0f
    private var rotationY = .0f
    private var rotationZ = .0f
    private var moveX = .0f
    private var moveY = .0f
    private var moveZ = .0f
    private var sizeX = .6f
    private var sizeY = .6f
    private var sizeZ = .6f
    private var mode = GL_LINE
    private val small = .15f
    private var faces = 3
    private var step = (2 * PI / faces.toFloat()).toFloat()
    private var height = 1f
    private var top = ArrayList<ArrayList<Triple<Float, Float, Float>>>()
    private var bottom = ArrayList<ArrayList<Triple<Float, Float, Float>>>()
    private var sidesTop = ArrayList<Triple<Float, Float, Float>>()
    private var sidesBottom = ArrayList<Triple<Float, Float, Float>>()
    private var linesCount = 2
    private var lines = ArrayList<ArrayList<Triple<Float, Float, Float>>>()

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        val WIDTH = 1500
        val HEIGHT = 1000
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "LAB2", MemoryUtil.NULL, MemoryUtil.NULL)
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
                        GLFW.GLFW_KEY_UP -> rotationX += 5f
                        GLFW.GLFW_KEY_DOWN -> rotationX -= 5f
                        GLFW.GLFW_KEY_LEFT -> rotationY += 5f
                        GLFW.GLFW_KEY_RIGHT -> rotationY -= 5f
                        GLFW.GLFW_KEY_SPACE -> changeMode()
                        GLFW.GLFW_KEY_W -> moveY += .15f
                        GLFW.GLFW_KEY_S -> moveY -= .15f
                        GLFW.GLFW_KEY_A -> moveX -= .15f
                        GLFW.GLFW_KEY_D -> moveX += .15f
                        GLFW.GLFW_KEY_O -> moveZ += .15f
                        GLFW.GLFW_KEY_K -> moveZ -= .15f
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
                        GLFW.GLFW_KEY_PAGE_UP -> {
                            faces += 1
                            step = (2 * PI / faces).toFloat()
                            calc()
                        }
                        GLFW.GLFW_KEY_PAGE_DOWN -> {
                            if (faces > 3) {
                                faces -= 1
                                step = (2 * PI / faces).toFloat()
                                calc()
                            }
                        }
                        GLFW.GLFW_KEY_HOME -> {
                            if (linesCount > 2) {
                                linesCount--
                                calc()
                            }
                        }
                        GLFW.GLFW_KEY_END -> {
                            linesCount++
                            calc()
                        }
                    }
                }
            }
        }.also { keyCallback = it })

        GLFW.glfwSetMouseButtonCallback(window, object : GLFWMouseButtonCallbackI {
            override fun invoke(window: Long, button: Int, action: Int, mods: Int) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS){
                    var x = DoubleArray(1)
                    var y = DoubleArray(1)
                    GLFW.glfwGetCursorPos(window, x, y)
                    print("${x[0]} - ${y[0]}")
                }
            }

        })

        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }

    private fun render() {
        drawCoords()
        drawPyramid()
        drawCube()
    }

    private fun calc() {
        var angle = 0f
        val lineStep = height / linesCount

        //SIDE
        sidesTop.clear()
        sidesBottom.clear()
        while (angle <= 2 * PI) {
            val x = cos(angle)
            val y = sin(angle)
            sidesTop.add(Triple(x/2, height, y/2))
            sidesBottom.add(Triple(x, 0f, y))
            angle += step
        }
        sidesTop.add(sidesTop[0])
        sidesBottom.add(sidesBottom[0])


        //LINES
        lines.clear()
        var i: Float = lineStep
        var j = 0
        while (i < height) {
            angle = 0f
            val level = ArrayList<Triple<Float, Float, Float>>()
            while (angle < 2*PI){
                val k = 1 - i / 2
                val x = cos(angle)
                val y = sin(angle)
                level.add(Triple(x * k, i, y * k))
                angle+=step
                println("$k")
            }
            j++
            lines.add(level)
            println("--- $i")
            i += lineStep
        }

        //TOP
        top.clear()
        var k = (.5f / linesCount)
        while(k <= .5f) {
            val tmp = ArrayList<Triple<Float, Float, Float>>()
            tmp.add(Triple(0f, height, 0f))
            angle = 0f
            while (angle <= 2 * PI) {
                tmp.add(Triple(k * cos(angle), height,  k * sin(angle)))
                angle += step
            }
            tmp.add(tmp[1])
            top.add(tmp)
            k += (.5f/linesCount)
        }

        //BOTTOM
        bottom.clear()
        k = 1f / linesCount
        while (k <= 1) {
            val tmp = ArrayList<Triple<Float, Float, Float>>()
            tmp.add(Triple(0f, 0f, 0f))
            angle = 0f
            while (angle <= 2 * PI) {
                tmp.add(Triple(cos(angle) * k, 0f, sin(angle) * k))
                angle += step
            }
            tmp.add(tmp[1])
            bottom.add(tmp)
            k += 1f / linesCount
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

    private fun drawPyramid() {
        glPushMatrix()

        glTranslatef(moveX, moveY, moveZ)
        glRotatef(rotationX, 1f, 0f, 0f)
        glRotatef(rotationY, 0f, 1f, 0f)
        glRotatef(rotationZ, 0f, 0f, 1f)
        glScalef(sizeX, sizeY, sizeZ)

        // ***** //

        //SIDES
        glBegin(GL_QUAD_STRIP)
        for (s in sidesTop.indices) {
            glColor3f(0f, .5f, .3f)
            glVertex3f(sidesBottom[s].first, sidesBottom[s].second, sidesBottom[s].third)
            glVertex3f(sidesTop[s].first, sidesTop[s].second, sidesTop[s].third)
//            glVertex3f(0f, height*2, 0f)
        }
        glEnd()

        //LINES

        for (line in lines) {
            glBegin(GL_LINE_STRIP)
            for (l in line) {
                glVertex3f(l.first, l.second, l.third)
            }
            glVertex3f(line[0].first, line[0].second, line[0].third)
            glEnd()
        }


        //TOP


        for (tmp in top) {
            glBegin(GL_TRIANGLE_FAN)
            glColor3f(0.0f, 0.0f, 1.0f)
            for (t in tmp) {
                glColor3f(1.0f, 0.0f, 0.0f)
                glVertex3f(t.first, t.second, t.third)
            }
            glEnd()
        }

        //BOTTOM
        for (tmp in bottom) {
            glBegin(GL_TRIANGLE_FAN)
            glColor3f(0.0f, 0.0f, 1.0f)
            for (b in tmp) {
                glColor3f(1.0f, 0.0f, 0.0f)
                glVertex3f(b.first, b.second, b.third)
            }

            glEnd()
        }

        glPopMatrix()
    }


    private fun drawCube() {
        glPushMatrix()
        glLoadIdentity()

        glTranslatef(-.7f, -.7f, 0f)

        glBegin(GL_QUADS)

        glColor3f(1f, 0f, 0f)
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
            -.5f * cos(PI / 4).toFloat(), -.5f * cos(PI / 4).toFloat(), -1f, 0f,
            0f, 0f, 0f, 1f
        )
        glMultMatrixf(cabView)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
    }

    private fun loop() {
        GL.createCapabilities()

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEFT)

        calc()

        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE) {
            glViewport(0, 0, 1000, 1000)
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

    fun run() {
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
    Lab3().run()
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0

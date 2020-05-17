package example

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.matthiasmann.twl.utils.PNGDecoder
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.system.MemoryUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.ByteBuffer
import kotlin.math.*

/**
 * А1. использование сфокусированного источника света;
 *
 * Б3. моделирование движения тела (с заданной начальной скоростью)
 * при условии абсолютно упругого отражения объекта от границ
 * некоторого ограничивающего объема (регулярной формы);
 *
 * В2. использование текстуры для определения свойств поверхности
 * (модулирование коэффициента диффузного отражения);
 *
 * 1 - сохранить
 * 2 - загрузить
 */

class Lab6 {
    private var window: Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var state = State()
    private val vpWidth = 1500
    private val vpHeight = 1000
    private var top = ArrayList<ArrayList<Triple<Float, Float, Float>>>()
    private var bottom = ArrayList<ArrayList<Triple<Float, Float, Float>>>()
    private var sidesTop = ArrayList<Triple<Float, Float, Float>>()
    private var sidesBottom = ArrayList<Triple<Float, Float, Float>>()
    private var step = (2 * PI / state.faces.toFloat()).toFloat()
    private var lines = ArrayList<ArrayList<Triple<Float, Float, Float>>>()

    class State{
        var colorMain = floatArrayOf(.0f, .0f, .0f)
        var rotationX = .0f
        var rotationY = .0f
        var rotationZ = .0f
        var moveX = .0f
        var moveY = .0f
        var moveZ = .0f
        var sizeX = .6f
        var sizeY = .6f
        var sizeZ = .6f
        var mode = GL_LINE
        var faces = 30
        var height = 1f
        var linesCount = 50
    }

//    private lateinit var texture: example.texture.Texture

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        window = GLFW.glfwCreateWindow(vpWidth, vpHeight, "LAB6", MemoryUtil.NULL, MemoryUtil.NULL)
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
                        GLFW.GLFW_KEY_UP -> state.rotationX += 5f
                        GLFW.GLFW_KEY_DOWN -> state.rotationX -= 5f
                        GLFW.GLFW_KEY_LEFT -> state.rotationY += 5f
                        GLFW.GLFW_KEY_RIGHT -> state.rotationY -= 5f
                        GLFW.GLFW_KEY_SPACE -> changeMode()
                        GLFW.GLFW_KEY_W -> state.moveY += .15f
                        GLFW.GLFW_KEY_S -> state.moveY -= .15f
                        GLFW.GLFW_KEY_A -> state.moveX -= .15f
                        GLFW.GLFW_KEY_D -> state.moveX += .15f
                        GLFW.GLFW_KEY_O -> state.moveZ += .15f
                        GLFW.GLFW_KEY_K -> state.moveZ -= .15f
                        GLFW.GLFW_KEY_P -> {
                            state.sizeX += .01f
                            state.sizeY += .01f
                            state.sizeZ += .01f
                        }
                        GLFW.GLFW_KEY_M -> {
                            state.sizeX -= .01f
                            state.sizeY -= .01f
                            state.sizeZ -= .01f
                        }
                        GLFW.GLFW_KEY_PAGE_UP -> {
                            state.faces += 1
                            step = (2 * PI / state.faces).toFloat()
                            calc()
                        }
                        GLFW.GLFW_KEY_PAGE_DOWN -> {
                            if (state.faces > 3) {
                                state.faces -= 1
                                step = (2 * PI / state.faces).toFloat()
                                calc()
                            }
                        }
                        GLFW.GLFW_KEY_HOME -> {
                            if (state.linesCount > 2) {
                                state.linesCount--
                                calc()
                            }
                        }
                        GLFW.GLFW_KEY_END -> {
                            state.linesCount++
                            calc()
                        }
                        GLFW.GLFW_KEY_1 -> {
                            val gson = GsonBuilder()
                                .setPrettyPrinting()
                                .create()
                            val fileWriter = FileWriter("state.json")
                            gson.toJson(state, fileWriter)
                            fileWriter.flush()
                            fileWriter.close()
                        }
                        GLFW.GLFW_KEY_2 -> {
                            try {
                                val stream = FileInputStream("state.json")
                                val jsonBytes = stream.readAllBytes()
                                val json = String(jsonBytes)
                                state = Gson().fromJson(json, state::class.java)
                                stream.close()
                                calc()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }.also { keyCallback = it })

        GLFW.glfwSetMouseButtonCallback(window) { window, button, action, mods ->
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS) {
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                GLFW.glfwGetCursorPos(window, x, y)
                print("${x[0]} - ${y[0]}")
            }
        }

        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - vpWidth) / 2, (vidMode.height() - vpHeight) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }

    private fun render() {
//        drawCoords()
        drawPyramid()
        drawCube()
//        drawCubeTextured()
        light()
    }

    private fun calc() {
        var angle = 0f
        val lineStep = state.height / state.linesCount

        //SIDE
        sidesTop.clear()
        sidesBottom.clear()
        while (angle <= 2 * PI) {
            val x = cos(angle)
            val y = sin(angle)
        sidesTop.add(Triple(x / 2, state.height, y / 2))
        sidesBottom.add(Triple(x, 0f, y))
            angle += step
        }
        sidesTop.add(sidesTop[0])
        sidesBottom.add(sidesBottom[0])


        //LINES
        lines.clear()
        var i: Float = lineStep
        var j = 0
        while (i < state.height) {
            angle = 0f
            val level = ArrayList<Triple<Float, Float, Float>>()
            while (angle < 2 * PI) {
                val k = 1 - i / 2
                val x = cos(angle)
                val y = sin(angle)
                level.add(Triple(x * k, i, y * k))
                angle += step
                println("$k")
            }
            j++
            lines.add(level)
            println("--- $i")
            i += lineStep
        }

        //TOP
        top.clear()
        var k = (.5f / state.linesCount)
        while (k <= .5f) {
            val tmp = ArrayList<Triple<Float, Float, Float>>()
            tmp.add(Triple(0f, state.height, 0f))
            angle = 0f
            while (angle <= 2 * PI) {
                tmp.add(Triple(k * cos(angle), state.height, k * sin(angle)))
                angle += step
            }
            tmp.add(tmp[1])
            top.add(tmp)
            k += (.5f / state.linesCount)
        }

        //BOTTOM
        bottom.clear()
        k = 1f / state.linesCount
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
            k += 1f / state.linesCount
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

        glTranslatef(state.moveX, state.moveY, state.moveZ)
        glRotatef(state.rotationX, 1f, 0f, 0f)
        glRotatef(state.rotationY, 0f, 1f, 0f)
        glRotatef(state.rotationZ, 0f, 0f, 1f)
        glScalef(state.sizeX, state.sizeY, state.sizeZ)

        // ***** //

        //SIDES
        glBegin(GL_QUAD_STRIP)
        for (s in sidesTop.indices) {
            glColor3f(0f, .5f, .3f)
            glVertex3f(sidesBottom[s].first, sidesBottom[s].second, sidesBottom[s].third)
            glVertex3f(sidesTop[s].first, sidesTop[s].second, sidesTop[s].third)
        }
        glEnd()

        //LINES

        if (state.mode != GL_FILL)
            for (line in lines) {
                glBegin(GL_LINE_STRIP)
                for (l in line) {
                    glVertex3f(l.first, l.second, l.third)
                }
                glVertex3f(line[0].first, line[0].second, line[0].third)
                glEnd()
            }


        //TOP

//        if (mode != GL_FILL) {
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
//        } else {
//            glBegin(GL_TRIANGLE_FAN)
//            glColor3f(0.0f, 0.0f, 1.0f)
//            for (b in bottom.last()) {
//                glColor3f(1.0f, 0.0f, 0.0f)
//                glVertex3f(b.first, b.second, b.third)
//            }
//            glEnd()
//
//            glBegin(GL_TRIANGLE_FAN)
//            glColor3f(0.0f, 0.0f, 1.0f)
//            for (t in top.last()) {
//                glColor3f(1.0f, 0.0f, 0.0f)
//                glVertex3f(t.first, t.second, t.third)
//            }
//            glEnd()
//        }

        glPopMatrix()
    }


    private fun drawCube() {
        glPushMatrix()
        glLoadIdentity()

        glTranslatef(-.7f, -.7f, 0f)

        glBegin(GL_QUADS)

        glColor3f(1f, 0f, 0f)
        glVertex3f(-SMALL, -SMALL, -SMALL)
        glVertex3f(-SMALL, SMALL, -SMALL)
        glVertex3f(SMALL, SMALL, -SMALL)
        glVertex3f(SMALL, -SMALL, -SMALL)

        glColor3f(0f, 1f, 0f)
        glVertex3f(-SMALL, -SMALL, SMALL)
        glVertex3f(-SMALL, SMALL, SMALL)
        glVertex3f(SMALL, SMALL, SMALL)
        glVertex3f(SMALL, -SMALL, SMALL)

        glColor3f(0f, 0f, 1f)
        glVertex3f(-SMALL, SMALL, -SMALL)
        glVertex3f(SMALL, SMALL, -SMALL)
        glVertex3f(SMALL, SMALL, SMALL)
        glVertex3f(-SMALL, SMALL, SMALL)

        glColor3f(1f, 1f, 0f)
        glVertex3f(-SMALL, -SMALL, -SMALL)
        glVertex3f(SMALL, -SMALL, -SMALL)
        glVertex3f(SMALL, -SMALL, SMALL)
        glVertex3f(-SMALL, -SMALL, SMALL)

        glColor3f(1f, 1f, 1f)
        glVertex3f(-SMALL, -SMALL, -SMALL)
        glVertex3f(-SMALL, SMALL, -SMALL)
        glVertex3f(-SMALL, SMALL, SMALL)
        glVertex3f(-SMALL, -SMALL, SMALL)

        glColor3f(1f, 0f, .5f)
        glVertex3f(SMALL, -SMALL, -SMALL)
        glVertex3f(SMALL, SMALL, -SMALL)
        glVertex3f(SMALL, SMALL, SMALL)
        glVertex3f(SMALL, -SMALL, SMALL)
        glEnd()

        glPopMatrix()
    }

//    private fun drawCubeTextured() {
//
//        glPushMatrix()
//        glLoadIdentity()
//        glColor4f(1f, 1f, 1f, 1f)
//        glBindTexture(GL_TEXTURE_2D, texture.id)
//
//        glMatrixMode(GL_MODELVIEW)
//
//        glTranslatef(-.7f, -.7f, 0f)
//
//        glBegin(GL_QUADS)
//
//        glTexCoord2f(1f, 1f)
//        glVertex3f(-small, -small, -small)
//        glTexCoord2f(1f, 0f)
//        glVertex3f(-small, small, -small)
//        glTexCoord2f(0f, 0f)
//        glVertex3f(small, small, -small)
//        glTexCoord2f(0f, 1f)
//        glVertex3f(small, -small, -small)
//
//        glTexCoord2f(0f, 1f)
//        glVertex3f(-small, -small, small)
//        glTexCoord2f(0f, 0f)
//        glVertex3f(-small, small, small)
//        glTexCoord2f(1f, 0f)
//        glVertex3f(small, small, small)
//        glTexCoord2f(1f, 1f)
//        glVertex3f(small, -small, small)
//
//        glTexCoord2f(0f, 0f)
//        glVertex3f(-small, small, -small)
//        glTexCoord2f(1f, 0f)
//        glVertex3f(small, small, -small)
//        glTexCoord2f(1f, 1f)
//        glVertex3f(small, small, small)
//        glTexCoord2f(0f, 1f)
//        glVertex3f(-small, small, small)
//
//        glTexCoord2f(1f, 0f)
//        glVertex3f(-small, -small, -small)
//        glTexCoord2f(1f, 1f)
//        glVertex3f(small, -small, -small)
//        glTexCoord2f(0f, 1f)
//        glVertex3f(small, -small, small)
//        glTexCoord2f(0f, 0f)
//        glVertex3f(-small, -small, small)
//
//
//        glTexCoord2f(1f, 0f)
//        glVertex3f(-small, -small, -small)
//        glTexCoord2f(0f, 0f)
//        glVertex3f(-small, small, -small)
//        glTexCoord2f(0f, 1f)
//        glVertex3f(-small, small, small)
//        glTexCoord2f(1f, 1f)
//        glVertex3f(-small, -small, small)
//
//        glTexCoord2f(1f, 1f)
//        glVertex3f(small, -small, -small)
//        glTexCoord2f(1f, 0f)
//        glVertex3f(small, small, -small)
//        glTexCoord2f(0f, 0f)
//        glVertex3f(small, small, small)
//        glTexCoord2f(0f, 1f)
//        glVertex3f(small, -small, small)
//
//        glEnd()
//
//        glPopMatrix()
//
//        glBindTexture(GL_TEXTURE_2D, 0)
//        glDisable(GL_COLOR_MATERIAL)
//    }

    private fun rotateCoords() {
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
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

    private fun light() {
//      val lightPos = floatArrayOf(.2f, .4f, 5.0f, 1.0f)
        glEnable(GL_COLOR_MATERIAL)

        val lightPos = floatArrayOf(.2f, .4f, 8.0f, 3f)
        glLightfv(GL_LIGHT0, GL_POSITION, lightPos)

        val ambient = floatArrayOf(.2f, .4f, .2f, 1f)
        val diffuse = floatArrayOf(0.4f, 0.7f, 0.2f, 1.0f)
        val specular = floatArrayOf(0.2f, 0.4f, 0.2f, 1.0f)
        val cutoff = PI.toFloat()
        val exponent = 0f
        val direction = floatArrayOf(0f, 0f, -1f, 0f)

        glLightfv(GL_LIGHT0, GL_AMBIENT, ambient)
        glLightfv(GL_LIGHT0, GL_DIFFUSE, diffuse)
        glLightfv(GL_LIGHT0, GL_SPECULAR, specular)

        glLightf(GL_LIGHT0, GL_SPOT_CUTOFF, cutoff)
        glLightf(GL_LIGHT0, GL_SPOT_EXPONENT, exponent)
        glLightfv(GL_LIGHT0, GL_SPOT_DIRECTION, direction)

//        GL11.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, floatArrayOf(1f, 0f, 0f, 1f))
        glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, 1f)
        glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, floatArrayOf(1f, 1f, 1f, 1f))

        glEnable(GL_NORMALIZE)
        glEnable(GL_LIGHTING)
        glEnable(GL_LIGHT0)
    }

    private fun loop() {
        GL.createCapabilities()

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEFT)

        try {
//            texture = example.texture.Texture.loadTexture("wood")
//            texture = Texture.loadTexture("wood")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        calc()

        while (GLFW.glfwWindowShouldClose(window).toInt() == GLFW.GLFW_FALSE) {
            glViewport(0, 0, min(vpHeight, vpWidth), min(vpHeight, vpWidth))


            rotateCoords()

            glClearColor(state.colorMain[0], state.colorMain[1], state.colorMain[2], 1.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            glPolygonMode(GL_FRONT_AND_BACK, state.mode)

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

    private fun changeMode() {
        state.mode = if (state.mode == GL_FILL) {
            GL_LINE
        } else GL_FILL
    }

    companion object {
        const val SMALL = .15f
    }
}

fun main() {
    Lab6().run()
}

class Texture(val id: Int) {
    //("texture/$name.png")
    companion object {
        fun loadTexture(name: String): Texture {
//            texture = example.texture.Texture.loadTexture("/")

            println("load texture/$name.png")
            //load png file

            val decoder =
                PNGDecoder(Lab6::class.java.getResourceAsStream("/texture/$name.png"))

            //create a byte buffer big enough to store RGBA values

            val buffer =
                ByteBuffer.allocateDirect(4 * decoder.width * decoder.height)

            //decode
            decoder.decode(buffer, decoder.width * 4, PNGDecoder.Format.RGBA)

            //flip the buffer so its ready to read
            buffer.flip()

            //create a texture
            val id = glGenTextures()

            //bind the texture
            glBindTexture(GL_TEXTURE_2D, id)

            //tell opengl how to unpack bytes
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

            //set the texture parameters, can be GL_LINEAR or GL_NEAREST
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

            //upload texture
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                decoder.width,
                decoder.height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                buffer
            )

            // Generate Mip Map
            glGenerateMipmap(GL_TEXTURE_2D)

            return Texture(id)
        }
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0
package example

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
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
	private val normalsTop = ArrayList<Triple<Float, Float, Float>>()
	private val normalsSides = ArrayList<Triple<Float, Float, Float>>()
	private lateinit var texture: Texture

	class State {
		var colorMain = floatArrayOf(.0f, .0f, .0f)
		var rotationX = .0f
		var rotationY = .0f
		var rotationZ = .0f
		var moveX = .0f
		var moveY = .0f
		var moveZ = .0f
		var sizeX = .3f
		var sizeY = .3f
		var sizeZ = .3f
		var mode = GL_LINE
		var faces = 4
		var height = 1f
		var linesCount = 6
		var cutoff = PI.toFloat()
		var exponent = 0f
		var enableTexture = false
		var isLight = false
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
						GLFW.GLFW_KEY_SPACE -> {
							if (!state.enableTexture)
								changeMode()
						}
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
						GLFW.GLFW_KEY_T -> {
							if (!state.enableTexture)
								state.mode = GL_FILL
							state.enableTexture = !state.enableTexture
						}
						GLFW.GLFW_KEY_L -> {
							state.isLight = !state.isLight
						}
						GLFW.GLFW_KEY_0 -> {
							state.cutoff += PI.toFloat()
						}
						GLFW.GLFW_KEY_9 -> {
							state.cutoff -= PI.toFloat()
						}
					}
				}
			}
		}.also { keyCallback = it })

		GLFW.glfwSetMouseButtonCallback(window) { window, button, action, _ ->
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
//		if (state.enableTexture) {
			drawPyramidTextured()
//			drawCubeTextured()
//		} else {
//			drawPyramid()
			drawCube()
//		}
		if (state.isLight) {
			light()
			glEnable(GL_LIGHTING)
		} else {
			glDisable(GL_LIGHTING)
		}
	}

	private fun calc() {
		var angle = 0f
		val lineStep = state.height / state.linesCount

		//SIDE
		sidesTop.clear()
		sidesBottom.clear()
		var count = 0
		while (angle <= 2 * PI) {
			val x = cos(angle)
			val y = sin(angle)
			sidesTop.add(Triple(x / 2, state.height, y / 2))
			sidesBottom.add(Triple(x, 0f, y))
			angle += step
			if (count > 0) {
				var c = cross(sidesTop.last(), sidesTop[sidesTop.lastIndex-1]
					, sidesBottom.last(), sidesBottom[sidesBottom.lastIndex-1])
				c = c.normalize()
				normalsSides.add(c)
			}
			count++
		}
		println(count)
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
			}
			j++
			lines.add(level)
			i += lineStep
		}

		//TOP
		top.clear()
		var k = 0.toFloat()
		while (k <= TOP_UNTIL_THAT) {
			val tmp = ArrayList<Triple<Float, Float, Float>>()
			tmp.add(Triple(0f, state.height, 0f))
			angle = 0f

			while (angle <= 2 * PI) {
				tmp.add(Triple(k * cos(angle), state.height, k * sin(angle)))
				angle += step
			}

			tmp.add(tmp[1])
			top.add(tmp)
			k += .5f / state.linesCount
		}


		//BOTTOM
		bottom.clear()
		k = 1f / state.linesCount
		while (k <= BOTTOM_UNTIL_THAT) {
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

	private fun drawPyramidTextured() {
		glPushMatrix()

		glTranslatef(state.moveX, state.moveY, state.moveZ)
		glRotatef(state.rotationX, 1f, 0f, 0f)
		glRotatef(state.rotationY, 0f, 1f, 0f)
		glRotatef(state.rotationZ, 0f, 0f, 1f)
		glScalef(state.sizeX, state.sizeY, state.sizeZ)

		// ***** //

		//SIDES
		texture.bind()
		glBegin(GL_QUAD_STRIP)
		var i = 0
		var count = 0
		for (s in sidesTop.indices) {
			//0,0 | 0,1
			glTexCoord2f(0f, i.toFloat())
			glVertex3f(sidesBottom[s].first, sidesBottom[s].second, sidesBottom[s].third)
			//1,0 | 1,1
			glTexCoord2f(1f, i.toFloat())
			glVertex3f(sidesTop[s].first, sidesTop[s].second, sidesTop[s].third)
			i = (i + 1)%2
			if (i%2 == 0) {
				glNormal(normalsSides[count])
				count++
			}
		}
		glEnd()

		//LINES

		if (state.mode != GL_FILL)
			for (line in lines) {
				texture.bind()
				glBegin(GL_LINE_STRIP)
				for (l in line) {
					glVertex3f(l.first, l.second, l.third)
				}
				glVertex3f(line[0].first, line[0].second, line[0].third)
				glEnd()
			}


		//TOP

        if (state.mode != GL_FILL) {
			for (tmp in top) {
				glBegin(GL_TRIANGLE_FAN)
				for (t in tmp) {
					glVertex3f(t.first, t.second, t.third)
				}
				glNormal(Triple(0f, 0f, 1f))
				glEnd()
			}
		} else {
			texture.bind()
			glBegin(GL_TRIANGLE_FAN)
			glTexCoord2f(.5f, 1f)
			i = 0
			for (t in top.last()) {
				glTexCoord2f((i % 2).toFloat(), 0.toFloat())
				glVertex3f(t.first, t.second, t.third)
				i++
			}
			glNormal(Triple(0f, 0f, 1f))
			glEnd()
		}

		//BOTTOM
		if (state.mode != GL_FILL){
			for (tmp in bottom) {
				glBegin(GL_TRIANGLE_FAN)
				for (b in tmp) {
					glVertex3f(b.first, b.second, b.third)
				}
				glNormal(Triple(0f, 1f, 0f))
				glEnd()
			}
		} else {
			texture.bind()
			glBegin(GL_TRIANGLE_FAN)
			glTexCoord2f(.5f, 1f)
			for (t in bottom.last()) {
				glTexCoord2f((i % 2).toFloat(), 0.toFloat())
				glVertex3f(t.first, t.second, t.third)
				i++
			}
			glNormal(Triple(0f, 1f, 0f))
			glEnd()
		}

		glPopMatrix()
	}

	private fun drawCube() {
		glPushMatrix()
		glLoadIdentity()

		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
		glDisable(GL_TEXTURE_2D)

		glBegin(GL_QUADS)

		glVertex3f(-CUBE, -CUBE, -CUBE)
		glVertex3f(-CUBE, CUBE, -CUBE)
		glVertex3f(CUBE, CUBE, -CUBE)
		glVertex3f(CUBE, -CUBE, -CUBE)

		glVertex3f(-CUBE, -CUBE, CUBE)
		glVertex3f(-CUBE, CUBE, CUBE)
		glVertex3f(CUBE, CUBE, CUBE)
		glVertex3f(CUBE, -CUBE, CUBE)

		glVertex3f(-CUBE, CUBE, -CUBE)
		glVertex3f(CUBE, CUBE, -CUBE)
		glVertex3f(CUBE, CUBE, CUBE)
		glVertex3f(-CUBE, CUBE, CUBE)

		glVertex3f(-CUBE, -CUBE, -CUBE)
		glVertex3f(CUBE, -CUBE, -CUBE)
		glVertex3f(CUBE, -CUBE, CUBE)
		glVertex3f(-CUBE, -CUBE, CUBE)

		glVertex3f(-CUBE, -CUBE, -CUBE)
		glVertex3f(-CUBE, CUBE, -CUBE)
		glVertex3f(-CUBE, CUBE, CUBE)
		glVertex3f(-CUBE, -CUBE, CUBE)

		glVertex3f(CUBE, -CUBE, -CUBE)
		glVertex3f(CUBE, CUBE, -CUBE)
		glVertex3f(CUBE, CUBE, CUBE)
		glVertex3f(CUBE, -CUBE, CUBE)
		glEnd()

		glEnable(GL_TEXTURE_2D)
		glPopMatrix()
	}

	private fun drawCubeTextured() {
		glPushMatrix()
		glLoadIdentity()
//		glColor4f(1f, 1f, 1f, 1f)

		glTranslatef(-.7f, -.7f, 0f)
		texture.bind()

		glBegin(GL_QUADS)

		glTexCoord2f(0f, 0f)
		glVertex3f(-CUBE, -CUBE, CUBE)
		glTexCoord2f(1f, 0f)
		glVertex3f(CUBE, -CUBE, CUBE)
		glTexCoord2f(1f, 1f)
		glVertex3f(CUBE, CUBE, CUBE)
		glTexCoord2f(0f, 1f)
		glVertex3f(-CUBE, CUBE, CUBE)

		glTexCoord2f(1f, 0f)
		glVertex3f(-CUBE, -CUBE, -CUBE)
		glTexCoord2f(1f, 1f)
		glVertex3f(-CUBE, CUBE, -CUBE)
		glTexCoord2f(0f, 1f)
		glVertex3f(CUBE, CUBE, -CUBE)
		glTexCoord2f(0f, 0f)
		glVertex3f(CUBE, -CUBE, -CUBE)

		glTexCoord2f(0.0f, 1.0f)
		glVertex3f(-CUBE, CUBE, -CUBE)  // Верх лево
		glTexCoord2f(0.0f, 0.0f)
		glVertex3f(-CUBE, CUBE, CUBE)  // Низ лево
		glTexCoord2f(1.0f, 0.0f)
		glVertex3f(CUBE, CUBE, CUBE)  // Низ право
		glTexCoord2f(1.0f, 1.0f)
		glVertex3f(CUBE, CUBE, -CUBE)  // Верх право

		// Нижняя грань
		glTexCoord2f(1.0f, 1.0f)
		glVertex3f(-CUBE, -CUBE, -CUBE)  // Верх право
		glTexCoord2f(0.0f, 1.0f)
		glVertex3f(CUBE, -CUBE, -CUBE)  // Верх лево
		glTexCoord2f(0.0f, 0.0f)
		glVertex3f(CUBE, -CUBE, CUBE)  // Низ лево
		glTexCoord2f(1.0f, 0.0f)
		glVertex3f(-CUBE, -CUBE, CUBE)  // Низ право

		// Правая грань
		glTexCoord2f(1.0f, 0.0f)
		glVertex3f(CUBE, -CUBE, -CUBE)  // Низ право
		glTexCoord2f(1.0f, 1.0f)
		glVertex3f(CUBE, CUBE, -CUBE)  // Верх право
		glTexCoord2f(0.0f, 1.0f)
		glVertex3f(CUBE, CUBE, CUBE)  // Верх лево
		glTexCoord2f(0.0f, 0.0f)
		glVertex3f(CUBE, -CUBE, CUBE)  // Низ лево

		// Левая грань
		glTexCoord2f(0.0f, 0.0f)
		glVertex3f(-CUBE, -CUBE, -CUBE)  // Низ лево
		glTexCoord2f(1.0f, 0.0f)
		glVertex3f(-CUBE, -CUBE, CUBE)  // Низ право
		glTexCoord2f(1.0f, 1.0f)
		glVertex3f(-CUBE, CUBE, CUBE)  // Верх право
		glTexCoord2f(0.0f, 1.0f)
		glVertex3f(-CUBE, CUBE, -CUBE)  // Верх лево*/

		glEnd()

        glPopMatrix()


//		glBindTexture(GL_TEXTURE_2D, 0)
//		glDisable(GL_COLOR_MATERIAL)
	}

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
//        glDisable(GL_COLOR_MATERIAL)

		val lightPos = floatArrayOf(0f, 0f, 1f, 1f)
		glLightfv(GL_LIGHT0, GL_POSITION, lightPos)

		val materialAmbient = floatArrayOf(.9f, .35f, .51f, 1f)
		val materialDiffuse = floatArrayOf(.9f, .35f, .51f, 1f)
		val materialSpecular = floatArrayOf(.43f, .43f, .43f, 1f)
		glMaterialfv(GL_FRONT, GL_AMBIENT, materialAmbient)
		glMaterialfv(GL_FRONT, GL_DIFFUSE, materialDiffuse)
		glMaterialfv(GL_FRONT, GL_SPECULAR, materialSpecular)

		val lightAmbient = floatArrayOf(.45f, .45f, .45f, 1f)
		val lightDiffuse = floatArrayOf(0.65f, 0.45f, 0.45f, 1.0f)
		val lightSpecular = floatArrayOf(0.63f, 0.43f, 0.43f, 1.0f)
		val cutoff = state.cutoff
		val exponent = state.exponent
		val direction = floatArrayOf(0f, 0f, -1f, 0f)

		glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient)
		glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse)
		glLightfv(GL_LIGHT0, GL_SPECULAR, lightSpecular)

		glLightf(GL_LIGHT0, GL_SPOT_CUTOFF, cutoff)
		glLightf(GL_LIGHT0, GL_SPOT_EXPONENT, exponent)
		glLightfv(GL_LIGHT0, GL_SPOT_DIRECTION, direction)

//        GL11.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, floatArrayOf(1f, 0f, 0f, 1f))
		glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, 1f)
		glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, floatArrayOf(1f, 1f, 1f, 1f))

		glEnable(GL_LIGHT0)
	}

	private fun loop() {
		GL.createCapabilities()

		glEnable(GL_DEPTH_TEST)
		glDepthFunc(GL_LEFT)

		try {
			glEnable(GL_TEXTURE_2D)
			glDisable(GL_COLOR_MATERIAL)
			glEnable(GL_NORMALIZE)
			texture = Texture.loadTexture("wood")
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

			if (state.enableTexture) {
				glEnable(GL_TEXTURE_2D)
			} else {
				glDisable(GL_TEXTURE_2D)
			}

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
		const val CUBE = .7f
		const val BOTTOM_UNTIL_THAT = 1.0000003f
		const val TOP_UNTIL_THAT = .51f
	}
}


fun main() {
	Lab6().run()
}

class Texture {
	var id: Int = glGenTextures()
	var width: Int = 0
	var height: Int = 0

	fun bind() {
		glBindTexture(id, GL_TEXTURE_2D)
	}

	fun setParameter(name: Int, value: Int) {
		glTexParameteri(GL_TEXTURE_2D, name, value)
	}

	fun uploadData(
		internalFormat: Int,
		width: Int,
		height: Int,
		format: Int,
		data: ByteBuffer?
	) {
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data)
	}

	companion object {
		fun loadTexture(name: String): Texture {

			println("load texture/$name.bmp")

			val image: ByteBuffer?
			val width: Int
			val height: Int
			val stack = MemoryStack.stackPush()


			val w = stack.mallocInt(1)
			val h = stack.mallocInt(1)
			val comp = stack.mallocInt(1)

			STBImage.stbi_set_flip_vertically_on_load(true)
			image = STBImage.stbi_load("texture/$name.png", w, h, comp, 4)
			if (image != null) {
				width = w.get()
				height = h.get()
				println("$width, $height")
			} else {
				throw java.lang.RuntimeException(
					"Failed to load a texture file!" +
							System.lineSeparator() +
							stbi_failure_reason()
				)
			}


			return createTexture(width, height, image)
		}

		private fun createTexture(width: Int, height: Int, data: ByteBuffer): Texture {
			val texture = Texture()
			texture.width = width
			texture.height = height

			texture.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
			texture.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
			texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST)
			texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST)

			glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE)

			texture.uploadData(GL_RGBA8, width, height, GL_RGBA, data)

			return texture
		}
	}
}
private fun glNormal(a: Triple<Float, Float, Float>) {
	glNormal3f(a.first, a.second, a.third)
}
private fun Triple<Float, Float, Float>.normalize(): Triple<Float, Float, Float> {
	val zero = Triple(0f, 0f, 0f)
	val dist = getDistance(zero, this)
	return Triple(this.first / dist, this.second / dist, this.third / third)
}
private fun cross(
	a: Triple<Float, Float, Float>,
	b: Triple<Float, Float, Float>,
	c: Triple<Float, Float, Float>
): Triple<Float, Float, Float> {
	val v1 = b - a
	val v2 = c - a
	return Triple(v1.second * v2.third - v2.second * v1.third,
	-v1.first * v2.third + v2.first * v1.third,
	v1.first * v2.second - v2.first * v1.second)
}
private fun cross(
	a: Triple<Float, Float, Float>,
	b: Triple<Float, Float, Float>,
	c: Triple<Float, Float, Float>,
	d: Triple<Float, Float, Float>
): Triple<Float, Float, Float> {
	val v1 = c - a
	val v2 = d - b
	return Triple(v1.second * v2.third - v2.second * v1.third,
		-v1.first * v2.third + v2.first * v1.third,
		v1.first * v2.second - v2.first * v1.second)
}
private operator fun Triple<Float, Float, Float>.minus(other: Triple<Float, Float, Float>): Triple<Float, Float, Float> {
	return Triple(first-other.first, second-other.second, third-other.third)
}
private fun getDistance(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>): Float {
	val dx = a.first - b.first
	val dy = a.second - b.second
	val dz = a.third - b.third
	return sqrt(dx*dx + dy*dy + dz*dz)
}
private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0
package example

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class Lab5{
    private var window: Long = 0
    private var keyCallback: GLFWKeyCallback? = null
    private var errorCallback: GLFWErrorCallback? = null
    private var colorMain = floatArrayOf(.0f, .0f, .0f)
    private var subjs = ArrayList<Figure>()
    private var clips = ArrayList<Figure>()
    private var intersection = Figure()
    private val result = ArrayList<Figure>()
    private var intersect = false
    private var width = WIDTH
    private var height = HEIGHT
    private var final = arrayOf(false, false)

    companion object {
        const val WIDTH = 1000
        const val HEIGHT = 1000

        val WHITE = Triple(1f, 1f, 1f)
        val RED = Triple(1f, 0f, 0f)
        val GREEN = Triple(0f, 1f, 0f)
        val BLUE = Triple(0f, 0f, 1f)
    }

    enum class Type {
        VERTEX,
        INTERSECTION
    }

    private fun init() {
        GLFW.glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err).also { errorCallback = it })
        check(GLFW.glfwInit().toInt() == GLFW.GLFW_TRUE) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "LAB5", MemoryUtil.NULL, MemoryUtil.NULL)
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
                        GLFW.GLFW_KEY_BACKSPACE -> {
                            clear()
                            subjs.clear()
                            clips.clear()
                            intersection.clear()
                            result.clear()
                            subjs.add(Figure())
                            clips.add(Figure())
                        }
                        GLFW.GLFW_KEY_ENTER -> {
                            if (subjs.last().vertex.isNotEmpty()) {
                                subjs.last().mode = GL_LINE_LOOP
                                subjs.add(Figure())
                                println(subjs.size)
                            }
                        }
                        GLFW.GLFW_KEY_TAB -> {
                            if (clips.last().vertex.isNotEmpty()) {
                                clips.last().mode = GL_LINE_LOOP
                                clips.add(Figure())
                            }
                        }
                        GLFW.GLFW_KEY_SPACE -> {
                            intersect = !intersect
                            cut()
                        }
                    }
                }
            }
        }.also { keyCallback = it })

        GLFW.glfwSetMouseButtonCallback(window) { window, button, action, _ ->
            if (action == GLFW.GLFW_PRESS){
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                GLFW.glfwGetCursorPos(window, x, y)
                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    if (subjs.isEmpty()) subjs.add(Figure())
                    subjs.last().add(Point(x[0].toFloat(), y[0].toFloat(), Type.VERTEX))
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                    if (clips.isEmpty()) clips.add(Figure())
                    clips.last().add(Point(x[0].toFloat(), y[0].toFloat(), Type.VERTEX))
                }
            }
        }


        val vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(window, (vidMode!!.width() - width) / 2, (vidMode.height() - height) / 2)
        GLFW.glfwMakeContextCurrent(window)
        GLFW.glfwSwapInterval(1)
        GLFW.glfwShowWindow(window)
    }

    // внутреннее отсечение В-А (вариант 22)
    private fun cut() {
        findIntersection()

//        weilerAtherton()
    }

    private fun findIntersection() {
        for (subj in subjs) {
            for (i in subj.vertex.indices) {
                val enterA: Point = subj.vertex[i]
                val exitA: Point = subj.vertex[if (i + 1 < subj.vertex.size) i + 1 else 0]
                for (clip in clips) {
                    for (j in clip.vertex.indices) {
                        val enterB: Point = clip.vertex[j]
                        val exitB: Point = clip.vertex[if (j + 1 < clip.vertex.size) j + 1 else 0]

                        val x = ((enterA.x * exitA.y - enterA.y * exitA.x) * (enterB.x - exitB.x) -
                                (enterB.x * exitB.y - enterB.y * exitB.x) * (enterA.x - exitA.x)) /
                                ((enterA.x - exitA.x) * (enterB.y - exitB.y) - (enterA.y - exitA.y) * (enterB.x - exitB.x))
                        val y = ((enterA.x * exitA.y - enterA.y * exitA.x) * (enterB.y - exitB.y)
                                - (enterB.x * exitB.y - enterB.y * exitB.x) * (enterA.y - exitA.y)) /
                                ((enterA.x - exitA.x) * (enterB.y - exitB.y) - (enterA.y - exitA.y) * (enterB.x - exitB.x))

                        if ((x >= enterA.x && x <= exitA.x || x >= exitA.x && x <= enterA.x) &&
                            (y >= enterA.y && y <= exitA.y || y >= exitA.y && y <= enterA.y) &&
                            (x >= enterB.x && x <= exitB.x || x >= exitB.x && x <= enterB.x) &&
                            (y >= enterB.y && y <= exitB.y || y >= exitB.y && y <= enterB.y)
                        ) {
                            intersection.add(Point(x, y, Type.INTERSECTION))
                            println("adding $x $y")
                        }
                    }
                }
            }
        }
    }

//    private fun weilerAtherton () {
//        var beginning = true
//        val res = ArrayList<Figure>()
//
//        val subjCopy: ArrayList<Point> = subjs.vertexInsert
//        val clipCopy: ArrayList<Point> = clips.vertexInsert
//
//
//        val enters = LinkedList<Point>()
//        for (point in subjCopy) {
//            if (point.isIntersection) {
//                if (beginning) enters.add(point)
//                beginning = !beginning
//            }
//        }
//
//        while (enters.size > 0) {
//            val resPart = Figure()
//            val begin = enters.pollFirst()
//            var current = begin
//            var isA = true
//            var currentList = subjCopy
//
//            do {
//                var i = currentList.indexOf(current)
//                do {
//                    if (i == -1) break
//
//                        resPart.add(currentList[i])
//                    i++
//                    if (i == currentList.size) i = 0
//                } while (!currentList[i].isIntersection)
//                if (i == -1) break
//                current = currentList[i]
//                enters.remove(current)
//                currentList = if (isA) clipCopy else subjCopy
//                isA = !isA
//            } while (current !== begin)
//            res.add(resPart)
//        }
//        result.addAll(res)
//    }

    private fun clear() {
        subjs.clear()
        clips.clear()
        final[0] = false
        final[1] = false
    }

    private fun drawSubj() {
        glPushMatrix()
        glColor3f(WHITE.first, WHITE.second, WHITE.third)
        for (subj in subjs) {
            glBegin(subj.mode)
            for (point in subj) {
                val x: Float = (point.x - (width / 2).toFloat()) / (width / 2).toFloat()
                val y: Float = -(point.y - (height / 2).toFloat()) / (height / 2).toFloat()
                glVertex2f(x, y)
            }
            glEnd()
        }
        glPopMatrix()
    }

    private fun drawClip() {
        glPushMatrix()
        glColor3f(RED.first, RED.second, RED.third)
        for (clip in clips) {
            glBegin(clip.mode)
            for (point in clip) {
                val x: Float = (point.x - (width / 2).toFloat()) / (width / 2).toFloat()
                val y: Float = -(point.y - (height / 2).toFloat()) / (height / 2).toFloat()
                glVertex2f(x, y)
            }
            glEnd()
        }
        glPopMatrix()
    }

    private fun drawIntersection () {
        glPushMatrix()
        glBegin(GL_LINE_LOOP)
        glColor3f(GREEN.first, GREEN.second, GREEN.third)
        for (figure in result){
            for (point in figure) {
                val x: Float = (point.x - (width / 2).toFloat()) / (width / 2).toFloat()
                val y: Float = -(point.y - (height / 2).toFloat()) / (height / 2).toFloat()
                glVertex2f(x, y)
            }
        }
        glEnd()
        glPopMatrix()
    }

    private fun drawDots() {
        for (p in intersection) {
            glPushMatrix()
            val x = (p.x - (width/2)) / (width / 2)
            val y = -(p.y - (height/2)) / (height / 2)
            glBegin(GL_QUADS)
            glColor3f(BLUE.first, BLUE.second, BLUE.third)
            glVertex2f(x+.005f, y+.005f)
            glVertex2f(x+.005f, y-.005f)
            glVertex2f(x-.005f, y-.005f)
            glVertex2f(x-.005f, y+.005f)
            glEnd()
            glPopMatrix()
        }
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

            drawSubj()
            drawClip()
            drawDots()
            if (intersect) {
                drawIntersection()
            }

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
}

fun main(){
    Lab5().run()
}

private class Figure: Iterable<Point> {
    var mode = GL_LINE_STRIP
    var vertex: ArrayList<Point> = ArrayList()
    var vertexInsert: ArrayList<Point>
    var isolation: Boolean

    fun add(a: Point) {
        if (!isolation) {
            vertex.add(a)
        }
    }

    fun clear() {
        isolation = false
        vertex.clear()
    }

    fun insertVertex(intersection: ArrayList<Point>) {
        vertexInsert = vertex.clone() as ArrayList<Point>
        for (point in intersection) {
            for (j in vertexInsert.indices) {
                val k = if (j + 1 == vertexInsert.size) 0 else j + 1
                if (checkInsert(vertexInsert[j], vertexInsert[k], point)) {
                    vertexInsert.add(if (k == 0) vertexInsert.size else k, point)
                    break
                }
            }
        }
    }

    init {
        vertexInsert = ArrayList()
        isolation = false
    }

    override fun iterator(): Iterator<Point> {
        return FigureIterator()
    }

    private inner class FigureIterator: Iterator<Point> {
        private var pos = 0

        override fun hasNext(): Boolean = pos < vertex.size

        override fun next(): Point = vertex[pos++]
    }
}

private class Point(var x: Float, var y: Float, t: Lab5.Type) {
    var vertex: Boolean = t == Lab5.Type.VERTEX
    var isIntersection: Boolean = t == Lab5.Type.INTERSECTION

    override fun toString(): String {
        return "($x,$y)"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Point) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + vertex.hashCode()
        result = 31 * result + isIntersection.hashCode()
        return result
    }

}



private fun checkInsert(start: Point, end: Point, middle: Point): Boolean {
    return abs((-start.x * end.y + end.x * start.y) / ((start.y - end.y) * middle.x + (end.x - start.x) * middle.y) - 1) <= 0.001 &&
            middle.x < start.x.coerceAtLeast(end.x) &&
            middle.x > start.x.coerceAtMost(end.x) &&
            middle.y < start.y.coerceAtLeast(end.y) &&
            middle.y > start.y.coerceAtMost(end.y)
}

private fun Boolean.toInt() = if (this) 1 else 0
private fun Int.toBoolean() = this != 0




















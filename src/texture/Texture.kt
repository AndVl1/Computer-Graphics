package example.texture

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer


class Texture {
    var id = glGenTextures()

    /**
     * Width of the texture.
     */
    private var width = 0

    /**
     * Height of the texture.
     */
    private var height = 0

    /** Creates a texture.  */

    /**
     * Binds the texture.
     */
    fun bind() {
        glBindTexture(GL_TEXTURE_2D, id)
    }

    fun setParameter(name: Int, value: Int) {
        glTexParameteri(GL_TEXTURE_2D, name, value)
    }

    fun uploadData(width: Int, height: Int, data: ByteBuffer?) {
        uploadData(GL_RGBA8, width, height, GL_RGBA, data)
    }


    fun uploadData(internalFormat: Int, width: Int, height: Int, format: Int, data: ByteBuffer?) {
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data)
    }

    fun delete() {
        glDeleteTextures(id)
    }

    fun getWidth(): Int {
        return width
    }

    fun setWidth(width: Int) {
        if (width > 0) {
            this.width = width
        }
    }

    fun getHeight(): Int {
        return height
    }

    fun setHeight(height: Int) {
        if (height > 0) {
            this.height = height
        }
    }

    companion object {
        public fun createTexture(width: Int, height: Int, data: ByteBuffer): Texture {
            val texture = Texture()
            texture.setWidth(width)
            texture.setHeight(height)

            texture.bind()

            texture.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER)
            texture.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER)
            texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            texture.uploadData(GL_RGBA8, width, height, GL_RGBA, data)

            return texture
        }

        fun loadTexture(path: String): Texture {
            var image: ByteBuffer?
            var width: Int
            var height: Int
            MemoryStack.stackPush().use { stack ->
                /* Prepare image buffers */
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val comp = stack.mallocInt(1)

                /* Load image */stbi_set_flip_vertically_on_load(true)
                image = stbi_load(path, w, h, comp, 4)
                if (image == null) {
                    throw RuntimeException(
                        "Failed to load a texture file!"
                                + System.lineSeparator() + stbi_failure_reason()
                    )
                }

                /* Get width and height of image */
                width = w.get()
                height = h.get()

                return createTexture(width, height, image!!)
            }
        }
    }
}
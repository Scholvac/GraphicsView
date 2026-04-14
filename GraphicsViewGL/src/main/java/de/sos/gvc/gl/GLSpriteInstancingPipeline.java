package de.sos.gvc.gl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;

final class GLSpriteInstancingPipeline {

	private static final int ATTR_CORNER = 0;
	private static final int ATTR_ORIGIN = 1;
	private static final int ATTR_AXIS_X = 2;
	private static final int ATTR_AXIS_Y = 3;
	private static final int ATTR_UV_RECT = 4;
	private static final int FLOAT_BYTES = Float.BYTES;
	private static final int INSTANCE_FLOATS = 10;
	private static final int INSTANCE_STRIDE = INSTANCE_FLOATS * FLOAT_BYTES;

	private static final float[] QUAD_CORNERS = {
			0f, 0f,
			1f, 0f,
			1f, 1f,
			0f, 0f,
			1f, 1f,
			0f, 1f
	};

	private static final String VERTEX_SHADER =
			"#version 330 core\n" +
					"layout(location = 0) in vec2 aCorner;\n" +
					"layout(location = 1) in vec2 iOrigin;\n" +
					"layout(location = 2) in vec2 iAxisX;\n" +
					"layout(location = 3) in vec2 iAxisY;\n" +
					"layout(location = 4) in vec4 iUvRect;\n" +
					"uniform vec2 uViewport;\n" +
					"out vec2 vTexCoord;\n" +
					"void main() {\n" +
					"  vec2 pos = iOrigin + aCorner.x * iAxisX + aCorner.y * iAxisY;\n" +
					"  vec2 ndc = vec2((pos.x / uViewport.x) * 2.0 - 1.0, 1.0 - (pos.y / uViewport.y) * 2.0);\n" +
					"  gl_Position = vec4(ndc, 0.0, 1.0);\n" +
					"  vTexCoord = mix(iUvRect.xy, vec2(iUvRect.z, iUvRect.w), aCorner);\n" +
					"}\n";

	private static final String FRAGMENT_SHADER =
			"#version 330 core\n" +
					"in vec2 vTexCoord;\n" +
					"uniform sampler2D uTexture;\n" +
					"out vec4 fragColor;\n" +
					"void main() {\n" +
					"  fragColor = texture(uTexture, vTexCoord);\n" +
					"}\n";

	private boolean mInitialized;
	private int mProgram;
	private int mVertexShader;
	private int mFragmentShader;
	private int mVertexArrayObject;
	private int mCornerBufferObject;
	private int mInstanceBufferObject;
	private int mViewportUniform = -1;
	private int mTextureUniform = -1;

	void render(final GL3 gl, final Texture texture, final FloatBuffer instanceData,
			final int instanceCount, final int viewportWidth, final int viewportHeight) {
		if (instanceCount <= 0)
			return;
		ensureInitialized(gl);

		gl.glUseProgram(mProgram);
		gl.glUniform2f(mViewportUniform, viewportWidth, viewportHeight);
		gl.glUniform1i(mTextureUniform, 0);
		texture.bind(gl);

		gl.glBindVertexArray(mVertexArrayObject);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mInstanceBufferObject);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, instanceData.remaining() * FLOAT_BYTES, instanceData, GL.GL_DYNAMIC_DRAW);
		gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, 6, instanceCount);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindVertexArray(0);
		gl.glUseProgram(0);
	}

	void dispose(final GL3 gl) {
		if (!mInitialized)
			return;
		if (mProgram != 0)
			gl.glUseProgram(0);
		if (mVertexArrayObject != 0) {
			final IntBuffer tmp = GLBuffers.newDirectIntBuffer(1);
			tmp.put(0, mVertexArrayObject);
			gl.glDeleteVertexArrays(1, tmp);
		}
		if (mCornerBufferObject != 0 || mInstanceBufferObject != 0) {
			final IntBuffer tmp = GLBuffers.newDirectIntBuffer(2);
			tmp.put(0, mCornerBufferObject);
			tmp.put(1, mInstanceBufferObject);
			gl.glDeleteBuffers(2, tmp);
		}
		if (mProgram != 0)
			gl.glDeleteProgram(mProgram);
		if (mVertexShader != 0)
			gl.glDeleteShader(mVertexShader);
		if (mFragmentShader != 0)
			gl.glDeleteShader(mFragmentShader);

		mInitialized = false;
		mProgram = 0;
		mVertexShader = 0;
		mFragmentShader = 0;
		mVertexArrayObject = 0;
		mCornerBufferObject = 0;
		mInstanceBufferObject = 0;
		mViewportUniform = -1;
		mTextureUniform = -1;
	}

	private void ensureInitialized(final GL3 gl) {
		if (mInitialized)
			return;

		mVertexShader = compileShader(gl, GL3.GL_VERTEX_SHADER, VERTEX_SHADER);
		mFragmentShader = compileShader(gl, GL3.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
		mProgram = gl.glCreateProgram();
		gl.glAttachShader(mProgram, mVertexShader);
		gl.glAttachShader(mProgram, mFragmentShader);
		gl.glLinkProgram(mProgram);
		verifyProgram(gl, mProgram);

		mViewportUniform = gl.glGetUniformLocation(mProgram, "uViewport");
		mTextureUniform = gl.glGetUniformLocation(mProgram, "uTexture");

		final IntBuffer vertexArrayIds = GLBuffers.newDirectIntBuffer(1);
		gl.glGenVertexArrays(1, vertexArrayIds);
		mVertexArrayObject = vertexArrayIds.get(0);

		final IntBuffer bufferIds = GLBuffers.newDirectIntBuffer(2);
		gl.glGenBuffers(2, bufferIds);
		mCornerBufferObject = bufferIds.get(0);
		mInstanceBufferObject = bufferIds.get(1);

		gl.glBindVertexArray(mVertexArrayObject);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mCornerBufferObject);
		final FloatBuffer quadBuffer = GLBuffers.newDirectFloatBuffer(QUAD_CORNERS);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, QUAD_CORNERS.length * FLOAT_BYTES, quadBuffer, GL.GL_STATIC_DRAW);
		gl.glEnableVertexAttribArray(ATTR_CORNER);
		gl.glVertexAttribPointer(ATTR_CORNER, 2, GL.GL_FLOAT, false, 2 * FLOAT_BYTES, 0L);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mInstanceBufferObject);
		gl.glEnableVertexAttribArray(ATTR_ORIGIN);
		gl.glVertexAttribPointer(ATTR_ORIGIN, 2, GL.GL_FLOAT, false, INSTANCE_STRIDE, 0L);
		gl.glVertexAttribDivisor(ATTR_ORIGIN, 1);
		gl.glEnableVertexAttribArray(ATTR_AXIS_X);
		gl.glVertexAttribPointer(ATTR_AXIS_X, 2, GL.GL_FLOAT, false, INSTANCE_STRIDE, 2L * FLOAT_BYTES);
		gl.glVertexAttribDivisor(ATTR_AXIS_X, 1);
		gl.glEnableVertexAttribArray(ATTR_AXIS_Y);
		gl.glVertexAttribPointer(ATTR_AXIS_Y, 2, GL.GL_FLOAT, false, INSTANCE_STRIDE, 4L * FLOAT_BYTES);
		gl.glVertexAttribDivisor(ATTR_AXIS_Y, 1);
		gl.glEnableVertexAttribArray(ATTR_UV_RECT);
		gl.glVertexAttribPointer(ATTR_UV_RECT, 4, GL.GL_FLOAT, false, INSTANCE_STRIDE, 6L * FLOAT_BYTES);
		gl.glVertexAttribDivisor(ATTR_UV_RECT, 1);

		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		gl.glBindVertexArray(0);
		mInitialized = true;
	}

	private int compileShader(final GL3 gl, final int shaderType, final String source) {
		final int shader = gl.glCreateShader(shaderType);
		final String[] sources = { source };
		final int[] lengths = { source.length() };
		gl.glShaderSource(shader, 1, sources, lengths, 0);
		gl.glCompileShader(shader);
		verifyShader(gl, shader);
		return shader;
	}

	private void verifyShader(final GL3 gl, final int shader) {
		final IntBuffer status = GLBuffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, status);
		if (status.get(0) == GL.GL_TRUE)
			return;
		throw new IllegalStateException("GL sprite shader compilation failed: " + getShaderLog(gl, shader));
	}

	private void verifyProgram(final GL3 gl, final int program) {
		final IntBuffer status = GLBuffers.newDirectIntBuffer(1);
		gl.glGetProgramiv(program, GL3.GL_LINK_STATUS, status);
		if (status.get(0) == GL.GL_TRUE)
			return;
		throw new IllegalStateException("GL sprite program link failed: " + getProgramLog(gl, program));
	}

	private String getShaderLog(final GL3 gl, final int shader) {
		final IntBuffer length = GLBuffers.newDirectIntBuffer(1);
		gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, length);
		final int size = Math.max(length.get(0), 1);
		final byte[] log = new byte[size];
		gl.glGetShaderInfoLog(shader, size, null, 0, log, 0);
		return new String(log).trim();
	}

	private String getProgramLog(final GL3 gl, final int program) {
		final IntBuffer length = GLBuffers.newDirectIntBuffer(1);
		gl.glGetProgramiv(program, GL3.GL_INFO_LOG_LENGTH, length);
		final int size = Math.max(length.get(0), 1);
		final byte[] log = new byte[size];
		gl.glGetProgramInfoLog(program, size, null, 0, log, 0);
		return new String(log).trim();
	}
}

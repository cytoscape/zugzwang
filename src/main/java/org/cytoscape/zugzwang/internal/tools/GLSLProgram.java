package org.cytoscape.zugzwang.internal.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Scanner;

import com.jogamp.opengl.GL4;

/**
 * Helper class for compiling GLSL programs.
 */
public class GLSLProgram
{
	/**
	 * Takes paths to text files with the individual shaders' code, 
	 * loads and compiles them, and link everything into a GLSL program.
	 * 
	 * @param context Current GL context
	 * @param pathVS Path to the vertex shader code, null if none is needed
	 * @param pathTSControl Path to the tesselation control shader code, null if none is needed
	 * @param pathTSEval Path to the tesselation evaluation shader code, null if none is needed
	 * @param pathGS Path to the geometry shader code, null if none is needed
	 * @param pathFS Path to the fragment shader code, null if none is needed
	 * @return GLSL program handle
	 */
	public static int CompileProgram(GL4 context, URL pathVS, URL pathTSControl, URL pathTSEval, URL pathGS, URL pathFS)
	{
		try 
		{
			int vertexShader = 0, tesselationControlShader = 0, tesselationEvalShader = 0, geometryShader = 0, fragmentShader = 0;
			int shaderProgram = context.glCreateProgram();
		
			// Load and compile individual shaders:
			
			if (pathVS != null)
			{
				vertexShader = context.glCreateShader(GL4.GL_VERTEX_SHADER);
				context.glShaderSource(vertexShader, 1, new String[] { GetText(pathVS) }, null);
				context.glCompileShader(vertexShader);
				
				context.glAttachShader(shaderProgram, vertexShader);
			}			
			
			if (pathTSControl != null && pathTSEval != null)
			{
				tesselationControlShader = context.glCreateShader(GL4.GL_TESS_CONTROL_SHADER);
				context.glShaderSource(tesselationControlShader, 1, new String[] { GetText(pathTSControl) }, null);
				context.glCompileShader(tesselationControlShader);

				tesselationEvalShader = context.glCreateShader(GL4.GL_TESS_EVALUATION_SHADER);
				context.glShaderSource(tesselationEvalShader, 1, new String[] { GetText(pathTSEval) }, null);
				context.glCompileShader(tesselationEvalShader);
				
				context.glAttachShader(shaderProgram, tesselationControlShader);
				context.glAttachShader(shaderProgram, tesselationEvalShader);
			}
			
			if (pathGS != null)
			{
				geometryShader = context.glCreateShader(GL4.GL_GEOMETRY_SHADER);
				context.glShaderSource(geometryShader, 1, new String[] { GetText(pathGS) }, null);
				context.glCompileShader(geometryShader);
				
				context.glAttachShader(shaderProgram, geometryShader);
			}
			
			if (pathFS != null)
			{
				fragmentShader = context.glCreateShader(GL4.GL_FRAGMENT_SHADER);
				context.glShaderSource(fragmentShader, 1, new String[] { GetText(pathFS) }, null);
				context.glCompileShader(fragmentShader);
				
				context.glAttachShader(shaderProgram, fragmentShader);
			}
			
			// Link overall program:
			
			context.glLinkProgram(shaderProgram);
			context.glValidateProgram(shaderProgram);
			
			// Delete shaders:
			
			if (pathVS != null)
				context.glDeleteShader(vertexShader);
			if (pathTSControl != null && pathTSEval != null)
			{
				context.glDeleteShader(tesselationControlShader);
				context.glDeleteShader(tesselationEvalShader);
			}
			if (pathGS != null)
				context.glDeleteShader(geometryShader);
			if (pathFS != null)
				context.glDeleteShader(fragmentShader);
			
			// Find out what went wrong:
			
			IntBuffer intBuffer = IntBuffer.allocate(1);
			context.glGetProgramiv(shaderProgram, GL4.GL_LINK_STATUS, intBuffer);
	        
	        if (intBuffer.get(0) != 1)
	        {
	            context.glGetProgramiv(shaderProgram, GL4.GL_INFO_LOG_LENGTH, intBuffer);
	            int size = intBuffer.get(0);
	            System.err.println("Shader program link error: ");
	            if (size > 0)
	            {
	                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
	                context.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
	                for (byte b : byteBuffer.array())
	                    System.err.print((char) b);
	            }
	            else
	                System.out.println("Unknown error during shader program compilation.");
	            
	            return -1;
	        }
	        
	        return shaderProgram;
		} 
		catch (IOException e) 
		{
			return -1;
		}
	}
	
	/**
	 * Helper method for loading text from a resource URL
	 * 
	 * @param path URL path
	 * @return Loaded text
	 * @throws IOException In case no resource was found
	 */
	private static String GetText(URL path) throws IOException
	{
		InputStream programTextStream = path.openStream();
    	Scanner programTextScanner = new Scanner(programTextStream, "UTF-8");
    	String programText = programTextScanner.useDelimiter("\\Z").next();
    	programTextScanner.close();
        programTextStream.close();
        
        return programText;
	}
}
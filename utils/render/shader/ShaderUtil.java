package nuclear.utils.render.shader;

import nuclear.utils.IMinecraft;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;

import java.io.*;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtil implements IMinecraft {
    private final int programID;

    public static ShaderUtil spike_sandbox = new ShaderUtil("spike_sandbox");
    public static ShaderUtil blockoverlay = new ShaderUtil("blockoverlay");
    public static ShaderUtil outline_esp = new ShaderUtil("outline_esp");
    public static ShaderUtil glow_esp = new ShaderUtil("glow_esp");
    public static ShaderUtil outline_glow_esp = new ShaderUtil("outline_glow_esp");
    public static ShaderUtil light_kawase_down = new ShaderUtil("light_kawase_down");
    public static ShaderUtil light_kawase_up = new ShaderUtil("light_kawase_up");
    public static ShaderUtil blurred_round_rectangle = new ShaderUtil("blurred_round_rectangle");
    public static ShaderUtil rounded_head_texture = new ShaderUtil("rounded_head_texture");
    public static ShaderUtil substring = new ShaderUtil("substring");
    public static ShaderUtil rounded_texture = new ShaderUtil("rounded_texture");
    public static ShaderUtil hands = new ShaderUtil("hands");
    public static ShaderUtil saturation = new ShaderUtil("saturation");
    public static ShaderUtil sun_rays = new ShaderUtil("sun_rays");


    public ShaderUtil(String fragmentShaderLoc) {
        this(fragmentShaderLoc, "vertex");
    }

    public ShaderUtil(String fragmentShaderLoc, String vertexShaderLoc) {
        programID = ARBShaderObjects.glCreateProgramObjectARB();

        try {
            int fragmentShaderID = createShaderFromFile(fragmentShaderLoc, GL_FRAGMENT_SHADER);
            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderID);

            int vertexShaderID = createShaderFromFile(vertexShaderLoc, GL_VERTEX_SHADER);
            ARBShaderObjects.glAttachObjectARB(programID, vertexShaderID);

            ARBShaderObjects.glLinkProgramARB(programID);

            if (ARBShaderObjects.glGetObjectParameteriARB(programID, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == 0) {
                throw new IllegalStateException("Shader program failed to link: "
                        + ARBShaderObjects.glGetInfoLogARB(programID, 4096));
            }

        } catch (IOException exception) {
            exception.printStackTrace();
            System.out.println("Ошибка при загрузке шейдера: " + fragmentShaderLoc);
        }
    }

    public void attach() {
        ARBShaderObjects.glUseProgramObjectARB(programID);
    }

    public void detach() {
        glUseProgram(0);
    }


    public void setUniform(String name, float... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programID, name);
        switch (args.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(loc, args[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
            default ->
                    throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
        }
    }

    public void setUniform(String name, int... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programID, name);
        switch (args.length) {
            case 1 -> glUniform1iARB(loc, args[0]);
            case 2 -> glUniform2iARB(loc, args[0], args[1]);
            case 3 -> glUniform3iARB(loc, args[0], args[1], args[2]);
            case 4 -> glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
            default ->
                    throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
        }
    }

    public void setUniformf(String name, float... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(this.programID, name);
        switch (args.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(loc, args[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
        }
    }

    public void setUniformf(String var1, double... args) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(this.programID, var1);
        switch (args.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(loc, (float) args[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(loc, (float) args[0], (float) args[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(loc, (float) args[0], (float) args[1], (float) args[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(loc, (float) args[0], (float) args[1], (float) args[2],
                    (float) args[3]);
        }
    }

    private int createShaderFromFile(String shaderName, int shaderType) throws IOException {
        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        String shaderSource = loadShaderFromFile(shaderName);
        ARBShaderObjects.glShaderSourceARB(shader, shaderSource);
        ARBShaderObjects.glCompileShaderARB(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String errorLog = GL20.glGetShaderInfoLog(shader, 4096);
            throw new IllegalStateException("Shader (" + shaderName + ") failed to compile: " + errorLog);
        }

        return shader;
    }


    private String loadShaderFromFile(String shaderName) throws IOException {
        String path = "assets/minecraft/nuclear/shaders/" + shaderName + ".glsl";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Shader file not found: " + path);
            }
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    public int getUniform(String name) {
        return glGetUniformLocation(programID, name);
    }
}

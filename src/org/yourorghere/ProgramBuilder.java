package org.yourorghere;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.media.opengl.GL;

public class ProgramBuilder {

    public static int createProgram(GL gl, String path) {
        int[] status = new int[1];
        int program;
        int shader = gl.glCreateShader(GL.GL_FRAGMENT_SHADER);
        gl.glShaderSource(shader, 1, new String[]{loadSource(path)}, null);
        gl.glCompileShader(shader);

        gl.glGetShaderiv(shader, GL.GL_COMPILE_STATUS, status, 0);
        check(status[0], path + "\tshader");

        program = gl.glCreateProgram();
        gl.glAttachShader(program, shader);
        gl.glDeleteShader(shader);
        gl.glLinkProgram(program);
        gl.glGetProgramiv(program, GL.GL_LINK_STATUS, status, 0);
        check(status[0], path + "\tprogram");
        return program;
    }

    private static void check(int status, String name) {
        //статус = 0 => не скомпилировалось
        if (status == 0) {
            System.err.println(name + "\terror!");
            System.exit(status);
        } else {
            System.out.println(name + "\tcompile");
        }
    }

    public static String loadSource(String path) {
        try {
            BufferedReader brf = new BufferedReader(new FileReader(path));
            String source = "";
            String line;
            while ((line = brf.readLine()) != null) {
                source += line + "\n";
            }
            return source;
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            return "";
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return "";
        }
    }

}

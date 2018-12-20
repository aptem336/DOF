package org.yourorghere;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.media.opengl.GL;

//класс дл€ создани€ программы с шейдером
public class ProgramBuilder {

    //передаЄм gl и путь к файлу шейдера
    public static int createProgram(GL gl, String path) {
        //массив, в который будет записыватьс€ статус компил€ции шейдера и линковки программы
        int[] status = new int[1];
        
        //создаЄм шейдер фрагментный, то есть работающий с цветом кадра
        int shader = gl.glCreateShader(GL.GL_FRAGMENT_SHADER);
        //передаЄм шейдеру его исходный из файла по пути
        //функци€ loadSource принимает путь и возвращает строку с кодом
        gl.glShaderSource(shader, 1, new String[]{loadSource(path)}, null);
        //компилируем шейдер
        gl.glCompileShader(shader);
        //в перемнную статус передаЄм данный о статусе компил€ции шейдера
        gl.glGetShaderiv(shader, GL.GL_COMPILE_STATUS, status, 0);
        //вызываем процедуру, котора€ выведет соответсвующие сообщени€ и завершить работу в случае ошибки, передаЄм ей полученный статус 
        check(status[0], path + "\tshader");
        //создаЄм переменную под программу, дада хранитьс€ в целочисленной переменной и записываем в неЄ программу
        int program = gl.glCreateProgram();
        //присоедин€ем к программе шейдер
        gl.glAttachShader(program, shader);
        //удал€ем шейдер, теперь он есть в программе и больше не нужен
        gl.glDeleteShader(shader);
        //линкуем программу (лучше погуглить, что это значит)
        gl.glLinkProgram(program);
        //получаем статус линковки программы в переменную статуса
        gl.glGetProgramiv(program, GL.GL_LINK_STATUS, status, 0);
        //передаЄм статус обрабатывающей его процедуре
        check(status[0], path + "\tprogram");
        return program;
    }

    //процедура обрабатывающа€ статус
    private static void check(int status, String name) {
        //статус = 0 => не скомпилировалось
        if (status == 0) {
            System.err.println(name + "\terror!");
            //завершение работы
            System.exit(status);
        } else {
            System.out.println(name + "\tcompile");
        }
    }

    //процдеруа загрузки кода из файла
    public static String loadSource(String path) {
        try {
            //класс дл€ чтени€ файла
            BufferedReader brf = new BufferedReader(new FileReader(path));
            //пока что исходный код пустой
            String source = "";
            //переменна€ под линию - строку из файла
            String line;
            //пока мы не встретим конец файла т.е. пустую линию
            //читаем из файла строки
            while ((line = brf.readLine()) != null) {
                //добавл€ем к исходному коду считанное
                source += line + "\n";
            }
            //возвращаем считанное
            return source;
            //ошибка - файл не найден
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
            return "";
            //ошибка ввода-вывода
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return "";
        }
    }

}

package org.yourorghere;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.glu.GLU;

public class DOF implements GLEventListener {

    private static GL gl;
    private static GLU glu;
    private static GLUT glut;

    public static void main(String[] args) {
        //создаём окно
        Frame frame = new Frame("DOF!");
        //получаем размеры экрана
        int size = Toolkit.getDefaultToolkit().getScreenSize().height - 30;
        frame.setSize(size, size);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        //создаём канву - место рисования
        GLCanvas canvas = new GLCanvas();
        //создаём и добавляем слушателей
        Listener listener = new Listener();
        canvas.addKeyListener(listener);
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
        canvas.addMouseWheelListener(listener);
        canvas.addGLEventListener(new DOF());
        canvas.setBounds(0, 0, frame.getWidth(), frame.getHeight() - 30);
        //создаём управляющий методами отрисовки аниматор
        Animator animator = new Animator(canvas);

        frame.add(canvas);
        //добавляем слушателя окна
        frame.addWindowListener(new WindowAdapter() {
            @Override
            //переопределяем операцию закрытия окна
            public void windowClosing(WindowEvent e) {
                new Thread(() -> {
                    //останавливаем аниматор
                    animator.stop();
                    //выходим со статусом 0 (без ошибок)
                    System.exit(0);
                }).start();
            }
        });
        //запускаем аниматор
        animator.start();
    }

    //переменная для хранения размеров вывода
    private static final int[] VIEWPORT = new int[4];
    //пять текстур
    //0 - для начальной отрисовки сцены
    //1-2 - для размытия
    //3 - для карты глубины
    //4 - текстура марамора для наложения на стол
    private static final int[] TEXTURES = new int[5];
    //четыре буфера кадра
    //0 - для отрисовки сцены
    //1-2 - для размытия
    //3 - для карты глубины
    private static final int[] FBO = new int[3];

    //процедура инициализации текстур
    private static void initTextures() {
        //генерируем 4 текстуры в массив
        gl.glGenTextures(4, TEXTURES, 0);
        //5ю полчаем из файла
        TEXTURES[4] = loadTexture("table_texture.jpg").getTextureObject();
        //для всех текстур
        for (int i = 0; i < 5; i++) {
            //биндим "выбираем" текущую
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[i]);
            //устанавливаем параметры 
            //фильрации
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            //наложения
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        }
        //биндим текстуру по дефолту - сброс
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }

    //процедура генерация буферов кадра
    private static void initFrameBuffers() {
        //генерируем 3 буфера в массив
        gl.glGenFramebuffersEXT(3, FBO, 0);
        {
            //для трёх первых
            for (int i = 0; i < 3; i++) {
                //биндим текущий по индексу
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[i]);
                //биндим текстуру
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[i]);
                //устанавливаем параметры, будет принимать цвет RGBA, с таким-то размером
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, VIEWPORT[2], VIEWPORT[3], 0, GL.GL_RGBA, GL.GL_FLOAT, null);
                //прикрепляем к фреймбуферу текстуру
                gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, TEXTURES[i], 0);
            }

            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[0]);
            //для нулевого - для записи глубины
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[3]);
            //параметры записи меняются на запись глубины с теми же размерами
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH_COMPONENT, VIEWPORT[2], VIEWPORT[3], 0, GL.GL_DEPTH_COMPONENT, GL.GL_FLOAT, null);
            //прикрепляем к фреймбуферу текстуру
            gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_TEXTURE_2D, TEXTURES[3], 0);
            
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    //главная и реализующая размытие программы
    private static int mainProgram, blurProgram;
    //тип выводимого на экран изображения
    //1 - с применением эффекта
    //2 - полностью размытая
    //3 - текстура глубины
    public static int type = 1;
    //переменные фокуса, влияния глубины, силы размытия 
    public static float focus = 0.5f, range = 6.0f, force = 0.002f;

    //инициализация программ
    private static void initPrograms() {
        //создаём программу с шейдером код которого берём из файла ...
        //программа размытия
        blurProgram = ProgramBuilder.createProgram(gl, "blur.frag");
        //биндим эту программу
        gl.glUseProgram(blurProgram);
        //устанавливаем положение переменной текстуры (еще не размытой)
        gl.glUniform1i(gl.glGetUniformLocation(blurProgram, "text"), 0);
        //передаём ей параметры
        //размер экрана
        gl.glUniform2i(gl.glGetUniformLocation(blurProgram, "viewport"), VIEWPORT[2], VIEWPORT[3]);
        //количество точек для размытия (размытие нормальное => значения с нормальным распределением)
        gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "normalSize"), NORMAL.length);
        //передаём программе сам массив значений
        gl.glUniform1fv(gl.glGetUniformLocation(blurProgram, "normal"), NORMAL.length, NORMAL, 0);

        //создаём программу с шейдером, код которого будет взят из файла ...
        mainProgram = ProgramBuilder.createProgram(gl, "main.frag");
        //биндим эту программу
        gl.glUseProgram(mainProgram);
        //передаём размеры экрана
        gl.glUniform2i(gl.glGetUniformLocation(mainProgram, "viewport"), VIEWPORT[2], VIEWPORT[3]);
        //привязываем положения необходимым текстурам
        //четкая
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "high"), 0);
        //размытая
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "blur"), 1);
        //глубины
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "depth"), 2);
        //биндим программу по дефолту
        gl.glUseProgram(0);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL();
        glu = new GLU();
        glut = new GLUT();
        //записываем в переменную VIEWPORT размеры экрана
        gl.glGetIntegerv(GL.GL_VIEWPORT, VIEWPORT, 0);
        //генерируем текстуры
        initTextures();
        //генерируем буферы
        initFrameBuffers();
        //инициализируем шейдерные программы
        initPrograms();
        //включаем поддержку света
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        //включаем управление свойствами материала
        gl.glEnable(GL.GL_COLOR_MATERIAL);
        //очищаем буфер цветом
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
        //включаем сглаживание теней
        gl.glShadeModel(GL.GL_SMOOTH);
    }

    //процедура рагрузки текстуры из файла
    public static Texture loadTexture(String fileName) {
        Texture text = null;
        try {
            //читаем из фала
            text = TextureIO.newTexture(new File(fileName), false);
        } catch (IOException | GLException e) {
            //в случае ошибки выводим сообщение об ошибке
            System.out.println(e.getMessage());
        }
        //возвращаем текстуру
        return text;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final float h = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, h, 1.0d, 2000.0d);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    //расстояние камеры от точки сцены
    public static double len = 300d;
    //вертикальный и горизонтальный углы в шаре, на котором "расположена" камера
    public static double angleV = 70d, angleH = -45d;
    //место положения камеры
    private static double xView, zView, yView;

    private static void calcCam() {
        //вычисляем компоненты через вертикальный и горизонтальный углы
        //параметрические уравнения шара
        xView = len * Math.sin(Math.toRadians(angleV)) * Math.cos(Math.toRadians(angleH));
        zView = len * Math.sin(Math.toRadians(angleV)) * Math.sin(Math.toRadians(angleH));
        yView = len * Math.cos(Math.toRadians(angleV));
    }

    //переменные для подсчёта фпс
    //метки времени
    private static long time;
    private static long lc_time;
    //счётчик кадров
    private static int frames = Integer.MAX_VALUE;
    private static int fps;

    @Override
    public void display(GLAutoDrawable drawable) {
        //блок для подсчёта fps
        //текущее время
        time = System.currentTimeMillis();
        //если прошло больше секунды
        if (time - lc_time >= 1000) {
            //значение fps равно насчитанным кадрам
            fps = frames;
            //сохраняем время для вычисления разницы
            lc_time = time;
            //обнуляем количество фреймов
            frames = 0;
        }
        //увеличиваем количество кадров
        frames++;
        //вызываем метод вычисления коомпонент положения для камеры
        calcCam();
        //устанавливаем позицию света
        gl.glLightiv(GL.GL_LIGHT0, GL.GL_POSITION, new int[]{1, 1, 1, 0}, 0);
        //очищаем матрицу
        gl.glLoadIdentity();
        //перемщаемся в 0,0,0
        gl.glTranslated(0d, 0d, 0d);
        //перемещаем камеру
        //три первые переменные - откуда смотрим
        //три вторые - куда смотрим
        //три первые - направление
        glu.gluLookAt(xView, yView, zView, 0d, 0d, 0d, 0d, 0.5d, 0d);
        {
            //биндим 0вой фреймбуфер - для записи сцены
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[0]);
            //очищаем
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            //рисуем сцену в выбранный буферу
            drawScene();
        }
        //начинаем "плоскую" отрисоку, т.к. дальше работаем только с 2D текстура
        startOrtho();
        {
            //биндим размывающую программу
            gl.glUseProgram(blurProgram);
            {
                //биндим фреймбуфер 2й (теперь отрисованной будет оппадать во вторую текстуру т.т мы ранее их связали)
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[2]);
                //оичщаем
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                //активируем текстуру (нулевую)
                gl.glActiveTexture(GL.GL_TEXTURE0);
                //биндим нулевую текстуру (с записанной сценоЙ ранее сценой) на нулевое место
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[0]);
                {
                    //передаём программе силу размытия
                    gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "force"), force);
                    //направление размытия
                    gl.glUniform2f(gl.glGetUniformLocation(blurProgram, "dir"), 1.0f, 0.0f);
                    //рисуем полноэкранный прямоугольник
                    drawRect();
                }
                //биндим фреймбфер
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[1]);
                //очищаем
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                //активируем текстуру (нулевую)
                gl.glActiveTexture(GL.GL_TEXTURE0);
                //биндим 2ю текстуру на место 0 (активированное)
                //вторую, потому что только что в неё мы записали размытую в одном направлении сцену
                //номер текстуры соответсвует номеру использованного фреймбуфера
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[2]);
                {
                    //передаём программе силу размытия
                    gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "force"), force);
                    //направление размытия
                    gl.glUniform2f(gl.glGetUniformLocation(blurProgram, "dir"), 0.0f, 1.0f);
                    //рисуем полноэкранный прямоугольник
                    drawRect();
                }
            }
            //биндим дефолтную программу
            gl.glUseProgram(0);
            //биндим дефолтный фреймбуфер (тот который выводим данные на экран)
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
            //очищаем
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            //активируем текстуры 2, 1, 0 и пердаём записанные ранее текстуры
            //2 - глубины среди текстур она 3я, а место её второе
            //1 - размытая
            //0 - четкая
            gl.glActiveTexture(GL.GL_TEXTURE0 + 2);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[3]);
            gl.glActiveTexture(GL.GL_TEXTURE0 + 1);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[1]);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[0]);

            //испоьзуем главную программу
            gl.glUseProgram(mainProgram);
            {
                //пердаём тип
                gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "type"), type);
                //передаём положение фокуса
                gl.glUniform1f(gl.glGetUniformLocation(mainProgram, "focus"), focus);
                //передаёи влияние глубины
                gl.glUniform1f(gl.glGetUniformLocation(mainProgram, "range"), range);
                //отрисовываем полноэкранный прямоугольник
                drawRect();
            }
            //используем дефолтную программу
            gl.glUseProgram(0);
        }
        //отрисовываем текст
        drawText(drawable);
        //заканчиваем работу в 2D
        endOrtho();
        gl.glFlush();
    }

    //процедура отрисовки текста, пояснять думаю не стоит
    private static void drawText(GLAutoDrawable drawable) {
        gl.glColor4f(1f, 1f, 1f, 0.6f);
        gl.glWindowPos2i(10, drawable.getHeight() - 45);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, fps + "   fps");

        gl.glWindowPos2i(10, drawable.getHeight() - 65);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, focus + "   focus distance");

        gl.glWindowPos2i(10, drawable.getHeight() - 85);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, range + "   depth effect");

        gl.glWindowPos2i(10, drawable.getHeight() - 105);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, force + "   blur force");

        gl.glWindowPos2i(drawable.getWidth() - 180, drawable.getHeight() - 45);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "ESC - reset");

        gl.glWindowPos2i(drawable.getWidth() - 180, drawable.getHeight() - 65);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "1, 2, 3 - operation mode");

        gl.glWindowPos2i(drawable.getWidth() - 180, drawable.getHeight() - 85);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "mouse wheel - focus distance");

        gl.glWindowPos2i(drawable.getWidth() - 180, drawable.getHeight() - 105);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "[ ] - depth effect");

        gl.glWindowPos2i(drawable.getWidth() - 180, drawable.getHeight() - 125);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "O P - blur force");
    }

    //"включаем" 2D
    private void startOrtho() {
        //устанавливаем матрицу проекции
        gl.glMatrixMode(GL.GL_PROJECTION);
        //"запихиваем" в стек, находящееся в ней
        gl.glPushMatrix();
        //очищаем единичной
        gl.glLoadIdentity();
        //устанавливаем место отрисвоки 2D 
        gl.glOrtho(0, VIEWPORT[2], 0, VIEWPORT[3], 0, 1);
        //устанавливаем матрицу модель-вид
        gl.glMatrixMode(GL.GL_MODELVIEW);
        //"запихиваем" в стек, нахоядщееся в ней
        gl.glPushMatrix();
        //очищаем единичной
        gl.glLoadIdentity();
        //выключаем текст глубины
        gl.glDisable(GL.GL_DEPTH_TEST);
    }

    //"выключаем" 2D
    private void endOrtho() {
        //утснавливаем матрицу проекция
        gl.glMatrixMode(GL.GL_PROJECTION);
        //"вытаскиваем" записанную из стека
        gl.glPopMatrix();
        //устанавливаем матрицу модель-вид
        gl.glMatrixMode(GL.GL_MODELVIEW);
        //"вытаскиваем" записанную из стека
        gl.glPopMatrix();
        //включаем тест глубины
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    //отрисовка полноэкранного прямоугольника
    private static void drawRect() {
        //начинаем отрисовку прямоугольников
        gl.glBegin(GL.GL_QUADS);
        {
            //передаём вершины
            gl.glVertex2f(0.0f, 0.0f);
            gl.glVertex2f(VIEWPORT[2], 0);
            gl.glVertex2f(VIEWPORT[2], VIEWPORT[3]);
            gl.glVertex2f(0.0f, VIEWPORT[3]);
        }
        gl.glEnd();
    }

    //отрисовка сцена
    private static void drawScene() {
        //отрисока заварников в соотвествующих трём первый переменный положениях
        drawTeapot(-200d, 0d, 100d, 30d, Color.white);
        drawTeapot(-150d, 0d, 50d, 30d, Color.white);
        drawTeapot(-100d, 0d, 0d, 30d, Color.white);
        drawTeapot(-50d, 0d, -50d, 30d, Color.white);
        drawTeapot(0d, 0d, -100d, 30d, Color.white);
        //отрисовываем стол с размерами 300х300
        drawTable(300.0d, 300.0d);
    }

    private static void drawTeapot(double x, double y, double z, double R, Color color) {
        //"запихиваем" в стек
        gl.glPushMatrix();
        //перемещаем в точку x, y, z
        gl.glTranslated(x, y, z);
        //устанавливаем цвет в float
        gl.glColor4d(color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d, color.getAlpha() / 255d);
        //отрисовывем заварник
        glut.glutSolidTeapot(R);
        //"вытаскиваем" матрицу из стека
        gl.glPopMatrix();
    }

    //отрисовывка "стола"
    private static void drawTable(double width, double height) {
        //включаем поддержку текстур (для шейдеров) она была не нужна
        gl.glEnable(GL.GL_TEXTURE_2D);
        //биндим 4ю текстуру - с фактурой черного мармора
        gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[4]);
        //начинаем отрисовку прямоугольников
        gl.glBegin(GL.GL_QUADS);
        //передаём вершины из соотствующие им текстурные координаты
        gl.glTexCoord2i(0, 0);
        gl.glVertex3d(-width, -22.5d, -height);
        gl.glTexCoord2i(0, 1);
        gl.glVertex3d(-width, -22.5d, height);
        gl.glTexCoord2i(1, 1);
        gl.glVertex3d(width, -22.5d, height);
        gl.glTexCoord2i(1, 0);
        gl.glVertex3d(width, -22.5d, -height);
        gl.glEnd();
        //выключаем поддержку текстур
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    @Override
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    //точки с нормальным распределение (для нормального размытия)
    private static final float[] NORMAL = {
        0.134598f,
        0.127325f,
        0.107778f,
        0.081638f,
        0.055335f,
        0.033562f,
        0.018216f,
        0.008847f};
}

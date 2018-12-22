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
        //������ ����
        Frame frame = new Frame("DOF!");
        //�������� ������� ������
        int size = Toolkit.getDefaultToolkit().getScreenSize().height - 30;
        frame.setSize(size, size);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        //������ ����� - ����� ���������
        GLCanvas canvas = new GLCanvas();
        //������ � ��������� ����������
        Listener listener = new Listener();
        canvas.addKeyListener(listener);
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
        canvas.addMouseWheelListener(listener);
        canvas.addGLEventListener(new DOF());
        canvas.setBounds(0, 0, frame.getWidth(), frame.getHeight() - 30);
        //������ ����������� �������� ��������� ��������
        Animator animator = new Animator(canvas);

        frame.add(canvas);
        //��������� ��������� ����
        frame.addWindowListener(new WindowAdapter() {
            @Override
            //�������������� �������� �������� ����
            public void windowClosing(WindowEvent e) {
                new Thread(() -> {
                    //������������� ��������
                    animator.stop();
                    //������� �� �������� 0 (��� ������)
                    System.exit(0);
                }).start();
            }
        });
        //��������� ��������
        animator.start();
    }

    //���������� ��� �������� �������� ������
    private static final int[] VIEWPORT = new int[4];
    //���� �������
    //0 - ��� ��������� ��������� �����
    //1-2 - ��� ��������
    //3 - ��� ����� �������
    //4 - �������� �������� ��� ��������� �� ����
    private static final int[] TEXTURES = new int[5];
    //������ ������ �����
    //0 - ��� ��������� �����
    //1-2 - ��� ��������
    //3 - ��� ����� �������
    private static final int[] FBO = new int[3];

    //��������� ������������� �������
    private static void initTextures() {
        //���������� 4 �������� � ������
        gl.glGenTextures(4, TEXTURES, 0);
        //5� ������� �� �����
        TEXTURES[4] = loadTexture("table_texture.jpg").getTextureObject();
        //��� ���� �������
        for (int i = 0; i < 5; i++) {
            //������ "��������" �������
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[i]);
            //������������� ��������� 
            //���������
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            //���������
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        }
        //������ �������� �� ������� - �����
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }

    //��������� ��������� ������� �����
    private static void initFrameBuffers() {
        //���������� 3 ������ � ������
        gl.glGenFramebuffersEXT(3, FBO, 0);
        {
            //��� ��� ������
            for (int i = 0; i < 3; i++) {
                //������ ������� �� �������
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[i]);
                //������ ��������
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[i]);
                //������������� ���������, ����� ��������� ���� RGBA, � �����-�� ��������
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, VIEWPORT[2], VIEWPORT[3], 0, GL.GL_RGBA, GL.GL_FLOAT, null);
                //����������� � ����������� ��������
                gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, TEXTURES[i], 0);
            }

            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[0]);
            //��� �������� - ��� ������ �������
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[3]);
            //��������� ������ �������� �� ������ ������� � ���� �� ���������
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH_COMPONENT, VIEWPORT[2], VIEWPORT[3], 0, GL.GL_DEPTH_COMPONENT, GL.GL_FLOAT, null);
            //����������� � ����������� ��������
            gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_TEXTURE_2D, TEXTURES[3], 0);
            
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    //������� � ����������� �������� ���������
    private static int mainProgram, blurProgram;
    //��� ���������� �� ����� �����������
    //1 - � ����������� �������
    //2 - ��������� ��������
    //3 - �������� �������
    public static int type = 1;
    //���������� ������, ������� �������, ���� �������� 
    public static float focus = 0.5f, range = 6.0f, force = 0.002f;

    //������������� ��������
    private static void initPrograms() {
        //������ ��������� � �������� ��� �������� ���� �� ����� ...
        //��������� ��������
        blurProgram = ProgramBuilder.createProgram(gl, "blur.frag");
        //������ ��� ���������
        gl.glUseProgram(blurProgram);
        //������������� ��������� ���������� �������� (��� �� ��������)
        gl.glUniform1i(gl.glGetUniformLocation(blurProgram, "text"), 0);
        //������� �� ���������
        //������ ������
        gl.glUniform2i(gl.glGetUniformLocation(blurProgram, "viewport"), VIEWPORT[2], VIEWPORT[3]);
        //���������� ����� ��� �������� (�������� ���������� => �������� � ���������� ��������������)
        gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "normalSize"), NORMAL.length);
        //������� ��������� ��� ������ ��������
        gl.glUniform1fv(gl.glGetUniformLocation(blurProgram, "normal"), NORMAL.length, NORMAL, 0);

        //������ ��������� � ��������, ��� �������� ����� ���� �� ����� ...
        mainProgram = ProgramBuilder.createProgram(gl, "main.frag");
        //������ ��� ���������
        gl.glUseProgram(mainProgram);
        //������� ������� ������
        gl.glUniform2i(gl.glGetUniformLocation(mainProgram, "viewport"), VIEWPORT[2], VIEWPORT[3]);
        //����������� ��������� ����������� ���������
        //������
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "high"), 0);
        //��������
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "blur"), 1);
        //�������
        gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "depth"), 2);
        //������ ��������� �� �������
        gl.glUseProgram(0);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL();
        glu = new GLU();
        glut = new GLUT();
        //���������� � ���������� VIEWPORT ������� ������
        gl.glGetIntegerv(GL.GL_VIEWPORT, VIEWPORT, 0);
        //���������� ��������
        initTextures();
        //���������� ������
        initFrameBuffers();
        //�������������� ��������� ���������
        initPrograms();
        //�������� ��������� �����
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        //�������� ���������� ���������� ���������
        gl.glEnable(GL.GL_COLOR_MATERIAL);
        //������� ����� ������
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
        //�������� ����������� �����
        gl.glShadeModel(GL.GL_SMOOTH);
    }

    //��������� �������� �������� �� �����
    public static Texture loadTexture(String fileName) {
        Texture text = null;
        try {
            //������ �� ����
            text = TextureIO.newTexture(new File(fileName), false);
        } catch (IOException | GLException e) {
            //� ������ ������ ������� ��������� �� ������
            System.out.println(e.getMessage());
        }
        //���������� ��������
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

    //���������� ������ �� ����� �����
    public static double len = 300d;
    //������������ � �������������� ���� � ����, �� ������� "�����������" ������
    public static double angleV = 70d, angleH = -45d;
    //����� ��������� ������
    private static double xView, zView, yView;

    private static void calcCam() {
        //��������� ���������� ����� ������������ � �������������� ����
        //��������������� ��������� ����
        xView = len * Math.sin(Math.toRadians(angleV)) * Math.cos(Math.toRadians(angleH));
        zView = len * Math.sin(Math.toRadians(angleV)) * Math.sin(Math.toRadians(angleH));
        yView = len * Math.cos(Math.toRadians(angleV));
    }

    //���������� ��� �������� ���
    //����� �������
    private static long time;
    private static long lc_time;
    //������� ������
    private static int frames = Integer.MAX_VALUE;
    private static int fps;

    @Override
    public void display(GLAutoDrawable drawable) {
        //���� ��� �������� fps
        //������� �����
        time = System.currentTimeMillis();
        //���� ������ ������ �������
        if (time - lc_time >= 1000) {
            //�������� fps ����� ����������� ������
            fps = frames;
            //��������� ����� ��� ���������� �������
            lc_time = time;
            //�������� ���������� �������
            frames = 0;
        }
        //����������� ���������� ������
        frames++;
        //�������� ����� ���������� ���������� ��������� ��� ������
        calcCam();
        //������������� ������� �����
        gl.glLightiv(GL.GL_LIGHT0, GL.GL_POSITION, new int[]{1, 1, 1, 0}, 0);
        //������� �������
        gl.glLoadIdentity();
        //����������� � 0,0,0
        gl.glTranslated(0d, 0d, 0d);
        //���������� ������
        //��� ������ ���������� - ������ �������
        //��� ������ - ���� �������
        //��� ������ - �����������
        glu.gluLookAt(xView, yView, zView, 0d, 0d, 0d, 0d, 0.5d, 0d);
        {
            //������ 0��� ���������� - ��� ������ �����
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[0]);
            //�������
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            //������ ����� � ��������� ������
            drawScene();
        }
        //�������� "�������" ��������, �.�. ������ �������� ������ � 2D ��������
        startOrtho();
        {
            //������ ����������� ���������
            gl.glUseProgram(blurProgram);
            {
                //������ ���������� 2� (������ ������������ ����� �������� �� ������ �������� �.� �� ����� �� �������)
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[2]);
                //�������
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                //���������� �������� (�������)
                gl.glActiveTexture(GL.GL_TEXTURE0);
                //������ ������� �������� (� ���������� ������ ����� ������) �� ������� �����
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[0]);
                {
                    //������� ��������� ���� ��������
                    gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "force"), force);
                    //����������� ��������
                    gl.glUniform2f(gl.glGetUniformLocation(blurProgram, "dir"), 1.0f, 0.0f);
                    //������ ������������� �������������
                    drawRect();
                }
                //������ ���������
                gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, FBO[1]);
                //�������
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                //���������� �������� (�������)
                gl.glActiveTexture(GL.GL_TEXTURE0);
                //������ 2� �������� �� ����� 0 (��������������)
                //������, ������ ��� ������ ��� � �� �� �������� �������� � ����� ����������� �����
                //����� �������� ������������ ������ ��������������� �����������
                gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[2]);
                {
                    //������� ��������� ���� ��������
                    gl.glUniform1f(gl.glGetUniformLocation(blurProgram, "force"), force);
                    //����������� ��������
                    gl.glUniform2f(gl.glGetUniformLocation(blurProgram, "dir"), 0.0f, 1.0f);
                    //������ ������������� �������������
                    drawRect();
                }
            }
            //������ ��������� ���������
            gl.glUseProgram(0);
            //������ ��������� ���������� (��� ������� ������� ������ �� �����)
            gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
            //�������
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            //���������� �������� 2, 1, 0 � ������ ���������� ����� ��������
            //2 - ������� ����� ������� ��� 3�, � ����� � ������
            //1 - ��������
            //0 - ������
            gl.glActiveTexture(GL.GL_TEXTURE0 + 2);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[3]);
            gl.glActiveTexture(GL.GL_TEXTURE0 + 1);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[1]);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[0]);

            //��������� ������� ���������
            gl.glUseProgram(mainProgram);
            {
                //������ ���
                gl.glUniform1i(gl.glGetUniformLocation(mainProgram, "type"), type);
                //������� ��������� ������
                gl.glUniform1f(gl.glGetUniformLocation(mainProgram, "focus"), focus);
                //������� ������� �������
                gl.glUniform1f(gl.glGetUniformLocation(mainProgram, "range"), range);
                //������������ ������������� �������������
                drawRect();
            }
            //���������� ��������� ���������
            gl.glUseProgram(0);
        }
        //������������ �����
        drawText(drawable);
        //����������� ������ � 2D
        endOrtho();
        gl.glFlush();
    }

    //��������� ��������� ������, �������� ����� �� �����
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

    //"��������" 2D
    private void startOrtho() {
        //������������� ������� ��������
        gl.glMatrixMode(GL.GL_PROJECTION);
        //"����������" � ����, ����������� � ���
        gl.glPushMatrix();
        //������� ���������
        gl.glLoadIdentity();
        //������������� ����� ��������� 2D 
        gl.glOrtho(0, VIEWPORT[2], 0, VIEWPORT[3], 0, 1);
        //������������� ������� ������-���
        gl.glMatrixMode(GL.GL_MODELVIEW);
        //"����������" � ����, ����������� � ���
        gl.glPushMatrix();
        //������� ���������
        gl.glLoadIdentity();
        //��������� ����� �������
        gl.glDisable(GL.GL_DEPTH_TEST);
    }

    //"���������" 2D
    private void endOrtho() {
        //������������ ������� ��������
        gl.glMatrixMode(GL.GL_PROJECTION);
        //"�����������" ���������� �� �����
        gl.glPopMatrix();
        //������������� ������� ������-���
        gl.glMatrixMode(GL.GL_MODELVIEW);
        //"�����������" ���������� �� �����
        gl.glPopMatrix();
        //�������� ���� �������
        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    //��������� �������������� ��������������
    private static void drawRect() {
        //�������� ��������� ���������������
        gl.glBegin(GL.GL_QUADS);
        {
            //������� �������
            gl.glVertex2f(0.0f, 0.0f);
            gl.glVertex2f(VIEWPORT[2], 0);
            gl.glVertex2f(VIEWPORT[2], VIEWPORT[3]);
            gl.glVertex2f(0.0f, VIEWPORT[3]);
        }
        gl.glEnd();
    }

    //��������� �����
    private static void drawScene() {
        //�������� ���������� � �������������� ��� ������ ���������� ����������
        drawTeapot(-200d, 0d, 100d, 30d, Color.white);
        drawTeapot(-150d, 0d, 50d, 30d, Color.white);
        drawTeapot(-100d, 0d, 0d, 30d, Color.white);
        drawTeapot(-50d, 0d, -50d, 30d, Color.white);
        drawTeapot(0d, 0d, -100d, 30d, Color.white);
        //������������ ���� � ��������� 300�300
        drawTable(300.0d, 300.0d);
    }

    private static void drawTeapot(double x, double y, double z, double R, Color color) {
        //"����������" � ����
        gl.glPushMatrix();
        //���������� � ����� x, y, z
        gl.glTranslated(x, y, z);
        //������������� ���� � float
        gl.glColor4d(color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d, color.getAlpha() / 255d);
        //����������� ��������
        glut.glutSolidTeapot(R);
        //"�����������" ������� �� �����
        gl.glPopMatrix();
    }

    //����������� "�����"
    private static void drawTable(double width, double height) {
        //�������� ��������� ������� (��� ��������) ��� ���� �� �����
        gl.glEnable(GL.GL_TEXTURE_2D);
        //������ 4� �������� - � �������� ������� �������
        gl.glBindTexture(GL.GL_TEXTURE_2D, TEXTURES[4]);
        //�������� ��������� ���������������
        gl.glBegin(GL.GL_QUADS);
        //������� ������� �� ������������ �� ���������� ����������
        gl.glTexCoord2i(0, 0);
        gl.glVertex3d(-width, -22.5d, -height);
        gl.glTexCoord2i(0, 1);
        gl.glVertex3d(-width, -22.5d, height);
        gl.glTexCoord2i(1, 1);
        gl.glVertex3d(width, -22.5d, height);
        gl.glTexCoord2i(1, 0);
        gl.glVertex3d(width, -22.5d, -height);
        gl.glEnd();
        //��������� ��������� �������
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    @Override
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    //����� � ���������� ������������� (��� ����������� ��������)
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

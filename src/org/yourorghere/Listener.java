package org.yourorghere;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.media.opengl.GLAutoDrawable;
import static org.yourorghere.DOF.angleH;
import static org.yourorghere.DOF.angleV;
import static org.yourorghere.DOF.len;
import static org.yourorghere.DOF.type;
import static org.yourorghere.DOF.focus;
import static org.yourorghere.DOF.force;
import static org.yourorghere.DOF.range;

public class Listener implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    @Override
    public void keyReleased(KeyEvent e) {
        //на ESC - сброс настроек
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            //дефолтные настройки
            angleV = 70d;
            angleH = -45d;
            len = 300d;
            focus = 0.5f;
            range = 6.0f;
            force = 0.002f;
        }
    }

    @Override
    //функция поворота колёсика мыши
    public void mouseWheelMoved(MouseWheelEvent e) {
        //прибавляем коэффициент, умноженный на направление -1 или 1
        focus -= e.getWheelRotation() * 0.005;
        //ограничиваем выход за гравницы
        focus = Math.max(focus, 0.05f);
        focus = Math.min(focus, 0.75f);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    //зажатие кнопок, комментировать думаю не стоит
    //где Math.max или Math.min - ограничение на изменение
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) {
            angleV -= 2;
        }
        if (e.getKeyCode() == KeyEvent.VK_S) {
            angleV += 2;
        }
        if (e.getKeyCode() == KeyEvent.VK_A) {
            angleH += 2;
        }
        if (e.getKeyCode() == KeyEvent.VK_D) {
            angleH -= 2;
        }
        if (e.getKeyCode() == KeyEvent.VK_Q) {
            len += 5;
        }
        if (e.getKeyCode() == KeyEvent.VK_E) {
            len -= 5;
        }
        if (e.getKeyCode() == KeyEvent.VK_OPEN_BRACKET) {
            range = Math.max(0, range - 0.05f);
        }
        if (e.getKeyCode() == KeyEvent.VK_CLOSE_BRACKET) {
            range = Math.min(20, range + 0.05f);
        }
        if (e.getKeyCode() == KeyEvent.VK_O) {
            force = Math.max(0.001f, force - 0.00005f);
        }
        if (e.getKeyCode() == KeyEvent.VK_P) {
            force = Math.min(0.002f, force + 0.00005f);
        }
        if (e.getKeyCode() == KeyEvent.VK_1) {
            type = 1;
        }
        if (e.getKeyCode() == KeyEvent.VK_2) {
            type = 2;
        }
        if (e.getKeyCode() == KeyEvent.VK_3) {
            type = 3;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public void init(GLAutoDrawable drawable) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

}

//uniform - ���������� �����
//uniform - ���������� �����
uniform ivec2 viewport;
//��������, ���������� ��������
uniform sampler2D text;
//������ ������� � ��������� �������������� ����������
uniform float normalSize;
//��� ������ ��������� ������������� ��������
uniform float normal[32];
//���� ��������
uniform float force;
//����������� ��������
uniform vec2 dir;
void main() {
    //������������� ���������� �������� [0..1]
    vec2 uv = (gl_FragCoord.xy / viewport);
    //������ �������� ���� �� �����������
    vec2 dx = dir * force;
    //������� ��������
    vec2 cdx = vec2(0.0);
    //����� ��������
    vec4 sum = vec4(0.0);
    for (int i = 0; i < normalSize; i++){
        //��������� � ����� �������� �� �������� ������ �� ������� ��������, ���������� �� �������������� �����������
        sum += (texture2D(text, uv + cdx) + texture2D(text, uv - cdx)) * normal[i];
        //�������� �����������
        cdx += dx;
    }
    //��������� ���������� �����
    gl_FragColor = sum;
}
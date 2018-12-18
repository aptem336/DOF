//uniform - ���������� �����
//uniform - ���������� �����
uniform ivec2 viewport;
//��� �������� � ��������������� ����������
uniform sampler2D high;
uniform sampler2D blur;
uniform sampler2D depth;
//���������� ���� ��������� 1 - DOF, 2 - �������� ��������, 3 - ����� �������
uniform int type;
//��������� ������
uniform float focus;
//������� ������� �� ��������
uniform float range;
//��������� ���������� ������� � ��������
//�.�. ��������� �������� �� �������� ��������
float linearize(float z){
    //������� ��������� ���������
    float near = 0.1;
    //������� -|-|-
    float far = 30.0;
    //��������� ��� ������� � ��������
    float z_c1 = near / (far - near); 
    float z_c2 = (far + near) / (2.0 * (far - near)); 
    return z_c1 / (-z_c2 - 0.5 + z);
}
//���� ������� ������� � ��������� ������
float calc(float z){
    //clamp - ����������� ���������� [0..1]
    //abs - ������ �.�. ������� ����� ���� � �������������, � ���������� � ���������� ��������� ������ ������������� � � ��������� [0..1]
    return clamp(range * abs(z + focus), 0.0, 1.0);
}
//������� ������� �������
void main(){
    //������������� ���������� �������� [0..1]
    vec2 uv = gl_FragCoord.xy / viewport;
    //������ �� ����, ���������� ���� �������
    if (type == 1){
        //�������� ������� �� ������ ��������
        vec4 high = texture2D(high, uv);
        //�������� ������� �� �������� ��������
	vec4 blur  = texture2D(blur, uv);
        //��������� � ���������� ������ �� �������� ������� ��� ����� �������
        //�������� ������ �� ����� �������, ��������������� � � ������ ������ � ���� ��������, ���������� � ��������� [0..1] ��. ������� ������
        gl_FragColor = mix(high, blur, calc(linearize(texture2D(depth, uv).r)));
    } else if (type == 2) {
        //���������� �������� �� �������� ��������
        gl_FragColor = texture2D(blur, uv);
    } else if (type == 3) {
        //��������� "��������������" �������� �� ����� �������
        gl_FragColor = vec4(vec3(calc(linearize(texture2D(depth, uv).r))), 1.0);
    }
}
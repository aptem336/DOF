//uniform - задаваемые извне
//uniform - задаваемые извне
uniform ivec2 viewport;
//три текстуры с соответствующим назавниями
uniform sampler2D high;
uniform sampler2D blur;
uniform sampler2D depth;
//переменная типа отрисовки 1 - DOF, 2 - размытая текстура, 3 - карта глубины
uniform int type;
//положение фокуса
uniform float focus;
//влияние глубины на размытие
uniform float range;
//процедура приводящая глубину к линейной
//т.к. изачально хранятся не линейные значения
float linearize(float z){
    //ближняя плоскость отсечения
    float near = 0.1;
    //дальняя -|-|-
    float far = 30.0;
    //собсвенно сам переход к линейной
    float z_c1 = near / (far - near); 
    float z_c2 = (far + near) / (2.0 * (far - near)); 
    return z_c1 / (-z_c2 - 0.5 + z);
}
//учёт влияния глубины и положения фокуса
float calc(float z){
    //clamp - огрничичить интервалом [0..1]
    //abs - модуль т.к. глубина может быть и отрицательной, а смешивание в пропорциях принимает только положительные и в интервале [0..1]
    return clamp(range * abs(z + focus), 0.0, 1.0);
}
//главная функция шейдера
void main(){
    //относительные координаты текстуры [0..1]
    vec2 uv = gl_FragCoord.xy / viewport;
    //исходя из типа, возвращаем цвет пикселя
    if (type == 1){
        //значения пикселя из четкой текстуры
        vec4 high = texture2D(high, uv);
        //значения пикселя из размытой текстуры
	vec4 blur  = texture2D(blur, uv);
        //смешиваем в пропорциях исходя из значения глубины для этого пикселя
        //значение берётся из карты глубины, линеализируется и с учётов фокуса и силы размытия, приводится к интервалу [0..1] см. функции вверху
        gl_FragColor = mix(high, blur, calc(linearize(texture2D(depth, uv).r)));
    } else if (type == 2) {
        //возаращаем значение из размытой текстуры
        gl_FragColor = texture2D(blur, uv);
    } else if (type == 3) {
        //возвращем "подготовленное" знаечние из карты глубины
        gl_FragColor = vec4(vec3(calc(linearize(texture2D(depth, uv).r))), 1.0);
    }
}
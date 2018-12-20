//uniform - задаваемые извне
//uniform - задаваемые извне
uniform ivec2 viewport;
//текстура, подлежащая размытию
uniform sampler2D text;
//размер массива с нормально распределёнными значениями
uniform float normalSize;
//сам массив нормально распределённых значений
uniform float normal[32];
//сила размытия
uniform float force;
//направление размытия
uniform vec2 dir;
void main() {
    //относительные координаты текстуры [0..1]
    vec2 uv = (gl_FragCoord.xy / viewport);
    //вектор смещения сила на направление
    vec2 dx = dir * force;
    //текущее смещение
    vec2 cdx = vec2(0.0);
    //сумма значений
    vec4 sum = vec4(0.0);
    for (int i = 0; i < normalSize; i++){
        //добавляем к сумме значения из пикселей исходя из вектора смещения, умноженные на соответсвующий коэффициент
        sum += (texture2D(text, uv + cdx) + texture2D(text, uv - cdx)) * normal[i];
        //смещение увеличиваем
        cdx += dx;
    }
    //возращаем полученную сумму
    gl_FragColor = sum;
}
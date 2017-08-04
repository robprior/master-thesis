
//necessary
precision mediump float;
uniform sampler2D mapTexture;
varying vec2 v_magTexCoordinate;
void main ()
{
    gl_FragColor = texture2D(mapTexture, v_magTexCoordinate);
}

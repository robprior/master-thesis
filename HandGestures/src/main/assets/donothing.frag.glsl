
//necessary
precision mediump float;
uniform sampler2D camTexture;
varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

void main ()
{
    gl_FragColor = texture2D(camTexture, v_CamTexCoordinate);
}

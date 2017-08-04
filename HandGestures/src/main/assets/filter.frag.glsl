#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform sampler2D maskTex;
uniform sampler2D camTexture;
//uniform samplerExternalOES camTexture;


varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

void main ()
{

    //gl_FragColor = texture2D(mask, v_CamTexCoordinate) * texture2D(camTexture, v_CamTexCoordinate);
    vec4 maskCol = texture2D(maskTex, v_CamTexCoordinate);
    vec4 col     = texture2D(camTexture, v_CamTexCoordinate);
    gl_FragColor = maskCol.x * col;
}

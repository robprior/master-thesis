#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

uniform float cbMin;
uniform float cbMax;
uniform float crMin;
uniform float crMax;

void main ()
{
    vec4 cameraColor = texture2D(camTexture, v_CamTexCoordinate);
    // assumes rgb 0-255
    //float cb = 128.0 + (cameraColor.r * -0.148 + cameraColor.g * -0.291 + cameraColor.b * 0.439);
    //float cr = 128.0 + (cameraColor.r *  0.439 + cameraColor.g * -0.368 + cameraColor.b * -0.071);

    float cb = 128.0 + (cameraColor.r * -37.74 + cameraColor.g * -47.205 + cameraColor.b *111.945);
    float cr = 128.0 + (cameraColor.r *111.945 + cameraColor.g * -93.84 + cameraColor.b * -18.105);

    // equivalent to cbmin < cb < cbmax and crmin < cr < crmax without ifs
    //TODO faster? probably but if so by how much
    float mult = 1.0;
    mult = mult * clamp(     cb - cbMin, 0.0, 1.0);
    mult = mult * clamp(-1.0*cb + cbMax, 0.0, 1.0);
    mult = mult * clamp(     cr - crMin, 0.0, 1.0);
    mult = mult * clamp(-1.0*cr + crMax, 0.0, 1.0);


    //this is reverse of above need it to be either or
    //float mult2 = 0.0;
    //mult = 0.0;
    //mult = mult + clamp(-1.0*cb +  76.0, 0.0, 1.0);
    //mult = mult + clamp(cb - 128.0, 0.0, 1.0);
    //mult2 = mult2 + clamp(-1.0*cr + 132.0, 0.0, 1.0);
    //mult2 = mult2 + clamp(cr - 174.0, 0.0, 1.0);
    //clamp(mult , 0.0, 1.0);
    //clamp(mult2, 0.0, 1.0);
    //mult = mult * mult2;

    // if all tests pass mult will be 1
    gl_FragColor = cameraColor * mult;
}
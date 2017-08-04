#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

void main() {
    vec4 cameraColor = texture2D(camTexture, v_CamTexCoordinate);

    // conv RGB in range 0-1 to YCbCr range Y:16-235 CbCr:16-240
    float y  =  16.0 + (cameraColor.r * 65.535 + cameraColor.g * 128.52  + cameraColor.b *  24.99);
    float cb = 128.0 + (cameraColor.r * -37.74 + cameraColor.g * -47.205 + cameraColor.b * 111.945);
    float cr = 128.0 + (cameraColor.r *111.945 + cameraColor.g * -93.84  + cameraColor.b * -18.105);

    float mult = 1.0;
    if (!(cb >= 80.0  && cb <= 120.0)) mult*=0.0;
    if (!(cr >= 133.0 && cb <= 173.0)) mult*=0.0;

    // TODO can optimize this remove the divisions by changing factors above so YCbCr ranges from 0-1
    gl_FragColor = vec4(mult*vec3(cameraColor), 1.0);
}

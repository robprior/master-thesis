#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

void main() {
    vec4 cameraColor = texture2D(camTexture, v_CamTexCoordinate);

    // conv RGB in range 0-1 to YCbCr range Y:16-235 CbCr:16-240
    float y  =  16.0 + (cameraColor.r *  65.535 + cameraColor.g * 128.52  + cameraColor.b *  24.99);
    float cb = 128.0 + (cameraColor.r * -37.74  + cameraColor.g * -47.205 + cameraColor.b * 111.945);
    float cr = 128.0 + (cameraColor.r * 111.945 + cameraColor.g * -93.84  + cameraColor.b * -18.105);

    // rescale to range 0-1
    gl_FragColor = vec4(y/235.0, cb/240.0, cr/240.0, 1.0);
}

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

void main() {
    vec4 cameraColor = texture2D(camTexture, v_CamTexCoordinate);

    float cb = 128.0 + (cameraColor.r * -37.74 + cameraColor.g * -47.205 + cameraColor.b *111.945);
    float cr = 128.0 + (cameraColor.r *111.945 + cameraColor.g * -93.84 + cameraColor.b * -18.105);

    gl_FragColor = vec4(vec3(cb/240.0), 1.0);
}

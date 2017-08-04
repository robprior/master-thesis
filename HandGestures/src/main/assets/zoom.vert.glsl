//position
attribute vec4 position;
attribute vec4 camTexCoordinate;

//tex coords
varying vec2 v_CamTexCoordinate;

uniform float zoom;
uniform float offsetX;
uniform float offsetY;

void main()
{
    //camera texcoord needs to be manipulated by the transform given back from the system
    v_CamTexCoordinate = ((camTexCoordinate.xy + vec2(offsetX, offsetY))- .5) * (1.0 / zoom) + .5;
    gl_Position = position;
}
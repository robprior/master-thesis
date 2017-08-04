//position
attribute vec4 position;

//Transform rotate scale

//TODO figure out why aspect ratio is wrong (maybe not worong but reported wrong on choosing video size expected 1280/720
//
//1980 / 720
#define ASPECT_RAT  0.5625
#define HALF_WIDTH  990.0
#define HALF_HEIGHT 540.0

uniform float scale;
uniform float xPos;
uniform float yPos;
uniform float angle;

// TRS
void main()
{
    // mat4(1.0) is identity

    //scale
    mat4 scaleMat = mat4(1.0);
    scaleMat[0][0] = scale * ASPECT_RAT;
    scaleMat[1][1] = scale;

    //rotate
    mat4 rotateMat = mat4(1.0);
    float cosA = cos(-1.0*angle);
    float sinA = sin(-1.0*angle);
    rotateMat[0][0] =      cosA;
    rotateMat[1][1] =      cosA;
    rotateMat[0][1] = -1.0*sinA;
    rotateMat[1][0] =      sinA;

    // range here is -1,-1 (bottom left) -> 1,1 top left
    // range from 0,0 -> 1980/720
    mat4 translateMat = mat4(1.0);
    translateMat[3][0] = xPos/HALF_WIDTH -1.0;
    translateMat[3][1] = yPos/HALF_HEIGHT-1.0;
    gl_Position = translateMat * rotateMat * scaleMat * position;
}
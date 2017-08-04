//position
attribute vec4 position;
attribute vec2 mapTexCoordinate;

varying vec2 v_magTexCoordinate;

//Transform rotate scale

//TODO figure out why aspect ratio is wrong (maybe not worong but reported wrong on choosing video size expected 1280/720
//
//1980 / 720
#define ASPECT_RAT  0.5625
#define HALF_WIDTH  990.0
#define HALF_HEIGHT 540.0

/*
#define scale 2.0
#define angle -3.14/4.0
#define xPos 990.0
#define yPos 540.0
*/


uniform float scale;
uniform float xPos;
uniform float yPos;
uniform float angle;

// TRS
void main()
{
    // mat4(1.0) is identity

    //scale bigger is smaller
    mat2 scaleMat = mat2(1.0);
    scaleMat[0][0] = scale;
    scaleMat[1][1] = scale;

    //rotate
    mat2 rotateMat = mat2(1.0);
    float cosA = cos(-1.0*angle);
    float sinA = sin(-1.0*angle);
    rotateMat[0][0] =      cosA;
    rotateMat[1][1] =      cosA;
    rotateMat[0][1] = -1.0*sinA;
    rotateMat[1][0] =      sinA;

    // range here is -1,-1 (bottom left) -> 1,1 top left
    // range from 0,0 -> 1980/720


    v_magTexCoordinate = mapTexCoordinate;

    //translate
    //- moves right
    v_magTexCoordinate.x += xPos/HALF_WIDTH -1.0;
    v_magTexCoordinate.y += yPos/HALF_HEIGHT-1.0;

    v_magTexCoordinate -= 0.5;

    v_magTexCoordinate = rotateMat * scaleMat * v_magTexCoordinate;

    v_magTexCoordinate += 0.5;



    //v_magTexCoordinate = (translateMat * rotateMat * scaleMat * mapTexCoordinate).xy;
    //v_magTexCoordinate = (rotateMat * scaleMat * translateMat * mapTexCoordinate).xy;

    gl_Position = position;
}
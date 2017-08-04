#version 310 es
#extension  GL_NV_shader_atomic_float : require

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;

struct Moments {
    float m00;
    float m01;
    float m10;
    //float m11;
    //float m02;
    //float m20;
    float cogX;
    float cogY;
    float lastX;
    float lastY;
    //float mu20Prime;
    //float mu02Prime;
    //float mu11Prime;
    //float angle;
    //float slope;
    //float yIntercept;
    //float perpSlope;
    //float perpYIntercept;
};


layout(shared, binding = 0) coherent buffer MomentBuffer {
    Moments data;
} moments;

/*
layout(shared, binding = 4) coherent buffer WristCandidateBuffer {
    float buff[];
} wristCandidate;
*/

layout (local_size_x = 8, local_size_y = 8) in;

#define PI 3.14159265359
#define HALF_PI 1.57079632679

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    int distX = pos.x - int(moments.data.cogX);
    int absDistX = abs(distX);
    int distY = pos.y - int(moments.data.cogY);
    int absDistY = abs(distY);

    if(pos == ivec2(0,0)) {
        moments.data.lastX = moments.data.cogX;
        moments.data.lastY = moments.data.cogY;
    }

    //float angRat = float(distY)/float(distX);

    /*
    if        (distX < 0  && 0 <= distY) { //quad 2
        angRat = abs(angRat);
    } else if (0 <= distX && distY < 0)  { //quad 4
        angRat = abs(angRat);
    }*/

    //TODO based
    //float angle = atan(angRat);//atan(float(distY), float(distX));

    vec4 cogColour = vec4(1,0,0,1);

    //TODO debug code to check quadrant of angle
    //atan(y,x) is in range -PI,PI so quadrants 2 & 3 only need 1 conditional
    /*
    if (0.0 <= moments.data.angle && moments.data.angle < HALF_PI) { //quad 1 (0 -> 0.5pi)
        cogColour.r = 1.0;
    } else if (HALF_PI <= moments.data.angle) { //quad 2 (0.5 pi -> pi)
        cogColour.b = 1.0;
    } else if (moments.data.angle < -HALF_PI)  { //quad 3 (-0.5 pi -> -pi)
        cogColour.g = 1.0;
    } else if (moments.data.angle < 0.0 && -HALF_PI <= moments.data.angle)  { //quad 4 (0-> -0.5 pi
        cogColour.rgb = vec3(1.0);
    }
    */

    if(absDistX < 10 && absDistY < 10) {
        imageStore(framebuffer, pos, cogColour);
    }

    /*
    if (abs(angle - moments.data.angle) < 0.001) {
    //if (abs(PI - abs(abs(angle - angleFromMoments) - PI)) < 0.001) {
        imageStore(framebuffer, pos, vec4(0,1,0,1));
    }*/

    /*
    float perpYIntercept = float(pos.y) - (moments.data.perpSlope * float(pos.x));

    // y = m1 * x + b1 = m2 * x + b2
    // x = ( b2 - b1 ) / ( m1 - m2 )
    // y = ( m1 * b2 - m2 * b1 ) / ( m1 - m2 ) or just use y = m1 x + b1
    float xIntersectPos = (perpYIntercept - moments.data.yIntercept) / ( moments.data.slope -  moments.data.perpSlope);
    float yIntersectPos =  moments.data.slope * xIntersectPos +  moments.data.yIntercept;//( slope * perYIntercept - perpSlope * yIntercept) / (slope - perpSlope);

    int binNum = int(sqrt(xIntersectPos * xIntersectPos + yIntersectPos * yIntersectPos));
    */
    /* //debug
    if (abs(binNum - 500) < 1) {
        imageStore(framebuffer, pos, vec4(1,0,1,1));
    }*/

    //if (abs(xIntersectPos - xMean - 25.0) < 10.0 && abs(yIntersectPos - yMean - 25.0) < 10.0 ){
    /*
    if (wristCandidate.buff[binNum] > 0.0) {
        imageStore(framebuffer, pos, vec4(1,0,1,1));
    }*/
    /*
    if (abs(angle2 - angleFromMoments + HALF_PI) < 1.0) {
            imageStore(framebuffer, pos, vec4(1,0,1,1));w
    }
    */

    /*

     if (int(angRat) * 1000 % int(PI/2.0*1000.0) < 1) { //check if the tan(angRat) will be close to pos/neg infinity
            imageStore(framebuffer, pos, vec4(1,0,1,1));
        } else {

        }

    if        (0 <= distX && 0 <= distY) { //quad 1

    } else if (distX < 0  && 0 <= distY) { //quad 2

    } else if (distX < 0  && distY < 0)  { //quad 3

    } else if (0 <= distX && distY < 0)  { //quad 4

    }*/
}

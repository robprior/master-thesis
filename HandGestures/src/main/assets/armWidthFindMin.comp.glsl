#version 310 es

#extension  GL_NV_shader_atomic_float : require

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;

struct Moments {
    float m00;
    float m01;
    float m10;
    float m11;
    float m02;
    float m20;
    float cogX;
    float cogY;
    float mu20Prime;
    float mu02Prime;
    float mu11Prime;
    float angle;
    float slope;
    float yIntercept;
    float perpSlope;
    float perpYIntercept;
};

uniform float armWidthThresh;

layout(shared, binding = 0) coherent buffer MomentBuffer {
    Moments data;
} moments;

layout (local_size_x = 8, local_size_y = 8) in;

#define PI 3.14159265359
#define HALF_PI 1.57079632679

layout(shared, binding = 3) coherent buffer ArmWidthBuffer {
    float buff[];
} armWidth;

layout(shared, binding = 4) coherent buffer WristCandidateBuffer {
    float buff[];
} wristCandidate;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    /*
    float perpYIntercept = float(pos.y) - (moments.data.perpSlope * float(pos.x));

        // y = m1 * x + b1 = m2 * x + b2
        // x = ( b2 - b1 ) / ( m1 - m2 )
        // y = ( m1 * b2 - m2 * b1 ) / ( m1 - m2 ) or just use y = m1 x + b1
    float xIntersectPos = (perpYIntercept - moments.data.yIntercept) / ( moments.data.slope -  moments.data.perpSlope);
    float yIntersectPos =  moments.data.slope * xIntersectPos +  moments.data.yIntercept;//( slope * perYIntercept - perpSlope * yIntercept) / (slope - perpSlope);

    int binNum = int(sqrt(xIntersectPos * xIntersectPos + yIntersectPos * yIntersectPos));

    float centerWidth = armWidth.buff[binNum];
    int numSteps = 1;

    while(armWidth.buff[binNum + numSteps] > armWidth.buff[binNum] &&
          armWidth.buff[binNum - numSteps] > armWidth.buff[binNum]) {

        numSteps++;
    }
    wristCandidate.buff[binNum] = float(numSteps);
    */
    int minBin = 0;
    float minWidth = 100000.0;
    if (pos == ivec2(0,0)) {
        for( int bin = 0; bin < 3000; ++bin) {
            if (armWidth.buff[bin] > armWidthThresh && armWidth.buff[bin] < minWidth) {
                minWidth = armWidth.buff[bin];
                minBin = bin;
            }
        }
        wristCandidate.buff[minBin] = 100.0;
    }
}

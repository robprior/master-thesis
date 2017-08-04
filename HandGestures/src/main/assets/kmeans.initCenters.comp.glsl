#version 310 es
#extension  GL_NV_shader_atomic_float : require

layout (local_size_x = 8, local_size_y = 8) in;

// TODO anything else needed here?
struct KMeansCentersStruct {
    float x; // x/y will be accumulated based on position so can go out of range of 4byte ints
    float y;
    float numPts; // this might not go over but not a big deal having float over int
    float tempX;
    float tempY;
    uint  sem;
    float closestDist;
    float closestContourX;
    float closestContourY;
    uint isActive; // boolean
    //TODO CPU?
    float gestLastX;
    float gestLastY;
};



layout(shared, binding = 2) coherent buffer KMeansCenters {
    KMeansCentersStruct centers[];
} kmeanCenter;

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

layout(shared, binding = 5) coherent buffer MomentBuffer {
    Moments data;
} moments;


uniform float distThresh;
const int numKCenters = 5;

void initCenter(int id) {
    kmeanCenter.centers[id].x               = 0.0;
    kmeanCenter.centers[id].y               = 0.0;
    kmeanCenter.centers[id].numPts          = 0.0;
    kmeanCenter.centers[id].sem             = 0u;
    kmeanCenter.centers[id].closestContourX = 0.0;
    kmeanCenter.centers[id].closestContourY = 0.0;
    kmeanCenter.centers[id].closestDist     = 100000.0;
    kmeanCenter.centers[id].isActive        = 1u;
    kmeanCenter.centers[id].gestLastX       = 0.0;
    kmeanCenter.centers[id].gestLastY       = 0.0;
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    // dist thresh is radius of a circle aorund cog
    //float offsetX = (2.0 * distThresh) / float(numKCenters);
    //float xPosBase = moments.data.cogX - distThresh;

    /*
    float xPosBase = moments.data.cogX - (distThresh / 2.0);
    float offset  = distThresh / 4.0;
    float yPosBase = moments.data.cogY + (distThresh / 2.0);
    int center = pos.x % numKCenters;
    kmeanCenter.centers[center].x = xPosBase + float(center) * offset;
    float yPos = yPosBase;
    if (center == 2) {
        yPos = yPos + 2.0*offset;
    } else {
        yPos = yPos + float(center % 2) * offset;
    }
    kmeanCenter.centers[pos.x % numKCenters].y = yPos;
    */

    if (pos == ivec2(0,0)) {
        for(int i = 0; i<5; ++i) {
            initCenter(i);
        }

        //thumb
        kmeanCenter.centers[0].x = moments.data.cogX - 0.8* distThresh;
        kmeanCenter.centers[0].y = moments.data.cogY;

        //index
        kmeanCenter.centers[1].x = moments.data.cogX - 0.2 * distThresh;
        kmeanCenter.centers[1].y = moments.data.cogY + 0.8 * distThresh;

        //middle
        kmeanCenter.centers[2].x = moments.data.cogX;
        kmeanCenter.centers[2].y = moments.data.cogY + 0.9 *distThresh;

        //ring
        kmeanCenter.centers[3].x = moments.data.cogX + 0.2 * distThresh;
        kmeanCenter.centers[3].y = moments.data.cogY + 0.8 * distThresh;

        //pinky
        kmeanCenter.centers[4].x = moments.data.cogX + 0.4 * distThresh;
        kmeanCenter.centers[4].y = moments.data.cogY + 0.7 * distThresh;
    }
}

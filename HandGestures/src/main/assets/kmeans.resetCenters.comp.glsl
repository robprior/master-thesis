#version 310 es
#extension  GL_NV_shader_atomic_float : require

layout (local_size_x = 8, local_size_y = 8) in;

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


void resetCenter(int id)  {
    kmeanCenter.centers[id].numPts          = 0.0;
    kmeanCenter.centers[id].tempX           = 0.0;
    kmeanCenter.centers[id].tempY           = 0.0;
    //kmeanCenter.centers[id].sem             = 0u;
    //kmeanCenter.centers[id].closestContourX = 0.0;
    //kmeanCenter.centers[id].closestContourY = 0.0;
}
const int numKCenters = 5;
void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    if (pos.x < 5 && pos.y == 0) {
        resetCenter(pos.x);
    }
}

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

uniform int numKCenters;
uniform float centerReposThresh;

void updateCenter(in int i) {
    float newX = kmeanCenter.centers[i].closestContourX;
    float newY = kmeanCenter.centers[i].closestContourY;
    float dist = sqrt((kmeanCenter.centers[i].x-newX)*(kmeanCenter.centers[i].x-newX) + (kmeanCenter.centers[i].y-newY)*(kmeanCenter.centers[i].y-newY));
    if (kmeanCenter.centers[i].numPts == -1.0 && dist < centerReposThresh) {
        kmeanCenter.centers[i].x = kmeanCenter.centers[i].closestContourX;
        kmeanCenter.centers[i].y = kmeanCenter.centers[i].closestContourY;
    }
    kmeanCenter.centers[i].sem             = 0u;
    kmeanCenter.centers[i].closestContourX = 0.0;
    kmeanCenter.centers[i].closestContourY = 0.0;
    kmeanCenter.centers[i].closestDist     = 100000.0;
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    /*
    if (pos == ivec2(0,0)) {
        //TODO why do I get complaints about complex indices when using loops only occasionaly?
        for(int i = 0; i<5; ++i) {
            updateCenter(i);
        }
    }*/
    if (pos.x < 5 && pos.y == 0) {
        updateCenter(pos.x);
    }
}

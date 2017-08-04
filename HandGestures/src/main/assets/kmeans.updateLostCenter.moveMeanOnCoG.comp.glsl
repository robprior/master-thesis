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


void updateCenter(in int i) {
    if (kmeanCenter.centers[i].numPts == -1.0) {
        kmeanCenter.centers[i].x += moments.data.cogX - moments.data.lastX;
        kmeanCenter.centers[i].y += moments.data.cogY - moments.data.lastY;
    }
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    if (pos.x < 5 && pos.y == 0) {
        updateCenter(pos.x);
    }
}

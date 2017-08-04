#version 310 es
#extension  GL_NV_shader_atomic_float : require

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer KMeansLabels {
    float labels[];
} kmeanLabel;

layout(shared, binding = 4) coherent buffer KMeansWeights {
    float weights[];
} kmeanWeights;


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



void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);

    if (kmeanLabel.labels[offset] >= 0.0) {
        int center = int(kmeanLabel.labels[offset]);
        //atomicAdd(kmeanCenter.centers[center].tempX, kmeanWeights.weights[offset] * float(pos.x));
        //atomicAdd(kmeanCenter.centers[center].tempY, kmeanWeights.weights[offset] * float(pos.y));
        //atomicAdd(kmeanCenter.centers[center].numPts, kmeanWeights.weights[offset]);
        atomicAdd(kmeanCenter.centers[center].tempX, float(pos.x));
        atomicAdd(kmeanCenter.centers[center].tempY, float(pos.y));
        atomicAdd(kmeanCenter.centers[center].numPts, 1.0);
    }
}

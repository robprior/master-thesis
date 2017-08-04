#version 310 es
#extension  GL_NV_shader_atomic_float : require

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer KMeansLabels {
    float labels[];
} kmeanLabel;

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

uniform int numKCenters;
const float LOST_CENTER = -1.0;

layout(shared, binding = 2) coherent buffer KMeansCenters {
    KMeansCentersStruct centers[];
} kmeanCenter;

void setDist(in ivec2 pos, int index, uint isActive) {
    if (kmeanCenter.centers[index].numPts == LOST_CENTER) {
        float x = (kmeanCenter.centers[index].x - float(pos.x));
        float y = (kmeanCenter.centers[index].y - float(pos.y));
        float dist = sqrt(x*x + y*y);
        if (dist < kmeanCenter.centers[index].closestDist) {
            bool written = false;
            do {
                bool canWrite = atomicCompSwap(kmeanCenter.centers[index].sem, 0u, 1u) == 0u;
                if (canWrite) {
                    memoryBarrier();
                    if (dist < kmeanCenter.centers[index].closestDist) {
                        kmeanCenter.centers[index].closestDist     = dist;
                        kmeanCenter.centers[index].closestContourX = float(pos.x);
                        kmeanCenter.centers[index].closestContourY = float(pos.y);
                        kmeanCenter.centers[index].isActive        = isActive;
                    }
                    written = true;
                    atomicExchange(kmeanCenter.centers[index].sem, 0u);
                }
            } while (!written);
        }
    }
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);

    //TODO might need to make this not just > than minimum which would require doing some reorganizing of labels
    //TODO that would be set in the draw box / draw line comp progs
    // -2.0 for outside of min but not actively a label
    // -3.0 for only inside min
    // >=0 for close to another kmean - probably do not want this ever
    if (kmeanLabel.labels[offset] == -2.0 || kmeanLabel.labels[offset] == -3.0) {
        uint isActive = kmeanLabel.labels[offset] == -3.0? 0u : 1u; // isActive is false/0 if kmeans is within min dist thresh
        setDist(pos, 0, isActive);
        setDist(pos, 1, isActive);
        setDist(pos, 2, isActive);
        setDist(pos, 3, isActive);
        setDist(pos, 4, isActive);
        /*
        setDist(pos, 5);
        setDist(pos, 6);
        setDist(pos, 7);
        setDist(pos, 8);
        */
    }
}

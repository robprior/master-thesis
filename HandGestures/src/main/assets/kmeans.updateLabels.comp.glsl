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

layout(shared, binding = 2) coherent buffer KMeansCenters {
    KMeansCentersStruct centers[];
} kmeanCenter;

uniform int numKCenters;

//TODO what is wrong with this function the bottom condition is always true inout should read and write back
void findMinDistToCenter(in int index, in ivec2 pos, inout float minDist, out int label) {
    float x = (kmeanCenter.centers[index].x - float(pos.x));
    float y = (kmeanCenter.centers[index].y - float(pos.y));
    float dist = sqrt(x*x + y*y);
    if (dist < minDist) {
        minDist = dist;
        label = index;
    }
}

float getDist(in ivec2 pos, int index) {
    float x = (kmeanCenter.centers[index].x - float(pos.x));
    float y = (kmeanCenter.centers[index].y - float(pos.y));
    return sqrt(x*x + y*y);
}
void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);

    if (kmeanLabel.labels[offset] >= 0.0) {
        // TODO there an equivalent of float max in glsl doesn't matter much max dist at 720p is ~1400
        float minDist = 100000.0;
        int label = -1;

        /*
        for (int i = 0; i<9; ++i) {
            float x = (kmeanCenter.centers[i].x - float(pos.x));
            float y = (kmeanCenter.centers[i].y - float(pos.y));
            float dist = sqrt(x*x + y*y);
            if (dist < minDist) {
                minDist = dist;
                label = i;
            }
        }
        */

        /*
        findMinDistToCenter(0, pos, minDist, label);
        findMinDistToCenter(1, pos, minDist, label);
        findMinDistToCenter(2, pos, minDist, label);
        findMinDistToCenter(3, pos, minDist, label);
        findMinDistToCenter(4, pos, minDist, label);
        findMinDistToCenter(5, pos, minDist, label);
        findMinDistToCenter(6, pos, minDist, label);
        findMinDistToCenter(7, pos, minDist, label);
        findMinDistToCenter(8, pos, minDist, label);
        */

        float dist;
        dist = getDist(pos, 0);
        if (dist < minDist) {
            minDist = dist;
            label = 0;
        }
        dist = getDist(pos, 1);
        if (dist < minDist) {
            minDist = dist;
            label = 1;
        }
        dist = getDist(pos, 2);
        if (dist < minDist) {
            minDist = dist;
            label = 2;
        }
        dist = getDist(pos, 3);
        if (dist < minDist) {
            minDist = dist;
            label = 3;
        }
        dist = getDist(pos, 4);
        if (dist < minDist) {
            minDist = dist;
            label = 4;
        }
        /*
        dist = getDist(pos, 5);
        if (dist < minDist) {
            minDist = dist;
            label = 5;
        }
        dist = getDist(pos, 6);
        if (dist < minDist) {
            minDist = dist;
            label = 6;
        }
        dist = getDist(pos, 7);
        if (dist < minDist) {
            minDist = dist;
            label = 7;
        }
        dist = getDist(pos, 8);
        if (dist < minDist) {
            minDist = dist;
            label = 8;
        }
        */
        kmeanLabel.labels[offset] = float(label);
    }
}

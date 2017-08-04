#version 310 es
#extension  GL_NV_shader_atomic_float : require
#pragma optionNV (unroll all)

layout (local_size_x = 8, local_size_y = 8) in;

precision lowp image2D;
layout(binding = 3, rgba32f) uniform  image2D frameBuffer;

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

void drawBox5x5(in ivec2 globalPos, in vec4 colour) {
    imageStore(frameBuffer, globalPos + ivec2(-2,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-2, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2(-1,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 0,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 1,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 2), colour);

    imageStore(frameBuffer, globalPos + ivec2( 2,-2), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 2, 2), colour);
}

uniform int numKCenters;

void findCloseKmeanCenters(in ivec2 pos, inout float label) {
    /*

    for (int i = 0; i < 9; ++i) {
        if(abs(pos.x - int(kmeanCenter.centers[i].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[i].y)) < 10) {
            label = float(i);
        }
    }

    */

    //TODO why dioes this not like looops
    //TODO may god have mercy on my soul for this blashemy I'm about to commit
    if(abs(pos.x - int(kmeanCenter.centers[0].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[0].y)) < 10) {
        label = kmeanCenter.centers[0].isActive == 1u? 0.0 : -4.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[1].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[1].y)) < 10) {
        label = kmeanCenter.centers[1].isActive == 1u? 1.0 : -4.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[2].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[2].y)) < 10) {
        label = kmeanCenter.centers[2].isActive == 1u? 2.0 : -4.0;
        return;
    }

    if(abs(pos.x - int(kmeanCenter.centers[3].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[3].y)) < 10) {
        label = kmeanCenter.centers[3].isActive == 1u? 3.0 : -4.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[4].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[4].y)) < 10) {
        label = kmeanCenter.centers[4].isActive == 1u? 4.0 : -4.0;
        return;
    }
    /*
    if(abs(pos.x - int(kmeanCenter.centers[5].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[5].y)) < 10) {
        label = 5.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[6].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[6].y)) < 10) {
        label = 6.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[7].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[7].y)) < 10) {
        label = 7.0;
        return;
    }
    if(abs(pos.x - int(kmeanCenter.centers[8].x)) < 10 && abs(pos.y - int(kmeanCenter.centers[8].y)) < 10) {
        label = 8.0;
        return;
    }*/
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);

    float label = kmeanLabel.labels[offset];

    vec4 colour = vec4(0.0);


    // TODO why doesn't this like loops
    //if (label < 0.0) findCloseKmeanCenters(pos, label);
    if (label != 0.0) findCloseKmeanCenters(pos, label);

    if (label == 0.0) { colour = vec4(1.0, 0.0, 1.0, 1.0);} //magenta { colour = vec4(1.0, 1.0, 1.0, 1.0);} //white
    if (label == 1.0) { colour = vec4(1.0, 0.0, 0.0, 1.0);} //red
    if (label == 2.0) { colour = vec4(0.0, 1.0, 0.0, 1.0);} //lime
    if (label == 3.0) { colour = vec4(0.0, 0.5, 0.0, 1.0);} //green
    if (label == 4.0) { colour = vec4(1.0, 1.0, 0.0, 1.0);} //yellow
    //if (label == 5.0) { colour = vec4(0.0, 1.0, 1.0, 1.0);} //cyan
    //if (label == 6.0) { colour = vec4(1.0, 0.0, 1.0, 1.0);} //magenta
    //if (label == 7.0) { colour = vec4(1.0, 0.55, 0.0, 1.0);} //orange
    //if (label == 8.0) { colour = vec4(0.75, 0.75, 0.75, 1.0);} // silver
    if (label == -2.0) { colour = vec4(0.0, 0.0, 1, 1.0);} // blue
    if (label == -3.0) { colour = vec4(1.0, 0.55, 0.0, 1.0);} //orange
    if (label == -4.0) { colour = vec4(0.4, 0.4, 0.4, 1.0);} //orange

    if(colour != vec4(0.0)) {
        imageStore(frameBuffer, pos, colour);
    }
}

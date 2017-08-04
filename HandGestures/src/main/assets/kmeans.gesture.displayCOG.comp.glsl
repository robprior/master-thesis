#version 310 es
#extension  GL_NV_shader_atomic_float : require
#pragma optionNV (unroll all)

layout (local_size_x = 8, local_size_y = 8) in;

precision lowp image2D;
layout(binding = 3, rgba32f) uniform  image2D frameBuffer;

struct GestureStruct {
    float centerCOGX;
    float centerCOGY;
    float numActiveCenters;
    //TODO this sufficient for scale?
    float avgDistToCenter;
    float angle;
    float xSum;
    float ySum;
};
layout(shared, binding = 4) coherent buffer GestureData {
    GestureStruct data;
} gesture;

uniform int numKCenters;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;
    uint offset = uint(pos.x * int(height) + pos.y);


    vec4 colour = vec4(0.0);

    int xDif = (pos.x - int(gesture.data.centerCOGX));
    int yDif = (pos.y - int(gesture.data.centerCOGY));

    /*
    if (abs(angle - angleToDraw) < 0.001) {
        colour = colour = vec4(0.0, 1.0, 0.0, 1.0); // green
    }
    */

    if(abs(xDif) < 10 && abs(yDif) < 10) {
          colour = colour = vec4(1.0, 1.0, 1.0, 1.0); //white
    }

    if(colour != vec4(0.0)) {
        imageStore(frameBuffer, pos, colour);
    }
}

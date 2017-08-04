#version 310 es

//TODO do I want this in place? do I want this to be just a mask?

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D frameBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer FingerPosData {
    float buff[];
} fingerPos;

layout (local_size_x = 8, local_size_y = 8) in;

uniform int fingerAccumThresh;

void drawBox(in ivec2 globalPos, in vec4 colour) {
    imageStore(frameBuffer, globalPos + ivec2(-1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2(-1, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 0, 1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1,-1), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 0), colour);
    imageStore(frameBuffer, globalPos + ivec2( 1, 1), colour);
}

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);

    if (fingerPos.buff[offset] > float(fingerAccumThresh)) {
        drawBox(pos, vec4(1.0));
    } else {
        //drawBox(pos, vec4(0.0));
    }
}

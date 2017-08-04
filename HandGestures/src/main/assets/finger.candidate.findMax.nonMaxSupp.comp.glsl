#version 310 es

//TODO do I want this in place? do I want this to be just a mask?

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D frameBuffer;

layout (local_size_x = 8, local_size_y = 8) in;

layout(shared, binding = 0) coherent buffer FingerPosData {
    float buff[];
} fingerPos;


layout (local_size_x = 8, local_size_y = 8) in;

uniform int kernelSize;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);
    float val = fingerPos.buff[offset];
    uint loopOffset;

     for(int i = -kernelSize/2; i<kernelSize/2; ++i) {
        for(int j = -kernelSize/2; j<kernelSize/2; ++j) {
            loopOffset = uint((pos.x + i) * int(height) + (pos.y + j));
            if(fingerPos.buff[loopOffset] >  val) {
                val = 0.0f;
            }
        }
     }

     fingerPos.buff[offset] = val;
}

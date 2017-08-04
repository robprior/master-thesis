#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D outputMask;

precision lowp image2D;
layout(binding = 0, rgba32f) uniform  image2D inputMask;

layout (local_size_x = 8, local_size_y = 8) in;

uniform int structuringElementSize;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y);

    vec4 inputMaskPix = imageLoad(inputMask, pos);

    int structWidth = 1;
    if (inputMaskPix.a != 0.0) {
        for(int i = -structuringElementSize/2; i <= structuringElementSize /2; ++i) {
            for(int j = -structWidth/2; j <= structWidth/2; ++j) {
                imageStore(outputMask, ivec2(pos.x + i, pos.y + j), vec4(1.0));
            }
            if(i < 0) {
                structWidth += 2;
            } else {
                structWidth -= 2;
            }
        }
    } else {
        imageStore(outputMask, pos, vec4(0.0));
    }
}

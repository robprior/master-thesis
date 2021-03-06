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


    int mult = 1;
    int structWidth = 1;

    for(int i = -structuringElementSize/2; i <= structuringElementSize /2; ++i) {
        for(int j = -structWidth/2; j <= structWidth/2; ++j) {
            vec4 pix = imageLoad(inputMask, ivec2(pos.x + i, pos.y));
            mult *= int(pix.a);
        }
        if(i < 0) {
            structWidth += 2;
        } else {
            structWidth -= 2;
        }
        if (mult == 0) break;
    }

    imageStore(outputMask, pos, vec4(mult));
}

#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;


struct CodeElement {
    float Ihigh;
    float Ilow;
    float min;
    float max;
    float tLast;
    float stale;
};

uniform uint numCE;

layout(shared, binding = 0) coherent buffer CodeBookBuffer {
    CodeElement data[];
} codeBook;


layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y) * uint(numCE);

    uint i;
    for (i = uint(0); i<numCE; ++i) {
        codeBook.data[offset+i].Ihigh = 0.0;
        codeBook.data[offset+i].Ilow  = 0.0;
        codeBook.data[offset+i].min   = 0.0;
        codeBook.data[offset+i].max   = 0.0;
        codeBook.data[offset+i].tLast = 0.0;
        codeBook.data[offset+i].stale = 0.0;
    }
}

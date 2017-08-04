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

uniform float MAX_MOD;
uniform float MIN_MOD;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint(pos.x * int(height) + pos.y) * uint(numCE);

    vec4 pix = imageLoad(framebuffer, pos);

    //TODO
    vec4 cameraColor = imageLoad(framebuffer, pos);

    float I = cameraColor.r + cameraColor.g + cameraColor.b;

    uint i;
    bool isBackground = false;
    for (i = uint(0); i<numCE; ++i) {
        if (I < codeBook.data[offset+i].max + MAX_MOD && I > codeBook.data[offset+i].min - MIN_MOD ) {
            isBackground = true;
            break;
        }
    }

    if (isBackground) {
        imageStore(framebuffer, pos, vec4(0.0));
    }
    else {
        imageStore(framebuffer, pos, vec4(1.0));
    }
}

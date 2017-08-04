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

#define VIDEO_WIDTH 1280
#define VIDEO_HEIGHT 720
#define MAX_CE 1

#define MAX_MOD 0.1
#define MIN_MOD 0.1

layout(shared, binding = 0) coherent buffer CodeBookBuffer {
    CodeElement data[];
} codeBook;


layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    uint offset = uint((pos.x * VIDEO_HEIGHT + pos.y) * MAX_CE);
    //uint offset = uint((pos.y * VIDEO_HEIGHT + pos.x) * MAX_CE);

    vec4 pix = imageLoad(framebuffer, pos);


    vec4 cameraColor = imageLoad(framebuffer, pos);

    if (codeBook.data[offset].stale == 1.0) {
        imageStore(framebuffer, pos, 1.0 - cameraColor);
    }
}

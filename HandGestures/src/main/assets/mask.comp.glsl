#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;

precision lowp image2D;
layout(binding = 0, rgba32f) uniform  image2D cameraImage;

layout (local_size_x = 8, local_size_y = 8) in;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    //uint width  = gl_WorkGroupSize.x * gl_NumWorkGroups.x;
    //uint height = gl_WorkGroupSize.y * gl_NumWorkGroups.y;

    //uint offset = uint(pos.x * int(height) + pos.y);

    vec4 camPix = imageLoad(cameraImage, pos);
    vec4 mask   = imageLoad(framebuffer, pos);

    if (mask.a != 0.0) {
        imageStore(framebuffer, pos, vec4(vec3(camPix),1.0));
    }
}

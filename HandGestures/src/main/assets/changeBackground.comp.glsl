#version 310 es

precision lowp image2D;
layout(binding = 1, rgba32f) uniform  image2D framebuffer;


layout (local_size_x = 8, local_size_y = 8) in;


void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);

    vec4 colour = imageLoad(framebuffer, pos);

    if (colour == vec4(0.0)) {
        imageStore(framebuffer, pos, vec4(0.8));
    }
}
